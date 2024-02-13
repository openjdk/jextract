/*
 * Copyright (c) 2020, 2024, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
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

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.spi.ToolProvider;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import testlib.TestUtils;

import org.openjdk.jextract.JextractTool;

public class JtregJextract {

    private static final ToolProvider JEXTRACT_TOOL = new JextractTool.JextractToolProvider();
    private static final ToolProvider JAVAC_TOOL = ToolProvider.findFirst("javac")
            .orElseThrow(() ->  new RuntimeException("javac tool not found"));

    private final Path inputDir;
    private final Path outputDir;

    JtregJextract(Path input, Path output) {
        inputDir = input;
        outputDir = output;
    }

    private String[] processArgs(String... args) {
        Pattern sysPropPattern = Pattern.compile("'?\\$\\((.*)\\)'?");
        ArrayList<String> jextrOpts = new ArrayList<>();

        // FIXME jextrOpts.add("-C-nostdinc");
        jextrOpts.add("-I");
        jextrOpts.add(inputDir.toAbsolutePath().toString());
        jextrOpts.add("--output");
        jextrOpts.add(outputDir.toAbsolutePath().toString());

        int i = 0;
        while (i < args.length - 1) {
            String opt = args[i++];
            if ("--".equals(opt)) {
                break;
            }

            if ("--output".equals(opt)) {
                i++;
                continue;
            }
            // Pattern $(system.property.name) is replaced with the
            // value of the System property of that name.
            Matcher m = sysPropPattern.matcher(opt);
            if (m.matches()) {
                jextrOpts.add(System.getProperty(m.group(1)));
            } else {
                jextrOpts.add(opt);
            }
        }

        while (i < args.length) {
            jextrOpts.add(getInputFilePath(args[i++]).toString());
        }

        return jextrOpts.toArray(String[]::new);
    }

    private void jextract(String... options) {
        try {
            StringWriter writer = new StringWriter();
            PrintWriter pw = new PrintWriter(writer);
            int result = JEXTRACT_TOOL.run(pw, pw, processArgs(options));
            if (result != 0) {
                System.err.println(writer);
                throw new RuntimeException("jextract returns non-zero value");
            }
        } catch (Throwable t) {
            throw new AssertionError(t);
        }
    }

    private Path getInputFilePath(String filename) {
        return inputDir.resolve(filename).toAbsolutePath();
    }

    private static Path getJextractSourcePath() {
        Path testSrc = Path.of(System.getProperty("test.file"));
        return Path.of(testSrc.toFile().getName() + "_sources");
    }

    public static int main(String[] args) throws IOException {
        System.err.println("jextract");
        Path sourcePath = getJextractSourcePath();
        JtregJextract jj =  new JtregJextract(
            Paths.get(System.getProperty("test.src", ".")),
            sourcePath);
        jj.jextract(args);

        Path outputDir = Paths.get(System.getProperty("test.classes", "."));

        TestUtils.compile(sourcePath, outputDir);
        return 0;
    }
}
