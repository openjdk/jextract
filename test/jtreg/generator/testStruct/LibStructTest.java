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

import java.lang.foreign.GroupLayout;
import java.lang.foreign.MemoryLayout;
import java.lang.foreign.MemoryLayout.PathElement;
import java.lang.foreign.NativeArena;
import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;
import static test.jextract.struct.struct_h.*;
import test.jextract.struct.*;

/*
 * @test id=classes
 * @library /lib
 * @run main/othervm JtregJextract -l Struct -t test.jextract.struct struct.h
 * @run testng/othervm --enable-native-access=ALL-UNNAMED LibStructTest
 */

/*
 * @test id=sources
 * @library /lib
 *
 * @run main/othervm JtregJextractSources -l Struct -t test.jextract.struct struct.h
 * @run testng/othervm --enable-native-access=ALL-UNNAMED LibStructTest
 */

public class LibStructTest {
    @Test
    public void testMakePoint() {
        try (NativeArena session = NativeArena.openConfined()) {
            var seg = makePoint(session, 42, -39);
            assertEquals(Point.x$get(seg), 42);
            assertEquals(Point.y$get(seg), -39);
        }
    }

    @Test
    public void testAllocate() {
        try (NativeArena session = NativeArena.openConfined()) {
            var seg = Point.allocate(session);
            Point.x$set(seg, 56);
            Point.y$set(seg, 65);
            assertEquals(Point.x$get(seg), 56);
            assertEquals(Point.y$get(seg), 65);
        }
    }

    @Test
    public void testAllocateArray() {
        try (NativeArena session = NativeArena.openConfined()) {
            var seg = Point.allocateArray(3, session);
            for (int i = 0; i < 3; i++) {
                Point.x$set(seg, i, 56 + i);
                Point.y$set(seg, i, 65 + i);
            }
            for (int i = 0; i < 3; i++) {
                assertEquals(Point.x$get(seg, i), 56 + i);
                assertEquals(Point.y$get(seg, i), 65 + i);
            }
        }
    }

    private static void checkField(GroupLayout group, String fieldName, MemoryLayout expected) {
        assertEquals(group.select(PathElement.groupElement(fieldName)), expected.withName(fieldName));
    }

    @Test
    public void testFieldTypes() {
        GroupLayout g = (GroupLayout)AllTypes.$LAYOUT();
        checkField(g, "sc", C_CHAR);
        checkField(g, "uc", C_CHAR);
        checkField(g, "s",  C_SHORT);
        checkField(g, "us", C_SHORT);
        checkField(g, "i",  C_INT);
        checkField(g, "ui", C_INT);
        checkField(g, "l",  C_LONG);
        checkField(g, "ul", C_LONG);
        checkField(g, "ll", C_LONG_LONG);
        checkField(g, "ull",C_LONG_LONG);
        checkField(g, "f",  C_FLOAT);
        checkField(g, "d",  C_DOUBLE);
    }
}
