/*
 * Copyright (c) 2024, Oracle and/or its affiliates. All rights reserved.
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
package org.openjdk.jextract.test.toolprovider.cpp;

import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import testlib.JextractToolRunner;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.Path;

public class TestCXXParsing extends JextractToolRunner {

    JextractResult result;

    @BeforeClass
    public void before() throws IOException {
        Path output = getOutputFilePath("TestCXXParsing-cxx.h");
        Path outputH = getInputFilePath("cxx.h");
        // put a "compile_flags.txt" file in current dir, to force C++ parsing
        Files.writeString(Path.of("compile_flags.txt"), "-xc++");
        result = run(output, outputH.toString());
    }

    @Test(dataProvider = "skippedCases")
    public void testSkipped(String skippedName, String reason) {
        result.checkContainsOutput("warning: Skipping " + skippedName + " (" + reason);
    }

    @Test(dataProvider = "nonSkippedCases")
    public void testNonSkipped(String nonSkippedName) {
        result.checkDoesNotContainOutput("warning: Skipping " + nonSkippedName);
    }

    private static final String REASON_UNSUPPORTED_LANG = "language";

    @DataProvider
    public static Object[][] skippedCases() {
        return new Object[][]{
            {"m_point",              REASON_UNSUPPORTED_LANG},
            {"m_choice",             REASON_UNSUPPORTED_LANG},
            {"Foo",                  REASON_UNSUPPORTED_LANG}
        };
    }

    @DataProvider
    public static Object[][] nonSkippedCases() {
        return new Object[][]{
                {"x" },
                {"y" },
                {"Point" },
                {"Choice" },
                {"enumtype" }
        };
    }
}
