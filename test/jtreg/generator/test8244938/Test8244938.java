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
import static org.testng.Assert.assertEquals;
import static test.jextract.test8244938.test8244938_h.*;
import test.jextract.test8244938.*;

/*
 * @test
 * @bug 8244938
 * @summary Crash in foreign ABI CallArranger class when a test native function returns a nested struct
 * @library /lib
 * @run main/othervm JtregJextract -l Test8244938 --use-system-load-library -t test.jextract.test8244938 test8244938.h
 * @build Test8244938
 * @run testng/othervm --enable-native-access=ALL-UNNAMED Test8244938
 */
public class Test8244938 {
    @Test
    public void testNestedStructReturn() {
         try (Arena arena = Arena.ofConfined()) {
             var seg = func(arena);
             assertEquals(seg.byteSize(), Point.sizeof());
             assertEquals(Point.k(seg), 44);
             var point2dSeg = Point.point2d(seg);
             assertEquals(Point2D.i(point2dSeg), 567);
             assertEquals(Point2D.j(point2dSeg), 33);
         }
    }
}
