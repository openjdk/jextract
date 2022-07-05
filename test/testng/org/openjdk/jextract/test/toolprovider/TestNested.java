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

import java.lang.foreign.NativeArena;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Method;
import java.nio.file.Path;
import java.util.function.BiConsumer;

import java.lang.foreign.MemoryLayout;
import java.lang.foreign.MemorySegment;
import testlib.TestUtils;
import org.testng.annotations.Test;
import testlib.JextractToolRunner;

import static java.lang.foreign.MemoryLayout.PathElement.groupElement;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.fail;

public class TestNested extends JextractToolRunner {
    @Test
    public void testNestedStructs() {
        Path nestedOutput = getOutputFilePath("nestedgen");
        Path nestedH = getInputFilePath("nested.h");
        run("--output", nestedOutput.toString(), nestedH.toString()).checkSuccess();
        try(TestUtils.Loader loader = TestUtils.classLoader(nestedOutput)) {
            checkClass(loader, "Foo",
                checkField("bar", MemorySegment.class, 0),
                checkField("color", int.class, 8)
            );
            checkClass(loader, "Foo$Bar",
                checkField("x", int.class, 0),
                checkField("y", int.class, 4)
            );
            checkClass(loader, "U",
                checkField("point", MemorySegment.class, 0),
                checkField("rgb", int.class, 0),
                checkField("i", int.class, 0)
            );
            checkClass(loader, "U$Point",
                checkField("x", short.class, 0),
                checkField("y", short.class, 2)
            );
            checkClass(loader, "MyStruct",
                checkField("a", byte.class, 0),

                checkField("b", int.class, 4, "$anon$0"),
                checkField("c", int.class, 8, "$anon$0", "$anon$0"),

                checkField("d", byte.class, 12, "$anon$0"),
                checkField("f", MemorySegment.class, 13, "$anon$0"),

                checkField("g", int.class, 16, "$anon$1"),
                checkField("h", long.class, 16, "$anon$1"),

                checkField("k", MemorySegment.class, 24)
            );
            checkClass(loader, "MyStruct$MyStruct_Z",
                checkField("e", byte.class, 0)
            );
            checkClass(loader, "MyStruct$k",
                checkField("i", int.class, 0),
                checkField("j", int.class, 4)
            );
            checkClass(loader, "MyUnion",
                checkField("a", byte.class, 0),

                checkField("b", int.class, 0, "$anon$0"),
                checkField("c", int.class, 4, "$anon$0", "$anon$0"),

                checkField("d", byte.class, 8, "$anon$0"),
                checkField("f", MemorySegment.class, 9, "$anon$0"),

                checkField("g", int.class, 0, "$anon$1"),
                checkField("h", int.class, 4, "$anon$1"),

                checkField("k", MemorySegment.class, 0)
            );
            checkClass(loader, "MyUnion$MyUnion_Z",
                checkField("e", byte.class, 0)
            );
            checkClass(loader, "MyUnion$k",
                checkField("i", int.class, 0),
                checkField("j", long.class, 0)
            );
            checkClass(loader, "X",
                checkField("Z", MemorySegment.class, 0, "$anon$0")
            );
            checkClass(loader, "X$Z",
                checkField("y", int.class, 0)
            );
            checkClass(loader, "X2",
                checkField("y", int.class, 0, "$anon$0", "$anon$0")
            );
            checkClass(loader, "NestedUnion",
                checkField("x", int.class, 0),
                checkField("y", int.class, 4, "$anon$0"),
                checkField("z", int.class, 4, "$anon$0")
            );
        } finally {
            TestUtils.deleteDir(nestedOutput);
        }
    }

    @SafeVarargs
    private static void checkClass(TestUtils.Loader loader, String name, BiConsumer<Class<?>, MemoryLayout>... checks) {
        Class<?> cls = loader.loadClass(name);
        assertNotNull(cls);
        MemoryLayout layout = findLayout(cls);
        for (var check : checks) {
            check.accept(cls, layout);
        }
    }

    private static BiConsumer<Class<?>, MemoryLayout> checkField(String fieldName, Class<?> fieldType,
                                                                 long expectedOffset, String... fieldPath) {
        MemoryLayout.PathElement[] path = new MemoryLayout.PathElement[fieldPath.length + 1];
        int i = 0;
        for (; i < fieldPath.length; i++) {
            path[i] = groupElement(fieldPath[i]);
        }
        path[i] = groupElement(fieldName);
        return (cls, layout) -> {
            assertEquals(layout.byteOffset(path), expectedOffset);
            checkAccessors(cls, layout, fieldName, fieldType, layout.select(path));
        };
    }

    private static void checkAccessors(Class<?> cls, MemoryLayout layout, String fieldName, Class<?> type,
                                       MemoryLayout fieldLayout) {
        try {
            if (type == MemorySegment.class) {
                Method slicer = cls.getMethod(fieldName + "$slice", MemorySegment.class);
                assertEquals(slicer.getReturnType(), MemorySegment.class);
                try (NativeArena session = NativeArena.openConfined()) {
                    MemorySegment struct = session.allocate(layout);
                    MemorySegment slice = (MemorySegment) slicer.invoke(null, struct);
                    assertEquals(slice.byteSize(), fieldLayout.byteSize());
                }
            } else {
                Method getter = cls.getMethod(fieldName + "$get", MemorySegment.class);
                assertEquals(getter.getReturnType(), type);
                Method setter = cls.getMethod(fieldName + "$set", MemorySegment.class, type);
                assertEquals(setter.getReturnType(), void.class);

                Object zero = MethodHandles.zero(type).invoke();
                try (NativeArena session = NativeArena.openConfined()) {
                    MemorySegment struct = session.allocate(layout);
                    setter.invoke(null, struct, zero);
                    Object actual = getter.invoke(null, struct);
                    assertEquals(actual, zero);
                }
            }
        } catch (Throwable t) {
            fail("Unexpected exception", t);
        }
    }
}
