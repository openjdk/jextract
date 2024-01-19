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
import java.lang.foreign.GroupLayout;
import java.lang.foreign.MemoryLayout;
import java.lang.foreign.MemoryLayout.PathElement;
import java.lang.foreign.MemorySegment;

import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;
import static test.jextract.struct.struct_h.*;
import test.jextract.struct.*;

/*
 * @test
 * @library /lib
 * @run main/othervm JtregJextract -l Struct -t test.jextract.struct struct.h
 * @build LibStructTest
 * @run testng/othervm --enable-native-access=ALL-UNNAMED LibStructTest
 */
public class LibStructTest {
    @Test
    public void testMakePoint() {
        try (Arena arena = Arena.ofConfined()) {
            var seg = makePoint(arena, 42, -39);
            assertEquals(Point.x(seg), 42);
            assertEquals(Point.y(seg), -39);
        }
    }

    @Test
    public void testAllocate() {
        try (Arena arena = Arena.ofConfined()) {
            var seg = Point.allocate(arena);
            Point.x(seg, 56);
            Point.y(seg, 65);
            assertEquals(Point.x(seg), 56);
            assertEquals(Point.y(seg), 65);
        }
    }

    @Test
    public void testAllocateArray() {
        try (Arena arena = Arena.ofConfined()) {
            var seg = Point.allocateArray(3, arena);
            for (int i = 0; i < 3; i++) {
                MemorySegment point = Point.asSlice(seg, i);
                Point.x(point, 56 + i);
                Point.y(point, 65 + i);
            }
            for (int i = 0; i < 3; i++) {
                MemorySegment point = Point.asSlice(seg, i);
                assertEquals(Point.x(point), 56 + i);
                assertEquals(Point.y(point), 65 + i);
            }
        }
    }

    private static void checkField(GroupLayout group, String fieldName, MemoryLayout expected) {
        assertEquals(group.select(PathElement.groupElement(fieldName)), expected.withName(fieldName));
    }

    @Test
    public void testFieldTypes() {
        GroupLayout g = (GroupLayout)AllTypes.layout();
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
