/*
 * Copyright (c) 2023, Oracle and/or its affiliates. All rights reserved.
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
package org.openjdk.jextract.test.toolprovider.unsupported;

import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import testlib.JextractToolRunner;

import java.nio.file.Path;

public class TestUnsupportedTypes extends JextractToolRunner {

    JextractToolRunner.JextractResult result;

    @BeforeClass
    public void before() {
        Path output = getOutputFilePath("TestUnsupportedTypes-unsupportedTypes.h");
        Path outputH = getInputFilePath("unsupportedTypes.h");
        result = run("--source", "--output",
            output.toString(), outputH.toString()).checkSuccess();
    }

    @Test(dataProvider = "cases")
    public void testUnsupportedTypes(String skippedName, String reason) {
        result.checkContainsOutput("WARNING: skipping " + skippedName + ": " + reason);

    }

    private static final String REASON_UNSUPPORTED_TYPE = "unsupported type usage";
    private static final String REASON_VARARGS_CALLBACK = "varargs in callbacks is not supported";
    private static final String REASON_NO_FUNC_DESC = "can not compute FunctionDescriptor";
    private static final String REASON_NO_MEM_LAYOUT = "can not compute MemoryLayout";

    @DataProvider
    public static Object[][] cases() {
        return new Object[][]{
            {"returns_unsupported",              REASON_UNSUPPORTED_TYPE},
            {"accepts_unsupported",              REASON_UNSUPPORTED_TYPE},
            {"unsupported_t",                    REASON_UNSUPPORTED_TYPE},
            {"unsupported_func_t",               REASON_UNSUPPORTED_TYPE},
            {"returns_unsupported_func",         REASON_UNSUPPORTED_TYPE},
            {"accepts_unsupported_func",         REASON_UNSUPPORTED_TYPE},
            {"accepts_unsupported_func_varargs", REASON_VARARGS_CALLBACK},
            {"GLOBAL_UNSUPPORTED",               REASON_UNSUPPORTED_TYPE},
            {"GLOBAL_UNSUPPORTED_FUNC",          REASON_UNSUPPORTED_TYPE},
            {"GLOBAL_HAS_UNSUPPORTED",           REASON_UNSUPPORTED_TYPE},
            {"accepts_undeclared",               REASON_NO_FUNC_DESC},
            {"returns_undeclared",               REASON_NO_FUNC_DESC},
            {"accepts_undeclared_func",          REASON_NO_FUNC_DESC},
            {"GLOBAL_UNDECLARED",                REASON_NO_MEM_LAYOUT},
            {"undeclared_typedef",               REASON_NO_MEM_LAYOUT},
        };
    }
}
