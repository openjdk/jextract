/*
 * Copyright (c) 2020, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

package org.openjdk.jextract;

import org.openjdk.jextract.clang.LibClang;
import org.openjdk.jextract.impl.ClangException;
import org.openjdk.jextract.impl.CommandLine;
import org.openjdk.jextract.impl.IncludeHelper;
import org.openjdk.jextract.impl.CodeGenerator;
import org.openjdk.jextract.impl.Parser;
import org.openjdk.jextract.impl.Options;
import org.openjdk.jextract.impl.Writer;

import javax.tools.JavaFileObject;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.spi.ToolProvider;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Simple extraction tool which generates a minimal Java API. Such an API consists mainly of static methods,
 * where for each native function a static method is added which calls the underlying native method handles.
 * Similarly, for struct fields and global variables, static accessors (getter and setter) are generated
 * on top of the underlying memory access var handles. For each struct, a static layout field is generated.
 */
public final class JextractTool {
    private static final String MESSAGES_RESOURCE = "org.openjdk.jextract.impl.resources.Messages";

    private static final ResourceBundle MESSAGES_BUNDLE;
    static {
        MESSAGES_BUNDLE = ResourceBundle.getBundle(MESSAGES_RESOURCE, Locale.getDefault());
    }

    public static final boolean DEBUG = Boolean.getBoolean("jextract.debug");
    public static final Optional<Path> PLATFORM_INCLUDE_PATH = inferPlatformIncludePath();

    // error codes
    private static final int SUCCESS       = 0;
    private static final int OPTION_ERROR  = 1;
    private static final int INPUT_ERROR   = 2;
    private static final int CLANG_ERROR   = 3;
    private static final int RUNTIME_ERROR = 4;
    private static final int OUTPUT_ERROR  = 5;

    private final PrintWriter out;
    private final PrintWriter err;

    private static String format(String msgId, Object... args) {
        return new MessageFormat(MESSAGES_BUNDLE.getString(msgId)).format(args);
    }

    private JextractTool(PrintWriter out, PrintWriter err) {
        this.out = out;
        this.err = err;
    }

    private static Path generateTmpSource(List<Path> headers) {
        assert headers.size() > 1;
        try {
            Path tmpFile = Files.createTempFile("jextract", ".h");
            tmpFile.toFile().deleteOnExit();
            Files.write(tmpFile, headers.stream().
                    map(src -> "#include \"" + src + "\"").
                    collect(Collectors.toList()));
            return tmpFile;
        } catch (IOException ioExp) {
            throw new UncheckedIOException(ioExp);
        }
    }

    /**
     * Parse input files into a toplevel declaration with given options.
     * @param parserOptions options to be passed to the parser.
     * @return a toplevel declaration.
     */
    public static Declaration.Scoped parse(List<Path> headers, String... parserOptions) {
        Path source = headers.size() > 1? generateTmpSource(headers) : headers.iterator().next();
        return new Parser().parse(source, Stream.of(parserOptions).collect(Collectors.toList()));
    }

    public static List<JavaFileObject> generate(Declaration.Scoped decl, String headerName,
                                                String targetPkg, List<String> libNames) {
        return List.of(CodeGenerator.generate(decl, headerName, targetPkg, new IncludeHelper(), libNames));
    }

    private static List<JavaFileObject> generateInternal(Declaration.Scoped decl, String headerName,
                                                String targetPkg, IncludeHelper includeHelper, List<String> libNames) {
        return List.of(CodeGenerator.generate(decl, headerName, targetPkg, includeHelper, libNames));
    }

