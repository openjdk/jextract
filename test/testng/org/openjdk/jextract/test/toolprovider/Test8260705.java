/*
 * Copyright (c) 2021, Oracle and/or its affiliates. All rights reserved.
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

import java.lang.reflect.Method;
import java.nio.file.Path;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.MemorySegment;
import testlib.TestUtils;
import org.testng.annotations.Test;
import testlib.JextractToolRunner;

import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;

public class Test8260705 extends JextractToolRunner {
    @Test
    public void test() {
        Path outputPath = getOutputFilePath("output");
        Path headerFile = getInputFilePath("test8260705.h");
        run("--output", outputPath.toString(), headerFile.toString()).checkSuccess();
        try(TestUtils.Loader loader = TestUtils.classLoader(outputPath)) {
            Class<?> FooClass = loader.loadClass("Foo");
            checkMethod(FooClass, "c$get", byte.class, MemorySegment.class);
            checkMethod(FooClass, "c$get", byte.class, MemorySegment.class, long.class);
            checkMethod(FooClass, "c$set", void.class, MemorySegment.class, byte.class);
            checkMethod(FooClass, "c$set", void.class, MemorySegment.class, long.class, byte.class);

            Class<?> Foo2Class = loader.loadClass("Foo2");
            checkMethod(Foo2Class, "z$get", int.class, MemorySegment.class);
            checkMethod(Foo2Class, "z$get", int.class, MemorySegment.class, long.class);
            checkMethod(Foo2Class, "z$set", void.class, MemorySegment.class, int.class);
            checkMethod(Foo2Class, "z$set", void.class, MemorySegment.class, long.class, int.class);
            checkMethod(Foo2Class, "w$get", int.class, MemorySegment.class);
            checkMethod(Foo2Class, "w$get", int.class, MemorySegment.class, long.class);
            checkMethod(Foo2Class, "w$set", void.class, MemorySegment.class, int.class);
            checkMethod(Foo2Class, "w$set", void.class, MemorySegment.class, long.class, int.class);

            assertNotNull(loader.loadClass("Foo3"));

            Class<?> Foo4Class = loader.loadClass("Foo4");
            assertTrue(sizeof(Foo4Class) == 8L);

            Class<?> Foo5Class = loader.loadClass("Foo5");
            assertTrue(sizeof(Foo5Class) == 4L);

        } finally {
            TestUtils.deleteDir(outputPath);
        }
    }

    private long sizeof(Class<?> cls) {
        Method m = findMethod(cls, "sizeof");
        try {
            return (long)m.invoke(null);
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }
}

