/*
 * Copyright (c) 2020, Oracle and/or its affiliates. All rights reserved.
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

import java.lang.foreign.MemorySegment;
import testlib.TestUtils;
import org.testng.annotations.Test;
import java.nio.file.Path;
import java.lang.foreign.GroupLayout;
import testlib.JextractToolRunner;

import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;

public class UniondeclTest extends JextractToolRunner {
    @Test
    public void unionDecl() {
        Path uniondeclOutput = getOutputFilePath("uniondecl.h");
        Path uniondeclH = getInputFilePath("uniondecl.h");
        run("--output", uniondeclOutput.toString(), uniondeclH.toString()).checkSuccess();
        try(TestUtils.Loader loader = TestUtils.classLoader(uniondeclOutput)) {
            Class<?> cls = loader.loadClass("uniondecl_h");
            // check a method for "void func(IntOrFloat*)"
            assertNotNull(findMethod(cls, "func", MemorySegment.class));
            // check IntOrFloat layout
            Class<?> intOrFloatCls = loader.loadClass("IntOrFloat");
            GroupLayout intOrFloatLayout = (GroupLayout)findLayout(intOrFloatCls);
            assertNotNull(intOrFloatLayout);
            assertTrue(intOrFloatLayout.isUnion());
            checkField(intOrFloatLayout, "i",  C_INT);
            checkField(intOrFloatLayout, "f", C_FLOAT);
        } finally {
            TestUtils.deleteDir(uniondeclOutput);
        }
    }
}
