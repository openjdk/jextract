/*
 * Copyright (c) 2026, Oracle and/or its affiliates. All rights reserved.
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

package org.openjdk.jextract.test.toolprovider.dumpIncludes;

import testlib.TestUtils;
import org.testng.annotations.Test;
import testlib.JextractToolRunner;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.testng.Assert.*;

public class TestNormalizedPathOutput extends JextractToolRunner {
    @Test
    public void testNormalizedPathOutput() throws IOException {
        Path filterOutput = getOutputFilePath("TestNormalizedPathOutput_output");
        try {
            Files.createDirectories(filterOutput);
            Path includes = filterOutput.resolve("test.conf");
            Path filterH = getInputFilePath("test.h");
            runNoOuput("--dump-includes", includes.toString(), filterH.toString()).checkSuccess();
            List<String> includeLines = Files.readAllLines(includes);
            List<String> heading = includeLines.stream().filter(line -> line.startsWith("#### Extracted from")).toList();
            assertEquals(heading.size(), 1);
            assertTrue(heading.getFirst().endsWith("/dumpIncludes/header.h"));
            List<String> filter = includeLines.stream().filter(line -> line.startsWith("--include-constant")).toList();
            assertEquals(filter.size(), 1);
            assertTrue(filter.getFirst().endsWith("/dumpIncludes/header.h"));
        } finally {
            TestUtils.deleteDir(filterOutput);
        }
    }
}
