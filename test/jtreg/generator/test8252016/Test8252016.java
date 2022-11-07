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

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySession;
import java.lang.foreign.SegmentAllocator;
import java.lang.foreign.VaList;
import org.testng.annotations.Test;

import java.lang.foreign.MemorySegment;

import static org.testng.Assert.assertEquals;
import static test.jextract.vsprintf.vsprintf_h.*;

/*
 * @test id=classes
 * @bug 8252016
 * @summary jextract should handle va_list
 * @library /lib
 * @run main/othervm JtregJextract -t test.jextract.vsprintf -l VSPrintf vsprintf.h
 * @run testng/othervm --enable-native-access=ALL-UNNAMED Test8252016
 */
/*
 * @test id=sources
 * @bug 8252016
 * @summary jextract should handle va_list
 * @library /lib
 * @run main/othervm JtregJextractSources -t test.jextract.vsprintf -l VSPrintf vsprintf.h
 * @run testng/othervm --enable-native-access=ALL-UNNAMED Test8252016
 */
public class Test8252016 {
    @Test
    public void testsVsprintf() {
        try (Arena arena = Arena.openConfined()) {
            MemorySegment s = arena.allocate(1024);
            VaList vaList = VaList.make(b -> {
                b.addVarg(C_INT, 12);
                b.addVarg(C_DOUBLE, 5.5d);
                b.addVarg(C_LONG_LONG, -200L);
                b.addVarg(C_LONG_LONG, Long.MAX_VALUE);
            }, arena.session());
            my_vsprintf(s, arena.allocateUtf8String("%hhd %.2f %lld %lld"), vaList.segment());
            String str = s.getUtf8String(0);
            assertEquals(str, "12 5.50 -200 " + Long.MAX_VALUE);
       }
    }
}
