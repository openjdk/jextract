/*
 * Copyright (c) 2020, 2025, Oracle and/or its affiliates. All rights reserved.
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
import org.openjdk.jextract.impl.CommandLine;
import org.openjdk.jextract.impl.DuplicateFilter;
import org.openjdk.jextract.impl.IncludeFilter;
import org.openjdk.jextract.impl.IncludeHelper;
import org.openjdk.jextract.impl.Logger;
import org.openjdk.jextract.impl.MissingDepChecker;
import org.openjdk.jextract.impl.NameMangler;
import org.openjdk.jextract.impl.Options.Library;
import org.openjdk.jextract.impl.OutputFactory;
import org.openjdk.jextract.impl.Parser;
import org.openjdk.jextract.impl.Options;
import org.openjdk.jextract.impl.UnsupportedFilter;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
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

    public static final boolean DEBUG = Boolean.getBoolean("jextract.debug");
    private static final boolean isMacOSX =
            System.getProperty("os.name", "unknown").equals("Mac OS X");

    // error codes
    private static final int SUCCESS       = 0;
    private static final int FAILURE       = 1;
    private static final int OPTION_ERROR  = 2;
    private static final int INPUT_ERROR   = 3;
    private static final int CLANG_ERROR   = 4;
    private static final int FATAL_ERROR   = 5;
    private static final int OUTPUT_ERROR  = 6;

    private final Logger logger;
    private final List<String> frameworkPaths;

    private JextractTool(Logger logger) {
        this.logger = logger;
        frameworkPaths = new ArrayList<>(Arrays.asList(
                "/System/Library/Frameworks/",
                "/System/Library/PrivateFrameworks/"
        ));
        inferMacOSFrameworkPath().ifPresent(path -> frameworkPaths.add(path.toString()));
    }

    private static boolean isSpecialHeaderName(String str) {
        return str.startsWith("<") && str.endsWith(">");
    }

    private static String generateTmpSource(List<String> headers) {
        return headers.stream().
               map(src -> {
                    if (isSpecialHeaderName(src)) {
                        return "#include " + src;
                    } else {
                        return "#include \"" + src + "\"";
                    }
               }).
               collect(Collectors.joining("\n"));
    }

    /**
     * Parse input files into a toplevel declaration with given options.
     * @param parserOptions options to be passed to the parser.
     * @return a toplevel declaration.
     */
    public static Declaration.Scoped parse(List<String> headers, String... parserOptions) {
        return parseInternal(Logger.DEFAULT, headers, parserOptions);
    }

    private static Declaration.Scoped parseInternal(Logger logger, List<String> headers, String... parserOptions) {
        String source = generateTmpSource(headers);
        return new Parser(logger)
                .parse("jextract$tmp.h", source, Stream.of(parserOptions).collect(Collectors.toList()));
    }

    public static List<JavaSourceFile> generate(Declaration.Scoped decl, String headerName,
                                                String targetPkg, List<Options.Library> libs,
                                                boolean useSystemLoadLibrary) {
        return generateInternal(decl, headerName, targetPkg, new IncludeHelper(),
                libs, useSystemLoadLibrary, Logger.DEFAULT);
    }

    private static List<JavaSourceFile> generateInternal(Declaration.Scoped decl, String headerName,
                                                         String targetPkg, IncludeHelper includeHelper,
                                                         List<Options.Library> libs, boolean useSystemLoadLibrary,
                                                         Logger logger) {
        var transformedDecl = Stream.of(decl)
                // process phases that add Skips first
                .map(new IncludeFilter(includeHelper)::scan)
                .map(new DuplicateFilter()::scan)
                .map(new UnsupportedFilter(logger)::scan)
                // then do the rest
                .map(new MissingDepChecker(logger)::scan)
                .map(new NameMangler(headerName)::scan)
                .findFirst().get();
        return logger.hasErrors() ?
                List.of() :
                List.of(OutputFactory.generateWrapped(transformedDecl, targetPkg, libs, useSystemLoadLibrary));
    }

    /**
     * Write resulting {@link JavaSourceFile} instances into specified destination path.
     * @param dest the destination path.
     * @param files the {@link JavaSourceFile} instances to be written.
     */
    public static void write(Path dest, List<JavaSourceFile> files) throws IOException {
        Path destDir = createOutputDir(dest);
        for (var entry : files) {
            String packagePath = packageNameToPath(entry.packageName());
            Path fullPath = destDir.resolve(packagePath, entry.className() + ".java").normalize();
            Path dir = fullPath.getParent();
            // In case the folder exist and is a link to a folder, this should be OK
            // Case in point, /tmp on MacOS link to /private/tmp
            if (Files.exists(dir)) {
                if (!Files.isDirectory(dir)) {
                    throw new FileAlreadyExistsException(dir.toAbsolutePath().toString());
                }
            } else {
                Files.createDirectories(fullPath.getParent());
            }
            Files.write(fullPath, List.of(entry.contents()));
        }
    }

    private static String packageNameToPath(String packageName) {
        return packageName.isEmpty() ? "" : packageName.replaceAll("\\.", "/") + "/";
    }

    private static Path createOutputDir(Path dest) throws IOException {
        Path absDest = dest.toAbsolutePath();
        if (!Files.exists(absDest)) {
            Files.createDirectories(absDest);
        }
        if (!Files.isDirectory(absDest)) {
            throw new IOException("Not a directory: " + dest);
        }
        return absDest;
    }

    private int printHelp(int exitCode) {
        logger.info("jextract.usage");
        return exitCode;
    }

    private void printOptionError(String message) {
        logger.err("jextract.opt.error", message);
    }

    /**
     * Main entry point to run the JextractTool
     *
     * @param args command line options passed
     */
    public static void main(String[] args) {
        JextractTool m = new JextractTool(Logger.DEFAULT);
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
                   if (spec == null) {
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
                       values = options.getOrDefault(spec.name(), new ArrayList<>());
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
            logger.fatal(ioexp, "argfile.read.error", ioexp);
            return OPTION_ERROR;
        }

        OptionParser parser = new OptionParser();
        parser.accepts("-D", List.of("--define-macro"), "help.D", true);
        parser.accepts("--dump-includes", "help.dump-includes", true);
        for (IncludeHelper.IncludeKind includeKind : IncludeHelper.IncludeKind.values()) {
            parser.accepts("--" + includeKind.optionName(), "help." + includeKind.optionName(), true);
        }
        parser.accepts("-h", List.of("-?", "--help"), "help.h", false);
        parser.accepts("--header-class-name", "help.header-class-name", true);
        parser.accepts("-I", List.of("--include-dir"), "help.I", true);
        parser.accepts("-l", List.of("--library"), "help.l", true);
        parser.accepts("--use-system-load-library", "help.use.system.load.library", false);
        parser.accepts("--output", "help.output", true);
        parser.accepts("-t", List.of("--target-package"), "help.t", true);
        parser.accepts("--version", "help.version", false);

        if (isMacOSX) {
            parser.accepts("-F", "help.mac.framework", true);
            parser.accepts("--framework", "help.framework.library.path", true);
        }

        OptionSet optionSet;
        try {
            optionSet = parser.parse(args);
        } catch (OptionException oe) {
            printOptionError(oe.getMessage());
            return OPTION_ERROR;
        }

        if (optionSet.has("--version")) {
            var version = JextractTool.class.getModule().getDescriptor().version();
            logger.info("jextract.version",
                    version.get(),
                    System.getProperty("java.runtime.version"),
                    LibClang.version());
            return SUCCESS;
        }

        if (optionSet.has("-h")) {
            return printHelp(SUCCESS);
        }

        Options.Builder builder = Options.builder();
        // before processing command line options, check & process compile_flags.txt.
        Path compileFlagsTxt = Paths.get(".", "compile_flags.txt");
        if (Files.exists(compileFlagsTxt)) {
            try {
                Files.lines(compileFlagsTxt).forEach(opt -> builder.addClangArg(opt));
            } catch (IOException ioExp) {
                logger.fatal(ioExp, "jextract.bad.compile.flags", ioExp.getMessage());
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

        inferPlatformIncludePath().ifPresent(platformPath -> builder.addClangArg("-I" + platformPath));

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

        boolean useSystemLoadLibrary = optionSet.has("--use-system-load-library");
        if (useSystemLoadLibrary) {
            if (!optionSet.has("-l")){
                logger.warn("jextract.no.library.specified");
            }
            builder.setUseSystemLoadLibrary(true);
        }

        if (optionSet.has("-F")) {
            List<String> paths = optionSet.valuesOf("-F");

            paths.forEach(p -> builder.addClangArg("-F" + p));
            frameworkPaths.addAll(0, paths);
        }

        inferMacOSFrameworkPath().ifPresent(platformPath -> builder.addClangArg("-F" + platformPath));

        int optionError = parseLibraries("l", optionSet, useSystemLoadLibrary, builder);
        if (optionError != 0) return optionError;

        optionError = parseLibraries("framework", optionSet, useSystemLoadLibrary, builder);
        if (optionError != 0) return optionError;

        String targetPackage = optionSet.has("-t") ? optionSet.valueOf("-t") : "";
        builder.setTargetPackage(targetPackage);

        builder.addClangArg("-I" + System.getProperty("user.dir"));

        if (optionSet.nonOptionArguments().size() == 0) {
            printOptionError(logger.format("expected.atleast.one.header"));
            return OPTION_ERROR;
        }

        Options options = builder.build();
        List<String> headers = optionSet.nonOptionArguments();

        List<JavaSourceFile> files;
        try {
            String headerName;
            if (optionSet.has("--header-class-name")) {
                headerName = optionSet.valueOf("--header-class-name");
            } else {
                if (headers.size() > 1) {
                    // more than one header specified but no --header-class-name specified.
                    logger.err("class.name.missing.for.multiple.headers");
                    return OPTION_ERROR;
                }
                headerName = headers.get(0);
                if (isSpecialHeaderName(headerName)) {
                    headerName = headerName.substring(1, headerName.length() - 1);
                }
                headerName = Paths.get(headerName).getFileName().toString();
            }
            Declaration.Scoped toplevel = parseInternal(logger, headers, options.clangArgs.toArray(new String[0]));

            if (JextractTool.DEBUG) {
                System.out.println(toplevel);
            }
            files = generateInternal(
                toplevel, headerName,
                options.targetPackage, options.includeHelper, options.libraries, options.useSystemLoadLibrary, logger);

            if (logger.hasClangErrors()) {
                return CLANG_ERROR;
            }
        } catch (RuntimeException re) {
            logger.fatal(re);
            return FATAL_ERROR;
        }

        try {
            if (options.includeHelper.dumpIncludesFile != null) {
                options.includeHelper.dumpIncludes();
            } else {
                Path output = Path.of(options.outputDir);
                try {
                    write(output, files);
                } catch (IOException e) {
                    logger.fatal(e);
                    return OUTPUT_ERROR;
                }
            }
        } catch (RuntimeException re) {
            logger.fatal(re);
            return FATAL_ERROR;
        }

        return logger.hasErrors() ?
                FAILURE :
                SUCCESS;
    }

    private int parseLibraries(String optionString, OptionSet optionSet, boolean useSystemLoadLibrary, Options.Builder builder) {
        String cmdOption = optionString.length() < 3 ?
                "-" + optionString :
                "--" + optionString;
        if (optionSet.has(cmdOption)) {
            for (String lib : optionSet.valuesOf(cmdOption)) {
                try {
                    String spec = cmdOption.equals("--framework") ?
                            resolveFrameworkPath(lib) :
                            lib;

                    if (spec == null) {
                        throw new IllegalArgumentException("Cannot find framework: " + lib);
                    }

                    Library library = Library.parse(spec);
                    Path libPath = Paths.get(library.libSpec());
                    if (!useSystemLoadLibrary ||
                            library.specKind() == Library.SpecKind.NAME ||
                            (libPath.isAbsolute() && Files.isRegularFile(libPath))) {
                        builder.addLibrary(library);
                    } else {
                        // not an absolute path, but --use-system-load-library was specified
                        logger.err("l.option.value.absolute.path", lib);
                    }
                } catch (IllegalArgumentException ex) {
                    logger.err(optionString + ".option.value.invalid", lib);
                    return OPTION_ERROR;
                }
            }
        }
        return 0;
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
            JextractTool instance = new JextractTool(new Logger(out, err));
            return instance.run(args);
        }
    }

    private Optional<Path> inferPlatformIncludePath() {
        return inferMacOsPath("usr", "include");
    }

    private Optional<Path> inferMacOSFrameworkPath() {
        return inferMacOsPath("System", "Library", "Frameworks");
    }

    private static String getMacOsSDKPath() throws IOException {
        ProcessBuilder pb = new ProcessBuilder().
                command("/usr/bin/xcrun", "--show-sdk-path");
        return new String(pb.start().getInputStream().readAllBytes());
    }

    private Optional<Path> inferMacOsPath(String... paths) {
        if (isMacOSX) {
            try {
                String str = getMacOsSDKPath();
                Path dir = Paths.get(str.trim(), paths);
                if (Files.isDirectory(dir)) {
                    return Optional.of(dir);
                }
            } catch (IOException ioExp) {
                if (JextractTool.DEBUG) {
                    logger.printStackTrace(ioExp);
                }
            }
        }
        return Optional.empty();
    }

    private String resolveFrameworkPath(String optionString) {
        for (String dir : frameworkPaths) {
            Path path = Path.of(dir, optionString + ".framework");
            if (Files.exists(path)) {
                return ":" + path + File.separator + optionString;
            }
        }

        return null;
    }
}
