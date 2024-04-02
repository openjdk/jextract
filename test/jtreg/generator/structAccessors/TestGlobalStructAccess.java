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
import test.jextract.globalaccess.*;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;

import static org.testng.Assert.*;
import static test.jextract.globalaccess.globalStructAccess_h.*;

/*
 * @test
 * @library /lib
 * @build testlib.TestUtils
 * @run main/othervm JtregJextract -l StructGlobal --use-system-load-library -t test.jextract.globalaccess globalStructAccess.h
 * @build TestGlobalStructAccess
 * @run testng/othervm --enable-native-access=ALL-UNNAMED TestGlobalStructAccess
 */
public class TestGlobalStructAccess {

    @Test
    public void testGlobalStructAccess() {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment gPoint = p();
            assertEquals(Point.x(gPoint), 1);
            assertEquals(Point.y(gPoint), 2);

            MemorySegment newPoint = allocatePoint(3, 4, arena);
            p(newPoint);
            assertEquals(Point.x(gPoint), 3);
            assertEquals(Point.y(gPoint), 4);
        }
    }

    static MemorySegment allocatePoint(int x, int y, Arena arena) {
        MemorySegment point = Point.allocate(arena);
        Point.x(point, x);
        Point.y(point, y);
        return point;
    }
}
