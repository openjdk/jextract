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

import org.testng.annotations.Test;
import jdk.incubator.foreign.MemorySegment;
import test.jextract.test8245003.*;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static test.jextract.test8245003.test8245003_h.*;
import static jdk.incubator.foreign.CLinker.*;

/*
 * @test id=classes
 * @bug 8245003
 * @summary jextract does not generate accessor for MemorySegement typed values
 * @library ../../lib
 * @run main/othervm JtregJextract -l Test8245003 -t test.jextract.test8245003 test8245003.h
 * @run testng/othervm --enable-native-access=ALL-UNNAMED Test8245003
 */
/*
 * @test id=sources
 * @bug 8245003
 * @summary jextract does not generate accessor for MemorySegement typed values
 * @library ../../lib
 * @run main/othervm JtregJextractSources -l Test8245003 -t test.jextract.test8245003 test8245003.h
 * @run testng/othervm --enable-native-access=ALL-UNNAMED Test8245003
 */
public class Test8245003 {
    @Test
    public void testStructAccessor() {
        var seg = special_pt$SEGMENT();
        assertEquals(seg.byteSize(), Point.sizeof());
        assertEquals(Point.x$get(seg), 56);
        assertEquals(Point.y$get(seg), 75);

        seg = special_pt3d$SEGMENT();
        assertEquals(seg.byteSize(), Point3D.sizeof());
        assertEquals(Point3D.z$get(seg), 35);
        var pointSeg = Point3D.p$slice(seg);
        assertEquals(pointSeg.byteSize(), Point.sizeof());
        assertEquals(Point.x$get(pointSeg), 43);
        assertEquals(Point.y$get(pointSeg), 45);
    }

    @Test
    public void testArrayAccessor() {
        var seg = iarr$SEGMENT();
        assertEquals(seg.byteSize(), C_INT.byteSize()*5);
        int[] arr = seg.toArray(C_INT);
        assertEquals(arr.length, 5);
        assertEquals(arr[0], 2);
        assertEquals(arr[1], -2);
        assertEquals(arr[2], 42);
        assertEquals(arr[3], -42);
        assertEquals(arr[4], 345);

        seg = foo$SEGMENT();
        assertEquals(seg.byteSize(), Foo.sizeof());
        assertEquals(Foo.count$get(seg), 37);
        var greeting = Foo.greeting$slice(seg);
        byte[] barr = greeting.toArray(C_CHAR);
        assertEquals(new String(barr), "hello");
    }
}