    /**
     * Write resulting {@link JavaFileObject} instances into specified destination path.
     * @param dest the destination path.
     * @param compileSources whether to compile .java sources or not
     * @param files the {@link JavaFileObject} instances to be written.
     */
    public static void write(Path dest, boolean compileSources, List<JavaFileObject> files) throws UncheckedIOException {
        try {
            new Writer(dest, files).writeAll(compileSources);
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
    }

    private int printHelp(int exitCode) {
        err.println(format("jextract.usage"));
        return exitCode;
    }


    private void printOptionError(Throwable throwable) {
        printOptionError(throwable.getMessage());
        if (DEBUG) {
            throwable.printStackTrace(err);
        }
    }

    private void printOptionError(String message) {
        err.println("OPTION ERROR: " + message);
        err.println("Usage: jextract <options> <header file>");
        err.println("Use --help for a list of possible options");
    }

    /**
     * Main entry point to run the JextractTool
     *
     * @param args command line options passed
     */
    public static void main(String[] args) {
        if (args.length == 0) {
            System.err.println("Expected a header file");
            return;
        }

        JextractTool m = new JextractTool(new PrintWriter(System.out, true), new PrintWriter(System.err, true));
        System.exit(m.run(args));
    }


    // Option handling code

    // specification for an option
    record OptionSpec(String name, List<String> aliases, String help, boolean argRequired) {
    }

    private static class OptionException extends RuntimeException {
        private static final long serialVersionUID = -1L;
        OptionException(String msg) {
            super(msg);
        }
    }

    // output of OptionParser.parse
    private static class OptionSet {
        private final Map<String, List<String>> options;
        // non-option arguments
        private final List<String> nonOptionArgs;

        OptionSet(Map<String, List<String>> options,
                List<String> nonOptionArgs) {
            this.options = options;
            this.nonOptionArgs = nonOptionArgs;
        }

        boolean has(String name) {
            return options.containsKey(name);
        }

        List<String> valuesOf(String name) {
            return options.get(name);
        }

        String valueOf(String name) {
            var values = valuesOf(name);
            return values == null? null : values.get(values.size() - 1);
        }

        List<String> nonOptionArguments() {
            return nonOptionArgs;
        }
    }

    private static final class OptionParser {
        // option name to corresponding OptionSpec mapping
        private Map<String, OptionSpec> optionSpecs = new HashMap<>();

        void accepts(String name, String help, boolean argRequired) {
            accepts(name, List.of(), help, argRequired);
        }

        void accepts(String name, List<String> aliases, String help, boolean argRequired) {
            var spec = new OptionSpec(name, aliases, help, argRequired);
            optionSpecs.put(name, spec);
            for (String alias : aliases) {
                optionSpecs.put(alias, spec);
            }
        }

        // does the string str start like an option?
        private boolean isOption(String str) {
            return str.length() > 1 && str.charAt(0) == '-';
        }

        // does the string str start like single char option?
        private boolean isSingleCharOptionWithArg(String str) {
            assert isOption(str);
            return str.length() > 2 && str.charAt(1) != '-';
        }

        // option part of single char option
        // -lclang => -l, -DFOO -> -D
        private String singleCharOption(String str) {
            assert isSingleCharOptionWithArg(str);
            return str.substring(0, 2);
        }

        // argument part of single char option
        // -lclang => clang, -DFOO -> FOO
        private String singleCharOptionArg(String str) {
            assert isSingleCharOptionWithArg(str);
            return str.substring(2);
        }

        OptionSet parse(String[] args) {
            Map<String, List<String>> options = new HashMap<>();
            List<String> nonOptionArgs = new ArrayList<>();
            for (int i = 0; i < args.length; i++) {
               String arg = args[i];
               // does this look like an option?
               if (isOption(arg)) {
                   OptionSpec spec = optionSpecs.get(arg);
                   String argValue = null;
                   // does not match known options directly.
                   // check for single char option followed
                   // by option value without whitespace in between.
                   // Examples: -lclang, -DFOO
                   if (spec == null ) {
                       spec = isSingleCharOptionWithArg(arg) ? optionSpecs.get(singleCharOption(arg)) : null;
                       // we have a matching single char option and that requires argument
                       if (spec != null && spec.argRequired()) {
                           argValue = singleCharOptionArg(arg);
                       } else {
                           // single char option special handling also failed. give up.
                           throw new OptionException("invalid option: " + arg);
                       }
                   }
                   // handle argument associated with the current option, if any
                   List<String> values;
                   if (spec.argRequired()) {
                       if (argValue == null) {
                           if (i == args.length - 1) {
                               throw new OptionException(spec.help());
                           }
                           argValue = args[i + 1];
                           i++; // consume value from next command line arg
                       } // else -DFOO like case. argValue already set

                       // do not allow argument value to start with '-'
                       // this will catch issues like "-l-lclang", "-l -t"
                       if (argValue.charAt(0) == '-') {
                           throw new OptionException(spec.help());
                       }
                       values = options.getOrDefault(spec.name(), new ArrayList<String>());
                       values.add(argValue);
                   } else {
                       // no argument value associated with this option.
                       // using empty list to flag that.
                       values = List.of();
                   }

                   // set value for the option as well as all its aliases
                   // so that option lookup, value lookup will work regardless
                   // which alias was used to check.
                   options.put(spec.name(), values);
                   for (String alias : spec.aliases()) {
                       options.put(spec.name(), values);
                   }
               } else { // !isOption(arg)
                   nonOptionArgs.add(arg);
               }
            }
            return new OptionSet(options, nonOptionArgs);
        }
    }

    private int run(String[] args) {
        try {
            args = CommandLine.parse(Arrays.asList(args)).toArray(new String[0]);
        } catch (IOException ioexp) {
            err.println(format("argfile.read.error", ioexp));
            if (JextractTool.DEBUG) {
                ioexp.printStackTrace(err);
            }
            return OPTION_ERROR;
        }

        OptionParser parser = new OptionParser();
        parser.accepts("-D", List.of("--define-macro"), format("help.D"), true);
        parser.accepts("--dump-includes", format("help.dump-includes"), true);
        for (IncludeHelper.IncludeKind includeKind : IncludeHelper.IncludeKind.values()) {
            parser.accepts("--" + includeKind.optionName(), format("help." + includeKind.optionName()), true);
        }
        parser.accepts("-h", List.of("-?", "--help"), format("help.h"), false);
        parser.accepts("--header-class-name", format("help.header-class-name"), true);
        parser.accepts("-I", List.of("--include-dir"), format("help.I"), true);
        parser.accepts("-l", List.of("--library"), format("help.l"), true);
        parser.accepts("--output", format("help.output"), true);
        parser.accepts("--source", format("help.source"), false);
        parser.accepts("-t", List.of("--target-package"), format("help.t"), true);
        parser.accepts("--version", format("help.version"), false);

        OptionSet optionSet;
        try {
            optionSet = parser.parse(args);
        } catch (OptionException oe) {
            printOptionError(oe);
            return OPTION_ERROR;
        }

        if (optionSet.has("--version")) {
            var version = JextractTool.class.getModule().getDescriptor().version();
            err.printf("%s %s\n", "jextract", version.get());
            err.printf("%s %s\n", "JDK version", System.getProperty("java.runtime.version"));
            err.printf("%s\n", LibClang.version());
            return SUCCESS;
        }

        if (optionSet.has("-h")) {
            return printHelp(SUCCESS);
        }

        if (optionSet.nonOptionArguments().size() != 1) {
            printOptionError("Expected 1 header file, not " + optionSet.nonOptionArguments().size());
            return OPTION_ERROR;
        }

        Options.Builder builder = Options.builder();
        // before processing command line options, check & process compile_flags.txt.
        Path compileFlagsTxt = Paths.get(".", "compile_flags.txt");
        if (Files.exists(compileFlagsTxt)) {
            try {
                Files.lines(compileFlagsTxt).forEach(opt -> builder.addClangArg(opt));
            } catch (IOException ioExp) {
                err.println("compile_flags.txt reading failed " + ioExp);
                if (JextractTool.DEBUG) {
                    ioExp.printStackTrace(err);
                }
                return OPTION_ERROR;
            }
        }

        if (optionSet.has("-D")) {
            optionSet.valuesOf("-D").forEach(p -> builder.addClangArg("-D" + p));
        }

        if (optionSet.has("-I")) {
            optionSet.valuesOf("-I").forEach(p -> builder.addClangArg("-I" + p));
        }

        Path builtinInc = Paths.get(System.getProperty("java.home"), "conf", "jextract");
        if (Files.isDirectory(builtinInc)) {
            builder.addClangArg("-I" + builtinInc);
        }

        PLATFORM_INCLUDE_PATH.ifPresent(platformPath -> {
            builder.addClangArg("-I" + platformPath);
        });

        String jextractHeaderPath = System.getProperty("jextract.header.path");
        if (jextractHeaderPath != null) {
            builtinInc = Paths.get(jextractHeaderPath);
            if (Files.isDirectory(builtinInc)) {
                builder.addClangArg("-I" + builtinInc);
            }
        }

        for (IncludeHelper.IncludeKind includeKind : IncludeHelper.IncludeKind.values()) {
            if (optionSet.has("--" + includeKind.optionName())) {
                optionSet.valuesOf("--" + includeKind.optionName()).forEach(p -> builder.addIncludeSymbol(includeKind, p));
            }
        }

        if (optionSet.has("--dump-includes")) {
            builder.setDumpIncludeFile(optionSet.valueOf("--dump-includes"));
        }

        if (optionSet.has("--output")) {
            builder.setOutputDir(optionSet.valueOf("--output"));
        }

        if (optionSet.has("--source")) {
            builder.setGenerateSource();
        }
        boolean librariesSpecified = optionSet.has("-l");
        if (librariesSpecified) {
            for (String lib : optionSet.valuesOf("-l")) {
                if (lib.indexOf(File.separatorChar) == -1) {
                    builder.addLibraryName(lib);
                } else {
                    Path libPath = Paths.get(lib);
                    if (libPath.isAbsolute() && Files.isRegularFile(libPath)) {
                        builder.addLibraryName(lib);
                    } else {
                        err.println(format("l.option.value.invalid", lib));
                        return OPTION_ERROR;
                    }
                }
            }
        }

        String targetPackage = optionSet.has("-t") ? optionSet.valueOf("-t") : "";
        builder.setTargetPackage(targetPackage);

        Options options = builder.build();

        Path header = Paths.get(optionSet.nonOptionArguments().get(0));
        if (!Files.isReadable(header)) {
            err.println(format("cannot.read.header.file", header));
            return INPUT_ERROR;
        }
        if (!(Files.isRegularFile(header))) {
            err.println(format("not.a.file", header));
            return INPUT_ERROR;
        }

        List<JavaFileObject> files = null;
        try {
            Declaration.Scoped toplevel = parse(List.of(header), options.clangArgs.toArray(new String[0]));

            if (JextractTool.DEBUG) {
                System.out.println(toplevel);
            }

            String headerName = optionSet.has("--header-class-name") ?
                optionSet.valueOf("--header-class-name") :
                header.getFileName().toString();

            files = generateInternal(
                toplevel, headerName,
                options.targetPackage, options.includeHelper, options.libraryNames);
        } catch (ClangException ce) {
            err.println(ce.getMessage());
            if (JextractTool.DEBUG) {
                ce.printStackTrace(err);
            }
            return CLANG_ERROR;
        } catch (RuntimeException re) {
            err.println(re.getMessage());
            if (JextractTool.DEBUG) {
                re.printStackTrace(err);
            }
            return RUNTIME_ERROR;
        }

        try {
            if (options.includeHelper.dumpIncludesFile != null) {
                options.includeHelper.dumpIncludes();
            } else {
                Path output = Path.of(options.outputDir);
                write(output, !options.source, files);
            }
        } catch (UncheckedIOException uioe) {
            err.println(uioe.getMessage());
            if (JextractTool.DEBUG) {
                uioe.printStackTrace(err);
            }
            return OUTPUT_ERROR;
        } catch (RuntimeException re) {
            err.println(re.getMessage());
            if (JextractTool.DEBUG) {
                re.printStackTrace(err);
            }
            return RUNTIME_ERROR;
        }

        return SUCCESS;
    }

    /**
     * ToolProvider implementation for jextract tool.
     */
    public static class JextractToolProvider implements ToolProvider {
        public JextractToolProvider() {}

        @Override
        public String name() {
            return "jextract";
        }

        @Override
        public int run(PrintWriter out, PrintWriter err, String... args) {
            JextractTool instance = new JextractTool(out, err);
            return instance.run(args);
        }
    }

    private static Optional<Path> inferPlatformIncludePath() {
        String os = System.getProperty("os.name");
        if (os.equals("Mac OS X")) {
            try {
                ProcessBuilder pb = new ProcessBuilder().
                    command("/usr/bin/xcrun", "--show-sdk-path");
                Process proc = pb.start();
                String str = new String(proc.getInputStream().readAllBytes());
                Path dir = Paths.get(str.trim(), "usr", "include");
                if (Files.isDirectory(dir)) {
                    return Optional.of(dir);
                }
            } catch (IOException ioExp) {
                if (JextractTool.DEBUG) {
                    ioExp.printStackTrace(System.err);
                }
            }
        }

        return Optional.empty();
    }
}
