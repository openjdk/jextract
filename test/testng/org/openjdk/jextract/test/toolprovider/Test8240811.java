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

package org.openjdk.jextract.test.toolprovider;

import java.nio.file.Path;

import java.lang.foreign.MemoryLayout;
import java.lang.foreign.StructLayout;
import java.lang.foreign.UnionLayout;
import testlib.TestUtils;
import org.testng.annotations.Test;
import testlib.JextractToolRunner;

import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;

public class Test8240811 extends JextractToolRunner {
    @Test
    public void testNameCollision() {
        Path nameCollisionOutput = getOutputFilePath("name_collision_gen");
        Path nameCollisionH = getInputFilePath("name_collision.h");
        runAndCompile(nameCollisionOutput, nameCollisionH.toString());
        try(TestUtils.Loader loader = TestUtils.classLoader(nameCollisionOutput)) {
            Class<?> cls = loader.loadClass("name_collision_h");
            assertNotNull(cls);

            // check foo layout
            Class<?> fooCls = loader.loadClass("foo");
            MemoryLayout fooLayout = findLayout(fooCls);
            assertNotNull(fooLayout);
            assertTrue(fooLayout instanceof StructLayout);
            checkField(fooLayout, "x",  C_INT);
            checkField(fooLayout, "y",  C_INT);
            checkField(fooLayout, "z",  C_INT);

            // check foo2 layout
            Class<?> foo2Cls = loader.loadClass("foo2");
            MemoryLayout foo2Layout = findLayout(foo2Cls);
            assertNotNull(foo2Layout);
            assertTrue(foo2Layout instanceof UnionLayout);
            checkField(foo2Layout, "i", C_INT);
            checkField(foo2Layout, "l", C_LONG);

            // check bar layout
            Class<?> barCls = loader.loadClass("bar");
            MemoryLayout barLayout = findLayout(barCls);
            assertNotNull(barLayout);
            assertTrue(barLayout instanceof StructLayout);
            checkField(barLayout, "f1", C_FLOAT);
            checkField(barLayout, "f2", C_FLOAT);

            // check bar layout
            Class<?> bar2Cls = loader.loadClass("bar2");
            MemoryLayout bar2Layout = findLayout(bar2Cls);
            assertNotNull(bar2Layout);
            assertTrue(bar2Layout instanceof UnionLayout);
            checkField(bar2Layout, "f", C_FLOAT);
            checkField(bar2Layout, "d", C_DOUBLE);
        } finally {
            TestUtils.deleteDir(nameCollisionOutput);
        }
    }
}
