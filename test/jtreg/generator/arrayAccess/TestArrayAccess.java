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

import org.testng.annotations.Test;

import static org.testng.Assert.*;

import test.jextract.arrayaccess.*;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;

import static test.jextract.arrayaccess.array_access_h.*;

/*
 * @test
 * @library /lib
 * @run main/othervm JtregJextract -l ArrayAccess --use-system-load-library -t test.jextract.arrayaccess array_access.h
 * @build TestArrayAccess
 * @run testng/othervm --enable-native-access=ALL-UNNAMED TestArrayAccess
 */

public class TestArrayAccess {
    @Test
    public void testArrayAccessStructInt1() {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment foo = Foo.allocate(arena);
            long[] dims = Foo.ints1$dimensions();
            for (int i = 0 ; i < dims[0] ; i++) {
                Foo.ints1(foo, i, i + 1);
                assertEquals(Foo.ints1(foo, i), i + 1);
            }
        }
    }

    @Test
    public void testArrayAccessStructInt2() {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment foo = Foo.allocate(arena);
            long[] dims = Foo.ints2$dimensions();
            for (int i = 0 ; i < dims[0] ; i++) {
                for (int j = 0; j < dims[1]; j++) {
                    Foo.ints2(foo, i, j, i + j + 1);
                    assertEquals(Foo.ints2(foo, i, j), i + j + 1);
                }
            }
        }
    }

    @Test
    public void testArrayAccessStructInt3() {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment foo = Foo.allocate(arena);
            long[] dims = Foo.ints3$dimensions();
            for (int i = 0 ; i < dims[0] ; i++) {
                for (int j = 0; j < dims[1]; j++) {
                    for (int k = 0; k < dims[2]; k++) {
                        Foo.ints3(foo, i, j, k, i + j + k + 1);
                        assertEquals(Foo.ints3(foo, i, j, k), i + j + k + 1);
                    }
                }
            }
        }
    }

    @Test
    public void testArrayAccessGlobalInt1() {
        long[] dims = ints1$dimensions();
        for (int i = 0 ; i < dims[0] ; i++) {
            ints1(i, i + 1);
            assertEquals(ints1(i), i + 1);
        }
    }

    @Test
    public void testArrayAccessGlobalInt2() {
        long[] dims = ints2$dimensions();
        for (int i = 0 ; i < dims[0] ; i++) {
            for (int j = 0; j < dims[1]; j++) {
                ints2(i, j, i + j + 1);
                assertEquals(ints2(i, j), i + j + 1);
            }
        }
    }

    @Test
    public void testArrayAccessGlobalInt3() {
        long[] dims = ints3$dimensions();
        for (int i = 0 ; i < dims[0] ; i++) {
            for (int j = 0; j < dims[1]; j++) {
                for (int k = 0; k < dims[2]; k++) {
                    ints3(i, j, k, i + j + k + 1);
                    assertEquals(ints3(i, j, k), i + j + k + 1);
                }
            }
        }
    }

    @Test
    public void testArrayAccessStructStruct1() {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment foo = Foo.allocate(arena);
            long[] dims = Foo.points1$dimensions();
            for (int i = 0 ; i < dims[0] ; i++) {
                MemorySegment point = Point.allocate(arena);
                Point.x(point, i + 1);
                Point.y(point, i + 2);
                Foo.points1(foo, i, point);
                assertEquals(Point.x(Foo.points1(foo, i)), i + 1);
                assertEquals(Point.y(Foo.points1(foo, i)), i + 2);
            }
        }
    }

    @Test
    public void testArrayAccessStructStruct2() {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment foo = Foo.allocate(arena);
            long[] dims = Foo.points2$dimensions();
            for (int i = 0 ; i < dims[0] ; i++) {
                for (int j = 0; j < dims[1]; j++) {
                    MemorySegment point = Point.allocate(arena);
                    Point.x(point, i + j + 1);
                    Point.y(point, i + j + 2);
                    Foo.points2(foo, i, j, point);
                    assertEquals(Point.x(Foo.points2(foo, i, j)), i + j + 1);
                    assertEquals(Point.y(Foo.points2(foo, i, j)), i + j + 2);
                }
            }
        }
    }

    @Test
    public void testArrayAccessStructStruct3() {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment foo = Foo.allocate(arena);
            long[] dims = Foo.points3$dimensions();
            for (int i = 0 ; i < dims[0] ; i++) {
                for (int j = 0; j < dims[1]; j++) {
                    for (int k = 0; k < dims[2]; k++) {
                        MemorySegment point = Point.allocate(arena);
                        Point.x(point, i + j + k + 1);
                        Point.y(point, i + j + k + 2);
                        Foo.points3(foo, i, j, k, point);
                        assertEquals(Point.x(Foo.points3(foo, i, j, k)), i + j + k + 1);
                        assertEquals(Point.y(Foo.points3(foo, i, j, k)), i + j + k + 2);
                    }
                }
            }
        }
    }

    @Test
    public void testArrayAccessGlobalStruct1() {
        try (Arena arena = Arena.ofConfined()) {
            long[] dims = points1$dimensions();
            for (int i = 0; i < dims[0]; i++) {
                MemorySegment point = Point.allocate(arena);
                Point.x(point, i + 1);
                Point.y(point, i + 2);
                points1(i, point);
                assertEquals(Point.x(points1(i)), i + 1);
                assertEquals(Point.y(points1(i)), i + 2);
            }
        }
    }

    @Test
    public void testArrayAccessGlobalStruct2() {
        try (Arena arena = Arena.ofConfined()) {
            long[] dims = points2$dimensions();
            for (int i = 0; i < dims[0]; i++) {
                for (int j = 0; j < dims[1]; j++) {
                    MemorySegment point = Point.allocate(arena);
                    Point.x(point, i + j + 1);
                    Point.y(point, i + j + 2);
                    points2(i, j, point);
                    assertEquals(Point.x(points2(i, j)), i + j + 1);
                    assertEquals(Point.y(points2(i, j)), i + j + 2);
                }
            }
        }
    }

    @Test
    public void testArrayAccessGlobalStruct3() {
        try (Arena arena = Arena.ofConfined()) {
            long[] dims = points3$dimensions();
            for (int i = 0 ; i < dims[0] ; i++) {
                for (int j = 0; j < dims[1]; j++) {
                    for (int k = 0; k < dims[2]; k++) {
                        MemorySegment point = Point.allocate(arena);
                        Point.x(point, i + j + k + 1);
                        Point.y(point, i + j + k + 2);
                        points3(i, j, k, point);
                        assertEquals(Point.x(points3(i, j, k)), i + j + k + 1);
                        assertEquals(Point.y(points3(i, j, k)), i + j + k + 2);
                    }
                }
            }
        }
    }
}
