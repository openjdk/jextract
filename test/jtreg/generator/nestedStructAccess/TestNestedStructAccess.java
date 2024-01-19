/*
 * Copyright (c) 2024, Oracle and/or its affiliates. All rights reserved.
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

import test.jextract.nestedaccess.*;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.util.concurrent.atomic.AtomicInteger;

import org.testng.annotations.Test;
import static org.testng.Assert.assertEquals;

/*
 * @test
  * @library /lib
 * @run main/othervm JtregJextract -l Func -t test.jextract.nestedaccess nestedStructAccess.h
 * @build TestNestedStructAccess
 * @run testng/othervm --enable-native-access=ALL-UNNAMED TestNestedStructAccess
 */
public class TestNestedStructAccess {

    @Test
    public void testNestedStructAccess() {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment start = allocatePoint(1, 2, arena);
            MemorySegment end = allocatePoint(3, 4, arena);
            MemorySegment line = allocateLine(start, end, arena);
            checkPointEquals(Line.start(line), start);
            checkPointEquals(Line.end(line), end);
        }
    }

    static MemorySegment allocatePoint(int x, int y, Arena arena) {
        MemorySegment point = Point.allocate(arena);
        Point.x(point, x);
        Point.y(point, y);
        return point;
    }

    static MemorySegment allocateLine(MemorySegment start, MemorySegment end, Arena arena) {
        MemorySegment line = Line.allocate(arena);
        Line.start(line, start);
        Line.end(line, end);
        return line;
    }

    static void checkPointEquals(MemorySegment found, MemorySegment expected) {
        assertEquals(Point.x(found), Point.x(expected));
        assertEquals(Point.y(found), Point.y(expected));
    }
}
