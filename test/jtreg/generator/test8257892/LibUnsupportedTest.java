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

import java.lang.reflect.Method;
import java.lang.foreign.GroupLayout;
import java.lang.foreign.MemoryLayout;
import java.lang.foreign.MemoryLayout.PathElement;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.NativeArena;
import org.testng.annotations.Test;

import test.jextract.unsupported.unsupported_h;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNull;
import static test.jextract.unsupported.unsupported_h.*;
import test.jextract.unsupported.*;

/*
 * @test id=classes
 * @library /lib
 * @run main/othervm JtregJextract -l Unsupported -t test.jextract.unsupported unsupported.h
 * @run testng/othervm --enable-native-access=ALL-UNNAMED LibUnsupportedTest
 */

/*
 * @test id=sources
 * @library /lib
 *
 * @run main/othervm JtregJextractSources -l Unsupported -t test.jextract.unsupported unsupported.h
 * @run testng/othervm --enable-native-access=ALL-UNNAMED LibUnsupportedTest
 */

public class LibUnsupportedTest {
    @Test
    public void testAllocateFoo() {
        try (NativeArena session = NativeArena.openConfined()) {
            var seg = Foo.allocate(session);
            Foo.i$set(seg, 32);
            Foo.c$set(seg, (byte)'z');
            assertEquals(Foo.i$get(seg), 32);
            assertEquals(Foo.c$get(seg), (byte)'z');
        }
    }

    @Test
    public void testGetFoo() {
        try (NativeArena session = NativeArena.openConfined()) {
            var seg = MemorySegment.ofAddress(getFoo().address(), Foo.sizeof(), null, session);
            Foo.i$set(seg, 42);
            Foo.c$set(seg, (byte)'j');
            assertEquals(Foo.i$get(seg), 42);
            assertEquals(Foo.c$get(seg), (byte)'j');
        }
    }

    private static void checkField(GroupLayout group, String fieldName, MemoryLayout expected) {
        assertEquals(group.select(PathElement.groupElement(fieldName)), expected.withName(fieldName));
    }

    @Test
    public void testFieldTypes() {
        GroupLayout g = (GroupLayout)Foo.$LAYOUT();
        checkField(g, "i", C_INT);
        checkField(g, "c", C_CHAR);
    }

    @Test
    public void testIgnoredMethods() {
        assertNull(findMethod(unsupported_h.class, "func"));
        assertNull(findMethod(unsupported_h.class, "func2"));
        assertNull(findMethod(unsupported_h.class, "func3"));
        assertNull(findMethod(unsupported_h.class, "func4"));
        assertNull(findMethod(unsupported_h.class, "makeFoo"));
        assertNull(findMethod(unsupported_h.class, "copyFoo"));
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
