/*
 * Copyright (c) 2022, Oracle and/or its affiliates. All rights reserved.
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

package org.openjdk.jextract.test.toolprovider;

import testlib.JextractToolRunner;
import testlib.TestUtils;
import org.testng.annotations.Test;
import java.nio.file.Path;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertNull;

public class Test7903164 extends JextractToolRunner {
    @Test
    public void testWithoutMacro() {
        Path output = getOutputFilePath("7903164gen_withoutmacro");
        Path outputH = getInputFilePath("test7903164.h");
        run("--output", output.toString(), outputH.toString()).checkSuccess();
        try(TestUtils.Loader loader = TestUtils.classLoader(output)) {
            assertNotNull(loader.loadClass("test7903164_h"));
            assertNotNull(loader.loadClass("func"));
            assertNull(loader.loadClass("func2"));
        } finally {
            TestUtils.deleteDir(output);
        }
    }

    @Test
    public void testWithMacro() {
        Path output = getOutputFilePath("7903164gen_withmacro");
        Path outputH = getInputFilePath("test7903164.h");
        run("-D", "FOO", "--output", output.toString(), outputH.toString()).checkSuccess();
        try(TestUtils.Loader loader = TestUtils.classLoader(output)) {
            assertNotNull(loader.loadClass("test7903164_h"));
            assertNotNull(loader.loadClass("func"));
            assertNotNull(loader.loadClass("func2"));
        } finally {
            TestUtils.deleteDir(output);
        }
    }

    @Test
    public void testWithMacro2() {
        Path output = getOutputFilePath("7903164gen_withmacro2");
        Path outputH = getInputFilePath("test7903164.h");
        run("--define-macro", "FOO", "--output", output.toString(), outputH.toString()).checkSuccess();
        try(TestUtils.Loader loader = TestUtils.classLoader(output)) {
            assertNotNull(loader.loadClass("test7903164_h"));
            assertNotNull(loader.loadClass("func"));
            assertNotNull(loader.loadClass("func2"));
        } finally {
            TestUtils.deleteDir(output);
        }
    }
}
