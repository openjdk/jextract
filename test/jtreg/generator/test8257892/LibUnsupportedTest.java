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

import java.lang.foreign.Arena;
import java.lang.reflect.Method;
import java.lang.foreign.GroupLayout;
import java.lang.foreign.MemoryLayout;
import java.lang.foreign.MemoryLayout.PathElement;
import java.lang.foreign.MemorySegment;
import org.testng.annotations.Test;
import org.testng.SkipException;

import test.jextract.unsupported.unsupported_h;

import static org.testng.Assert.*;
import static test.jextract.unsupported.unsupported_h.*;
import test.jextract.unsupported.*;

/*
 * @test
 * @library /lib
 * @run main/othervm JtregJextract -l Unsupported --use-system-load-library -t test.jextract.unsupported unsupported.h
 * @build LibUnsupportedTest
 * @run testng/othervm --enable-native-access=ALL-UNNAMED LibUnsupportedTest
 */
public class LibUnsupportedTest {

    private static final boolean IS_WINDOWS = System.getProperty("os.name").startsWith("Windows");

    @Test
    public void testAllocateFoo() {
        try (Arena arena = Arena.ofConfined()) {
            var seg = Foo.allocate(arena);
            Foo.i(seg, 32);
            Foo.c(seg, (byte)'z');
            assertEquals(Foo.i(seg), 32);
            assertEquals(Foo.c(seg), (byte)'z');
        }
    }

    @Test
    public void testGetFoo() {
        try (Arena arena = Arena.ofConfined()) {
            var seg = Foo.reinterpret(getFoo(), arena, null);
            Foo.i(seg, 42);
            Foo.c(seg, (byte)'j');
            assertEquals(Foo.i(seg), 42);
            assertEquals(Foo.c(seg), (byte)'j');
        }
    }

    private static void checkField(GroupLayout group, String fieldName, MemoryLayout expected) {
        assertEquals(group.select(PathElement.groupElement(fieldName)), expected.withName(fieldName));
    }

    @Test
    public void testFieldTypes() {
        GroupLayout g = (GroupLayout)Foo.layout();
        checkField(g, "i", C_INT);
        checkField(g, "c", C_CHAR);
    }

    @Test
    public void testProblematicMethods() {
        if (IS_WINDOWS) {
            assertNotNull(findMethod(unsupported_h.class, "func"));
            assertNotNull(findMethod(unsupported_h.class, "func2"));
            assertNotNull(findMethod(unsupported_h.class, "func3"));
        } else {
            assertNull(findMethod(unsupported_h.class, "func"));
            assertNull(findMethod(unsupported_h.class, "func2"));
            assertNull(findMethod(unsupported_h.class, "func3"));
        }
        assertNotNull(findMethod(unsupported_h.class, "func4"));
        assertNotNull(findMethod(unsupported_h.class, "makeFoo"));
        assertNotNull(findMethod(unsupported_h.class, "copyFoo"));
    }

    private Method findMethod(Class<?> cls, String name) {
        for (Method m : cls.getMethods()) {
            if (m.getName().equals(name)) {
                return m;
            }
        }
        return null;
    }
}
