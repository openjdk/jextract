/*
 * Copyright (c) 2025, Oracle and/or its affiliates. All rights reserved.
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
import java.lang.foreign.MemorySegment;
import org.testng.annotations.Test;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.assertFalse;
import test.jextract.testStructArrayFields.*;

/*
 * @test
 * @library /lib
 * @build testlib.TestUtils
 * @run main/othervm JtregJextract -l TestStructArrayFields --use-system-load-library -t test.jextract.testStructArrayFields testStructArrayFields.h
 * @build TestStructArrayFields
 * @run testng/othervm --enable-native-access=ALL-UNNAMED TestStructArrayFields
 */
public class TestStructArrayFields {
    @Test
    void testStructArrayNoClobber() {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment rec = Record_st.allocate(arena);

            Record_st.a(rec, 0x11111111);
            Record_st.b(rec, 0x22222222);

            Record_st.arr(rec, 0, 10);
            Record_st.arr(rec, 1, 20);
            Record_st.arr(rec, 2, 30);

            assertEquals(0x11111111, Record_st.a(rec));
            assertEquals(0x22222222, Record_st.b(rec));
            assertEquals(10, Record_st.arr(rec, 0));
            assertEquals(20, Record_st.arr(rec, 1));
            assertEquals(30, Record_st.arr(rec, 2));
        }
    }

    @Test
    void testUnionArrayNoClobber() {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment rec = Record_st.allocate(arena);

            Record_st.a(rec, 0xAAAAAAAA);
            Record_st.b(rec, 0xBBBBBBBB);
            Record_st.tail(rec, 0x7F7F7F7F);
            Record_st.flag(rec, false);

            MemorySegment u = Record_st.u(rec);

            InnerU.ua(u, 0, 101);
            InnerU.ua(u, 1, 202);
            InnerU.ua(u, 2, 303);

            assertEquals(0xAAAAAAAA, Record_st.a(rec));
            assertEquals(0xBBBBBBBB, Record_st.b(rec));
            assertEquals(0x7F7F7F7F, Record_st.tail(rec));
            assertFalse(Record_st.flag(rec));

            assertEquals(101, InnerU.ua(u, 0));
            assertEquals(202, InnerU.ua(u, 1));
            assertEquals(303, InnerU.ua(u, 2));
        }
    }

    @Test
    void testUnionStructSlice() {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment rec = Record_st.allocate(arena);
            MemorySegment u = Record_st.u(rec);

            MemorySegment is = InnerU.is(u);

            InnerS.s(is, (short) 0x1234);
            InnerS.c(is, (byte) 0x7F);

            Record_st.tail(rec, 0x01020304);
            Record_st.flag(rec, true);

            assertEquals((short) 0x1234, InnerS.s(is));
            assertEquals((byte) 0x7F, InnerS.c(is));

            assertEquals(0x01020304, Record_st.tail(rec));
            assertTrue(Record_st.flag(rec));
        }
    }

    @Test
    void testOffsetsAndSizes() {
        long offA = Record_st.a$offset();
        long offB = Record_st.b$offset();
        long offArr = Record_st.arr$offset();
        long offU = Record_st.u$offset();
        long offTail = Record_st.tail$offset();
        long offFlag = Record_st.flag$offset();

        assertTrue(offA < offB);
        assertTrue(offB < offArr);
        assertTrue(offArr < offU);
        assertTrue(offU < offTail);
        assertTrue(offTail < offFlag);

        assertEquals(C_INT.byteSize() * 3L, Record_st.arr$layout().byteSize());
        assertEquals(C_INT.byteSize() * 3L, InnerU.ua$layout().byteSize());
    }

    @Test
    void testStructOfStructArrayNoClobber() {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment rec = Record_st.allocate(arena);

            Record_st.a(rec, 0x11111111);
            Record_st.b(rec, 0x22222222);
            Record_st.tail(rec, 0x7F7F7F7F);
            Record_st.flag(rec, false);

            MemorySegment sa0 = Record_st.sa(rec, 0);
            MemorySegment sa1 = Record_st.sa(rec, 1);

            InnerS.s(sa0, (short) 0x1234);
            InnerS.c(sa0, (byte) 0x55);
            InnerS.s(sa1, (short) 0xABCD);
            InnerS.c(sa1, (byte) 0x7F);

            assertEquals(0x11111111, Record_st.a(rec));
            assertEquals(0x22222222, Record_st.b(rec));
            assertEquals(0x7F7F7F7F, Record_st.tail(rec));
            assertFalse(Record_st.flag(rec));

            assertEquals((short) 0x1234, InnerS.s(sa0));
            assertEquals((byte) 0x55, InnerS.c(sa0));
            assertEquals((short) 0xABCD, InnerS.s(sa1));
            assertEquals((byte) 0x7F, InnerS.c(sa1));
        }
    }

    @Test
    void testUnionStructArrayNoClobber() {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment rec = Record_st.allocate(arena);

            Record_st.a(rec, 0xAAAAAAAA);
            Record_st.b(rec, 0xBBBBBBBB);
            Record_st.tail(rec, 0x01020304);
            Record_st.flag(rec, true);

            MemorySegment u = Record_st.u(rec);

            MemorySegment ia0 = InnerU.ia(u, 0);
            MemorySegment ia1 = InnerU.ia(u, 1);

            InnerS.s(ia0, (short) 0x0001);
            InnerS.c(ia0, (byte) 0x11);
            InnerS.s(ia1, (short) 0x0002);
            InnerS.c(ia1, (byte) 0x22);

            assertEquals(0xAAAAAAAA, Record_st.a(rec));
            assertEquals(0xBBBBBBBB, Record_st.b(rec));
            assertEquals(0x01020304, Record_st.tail(rec));
            assertTrue(Record_st.flag(rec));

            assertEquals((short) 0x0001, InnerS.s(ia0));
            assertEquals((byte) 0x11, InnerS.c(ia0));
            assertEquals((short) 0x0002, InnerS.s(ia1));
            assertEquals((byte) 0x22, InnerS.c(ia1));
        }
    }
}
