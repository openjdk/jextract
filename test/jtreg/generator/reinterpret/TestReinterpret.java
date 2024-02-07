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

import org.testng.annotations.Test;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;
import static test.jextract.reinterpret.reinterpret_h.*;
import test.jextract.reinterpret.*;

/*
 * @test
 * @bug 8253102 7903626
 * @summary jextract should emit reinterpret utility method on struct classes
 * @library /lib
 * @run main/othervm JtregJextract -l Reinterpret,lookup=loaderLookup -t test.jextract.reinterpret reinterpret.h
 * @build TestReinterpret
 * @run testng/othervm --enable-native-access=ALL-UNNAMED TestReinterpret
 */
public class TestReinterpret {
    @Test
    public void testSingleStruct() {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment addr = make(14, 99);
            MemorySegment seg = Point.reinterpret(addr, arena, reinterpret_h::freePoint);
            assertEquals(Point.x(seg), 14);
            assertEquals(Point.y(seg), 99);
        }
    }

    @Test
    public void testStructArray() {
        try (Arena arena = Arena.ofConfined()) {
            int elementCount = 10;
            MemorySegment addr = makeArray(elementCount);
            MemorySegment array = Point.reinterpret(addr, elementCount, arena, reinterpret_h::freePoint);
            for (int i = 0; i < elementCount; i++) {
                MemorySegment point = Point.asSlice(array, i);
                assertEquals(Point.x(point), i);
                assertEquals(Point.y(point), i + 1);
            }
        }
    }
}
