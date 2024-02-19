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
package org.openjdk.jextract.test.toolprovider.unsupported;

import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import testlib.JextractToolRunner;

import java.nio.file.Path;

public class TestSkipped extends JextractToolRunner {

    JextractResult result;

    @BeforeClass
    public void before() {
        Path output = getOutputFilePath("TestUnsupportedTypes-unsupportedTypes.h");
        Path outputH = getInputFilePath("unsupportedTypes.h");
        result = run(output,
                // dummy include to turn on exclusions
                "--include-function", "nonexistent",
                outputH.toString());
    }

    @Test(dataProvider = "cases")
    public void testUnsupportedTypes(String skippedName) {
        result.checkDoesNotContainOutput("WARNING: Skipping " + skippedName);
    }

    @DataProvider
    public static Object[][] cases() {
        return new Object[][]{
            {"returns_unsupported"              },
            {"accepts_unsupported"              },
            {"unsupported_t"                    },
            {"unsupported_func_t"               },
            {"returns_unsupported_func"         },
            {"accepts_unsupported_func"         },
            {"accepts_unsupported_func_varargs" },
            {"GLOBAL_UNSUPPORTED"               },
            {"GLOBAL_UNSUPPORTED_FUNC"          },
            {"accepts_undefined"                },
            {"returns_undefined"                },
            {"accepts_undefined_func"           },
            {"GLOBAL_UNDECLARED"                },
            {"undefined_typedef"                },
            {"INT_128_NUM"                      }
        };
    }
}
