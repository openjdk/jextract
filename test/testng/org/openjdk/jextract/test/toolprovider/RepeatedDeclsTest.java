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
import java.lang.foreign.MemoryLayout;
import java.lang.foreign.MemorySegment;
import testlib.JextractToolRunner;

import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;

public class RepeatedDeclsTest extends JextractToolRunner {
    @Test
    public void repeatedDecls() throws Throwable {
        Path repeatedDeclsOutput = getOutputFilePath("repeatedDeclsgen");
        Path repeatedDeclsH = getInputFilePath("repeatedDecls.h");
        run("--output", repeatedDeclsOutput.toString(), repeatedDeclsH.toString()).checkSuccess();
        try(TestUtils.Loader loader = TestUtils.classLoader(repeatedDeclsOutput)) {
            Class<?> cls = loader.loadClass("repeatedDecls_h");
            // check a method for "void func(int)"
            assertNotNull(findMethod(cls, "func", int.class));

            // check a method for "void func2(int)"
            assertNotNull(findMethod(cls, "func2", int.class));

            // check a method for "void func3(int*)"
            assertNotNull(findMethod(cls, "func3", MemorySegment.class));

            // check a method for "void func4(int*)"
            assertNotNull(findMethod(cls, "func4", MemorySegment.class));

            // check a method for "void func5(int)"
            assertNotNull(findMethod(cls, "func5", int.class));

            // check a method for "double distance(struct Point)"
            assertNotNull(findMethod(cls, "distance", MemorySegment.class));

            // check a getter method for "i"
            assertNotNull(findMethod(cls, "i$get"));

            // check a setter method for "i"
            assertNotNull(findMethod(cls, "i$set", int.class));

            // make sure that enum constants are generated fine
            checkIntGetter(cls, "R", 0);
            checkIntGetter(cls, "G", 1);
            checkIntGetter(cls, "B", 2);
            checkIntGetter(cls, "C", 0);
            checkIntGetter(cls, "M", 1);
            checkIntGetter(cls, "Y", 2);

            // check Point layout
            Class<?> pointCls = loader.loadClass("Point");
            checkPoint(pointCls);
            Class<?> point_tCls = loader.loadClass("Point_t");
            checkPoint(point_tCls);
            assertTrue(pointCls.isAssignableFrom(point_tCls));
            Class<?> point$0Cls = loader.loadClass("POINT$0");
            checkPoint(point$0Cls);
            assertTrue(pointCls.isAssignableFrom(point$0Cls));

            // check Point3D layout
            Class<?> point3DCls = loader.loadClass("Point3D");
            checkPoint3D(point3DCls);
            Class<?> point3D_tCls = loader.loadClass("Point3D_t");
            checkPoint3D(point3D_tCls);
            assertTrue(point3DCls.isAssignableFrom(point3D_tCls));
        } finally {
            TestUtils.deleteDir(repeatedDeclsOutput);
        }
    }

    private void checkPoint(Class<?> pointCls) {
        MemoryLayout pointLayout = findLayout(pointCls);
        assertNotNull(pointLayout);
        assertTrue(((GroupLayout)pointLayout).isStruct());
        checkField(pointLayout, "i", C_INT);
        checkField(pointLayout, "j", C_INT);
    }

    private void checkPoint3D(Class<?> point3DCls) {
        MemoryLayout point3DLayout = findLayout(point3DCls);
        assertNotNull(point3DLayout);
        assertTrue(((GroupLayout)point3DLayout).isStruct());
        checkField(point3DLayout, "i", C_INT);
        checkField(point3DLayout, "j", C_INT);
        checkField(point3DLayout, "k", C_INT);
    }
}
