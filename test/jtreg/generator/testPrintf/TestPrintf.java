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

import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.lang.foreign.MemoryLayout;
import java.lang.foreign.MemorySegment;

import static org.testng.Assert.assertEquals;
import static test.jextract.printf.printf_h.*;

/*
 * @test
 *
 * @library /lib
 * @run main/othervm JtregJextract -t test.jextract.printf -l Printf printf.h
 * @build TestPrintf
 * @run testng/othervm --enable-native-access=ALL-UNNAMED TestPrintf
 */
public class TestPrintf {

    @Test(dataProvider = "cases")
    public void testsPrintf(String fmt, Object[] args, String expected, MemoryLayout[] unused) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment s = arena.allocate(1024);
            my_sprintf(s, arena.allocateFrom(fmt), args.length, args);
            String str = s.getString(0);
            assertEquals(str, expected);
        }
    }

    @Test(dataProvider = "cases")
    public void testsPrintfInvoker(String fmt, Object[] args, String expected, MemoryLayout[] layouts) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment s = arena.allocate(1024);
            my_sprintf$makeInvoker(layouts)
                .my_sprintf(s, arena.allocateFrom(fmt), args.length, args);
            String str = s.getString(0);
            assertEquals(str, expected);
        }
    }

    @Test(dataProvider = "wrongArgsCases", expectedExceptions = IllegalArgumentException.class)
    public void testsPrintfInvokerWrongArgs(String fmt, MemoryLayout[] layouts, Object[] args) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment s = arena.allocate(1024);
            my_sprintf$makeInvoker(layouts)
                .my_sprintf(s, arena.allocateFrom(fmt), args.length, args); // should throw
        }
    }

    // linker does not except unpromoted layouts
    @Test(dataProvider = "illegalLinkCases", expectedExceptions = IllegalArgumentException.class)
    public void testsPrintfInvokerWrongArgs(MemoryLayout[] layouts) {
        my_sprintf$makeInvoker(layouts); // should throw
    }

    // data providers:
    @DataProvider
    public Object[][] cases() {
        return new Object[][]{
            {
                "%hhd %c %.2f %.2f %lld %lld %d %hd %d %d %lld %c",
                new Object[] {(byte) 1, 'b', -1.25f, 5.5d, -200L, Long.MAX_VALUE, (byte) -2, (short) 2, 3, (short) -4, 5L, 'a' },
                "1 b -1.25 5.50 -200 " + Long.MAX_VALUE + " -2 2 3 -4 5 a",
                new MemoryLayout[] { C_INT, C_INT, C_DOUBLE, C_DOUBLE, C_LONG_LONG, C_LONG_LONG, C_INT, C_INT,
                        C_INT, C_INT, C_LONG_LONG, C_INT }
            }
        };
    }

    @DataProvider
    public Object[][] wrongArgsCases() {
        return new Object[][] {
            {
                "%d", new MemoryLayout[] {C_INT}, new Object[0], // too few args
                "%d", new MemoryLayout[] {C_INT}, new Object[] { 1, 2 }, // too many args
                "%.2f", new MemoryLayout[] {C_DOUBLE}, new Object[] { 1 }, // wrong type
            }
        };
    }

    @DataProvider
    public static Object[][] illegalLinkCases() {
        return new Object[][]{
                {new MemoryLayout[]{ C_CHAR }},
                {new MemoryLayout[]{ C_SHORT }},
                {new MemoryLayout[]{ C_BOOL }},
                {new MemoryLayout[]{ C_FLOAT }}
        };
    }
}
