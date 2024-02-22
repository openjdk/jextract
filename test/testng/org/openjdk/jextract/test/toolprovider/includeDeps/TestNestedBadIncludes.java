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
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class TestNestedBadIncludes extends JextractToolRunner {

    JextractResult result;

    @BeforeClass
    public void before() {
        Path output = getOutputFilePath("TestNestedBadIncludes-nestedBadIncludes.h");
        Path outputH = getInputFilePath("nested_bad_includes.h");
        List<String> options = new ArrayList<>();
        Stream.of(cases()).flatMap(
                arr -> Stream.of((String)arr[0], (String)arr[1])
        ).collect(Collectors.toCollection(() -> options));
        options.add(outputH.toString());
        result = run(output, options.toArray(new String[0]));
        result.checkFailure(FAILURE);
    }

    @Test(dataProvider = "cases")
    public void testBadIncludes(String includeOption, String badDeclName, String missingDepName) {
        result.checkContainsOutput("ERROR: " + badDeclName + " depends on " + missingDepName);
    }

    @DataProvider
    public static Object[][] cases() {
        return new Object[][]{
            {"--include-typedef",        "t_str",          "A" },
            {"--include-function",       "f_str_arg",      "A" },
            {"--include-function",       "f_str_ret",      "A" },
            {"--include-var",            "v_str",          "A" },
            {"--include-typedef",        "t_fp_arg",       "A" },
            {"--include-typedef",        "t_fp_ret",       "A" },
            {"--include-function",       "f_fp_arg",       "A" },
            {"--include-function",       "f_fp_ret",       "A" },
            {"--include-var",            "v_fp_arg",       "A" },
            {"--include-var",            "v_fp_ret",       "A" },
        };
    }
}
