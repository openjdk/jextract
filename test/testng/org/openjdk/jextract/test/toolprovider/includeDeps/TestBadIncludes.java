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
package org.openjdk.jextract.test.toolprovider.includeDeps;

import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import testlib.JextractToolRunner;

import java.nio.file.Path;

public class TestBadIncludes extends JextractToolRunner {

    JextractResult result;

    @BeforeClass
    public void before() {
        Path output = getOutputFilePath("TestBadIncludes-badIncludes.h");
        Path outputH = getInputFilePath("bad_includes.h");
        result = run(output,
        "--include-struct", "B",
                "--include-function", "m",
                "--include-typedef", "T",
                "--include-struct", "C",
                "--include-function", "n",
                outputH.toString());
        result.checkFailure(FAILURE);
    }

    @Test(dataProvider = "cases")
    public void testBadIncludes(String badDeclName, String missingDepName) {
        result.checkContainsOutput("ERROR: " + badDeclName + " depends on " + missingDepName);
    }

    @DataProvider
    public static Object[][] cases() {
        return new Object[][]{
            {"B",   "A" },
            {"m",   "A" },
            {"T",   "A" },
            {"a",   "A" }
        };
    }
}
