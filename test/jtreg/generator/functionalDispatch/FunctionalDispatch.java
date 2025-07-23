/*
 * Copyright (c) 2025, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * @test
 * @library /lib
 * @build testlib.TestUtils
 * @bug 7903947
 * @summary test option to streamline access to function pointers
 * @run main/othervm JtregJextract
 *      --include-typedef DupStruct,functional
 *      --include-typedef Funcs,functional
 *      --include-typedef Simple,functional
 *      -t test.jextract.functional_dispatch functional_dispatch.h
 * @build FunctionalDispatch
 * @run testng/othervm --enable-native-access=ALL-UNNAMED FunctionalDispatch
 */

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import test.jextract.functional_dispatch.*;

import org.testng.annotations.Test;
import static org.testng.Assert.*;

public class FunctionalDispatch {
    @Test
    public void testSimpleStruct() {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment s = Simple.allocate(arena);
            assertEquals(Simple.value(s), 0);
            Simple.value(s, 999);
            assertEquals(Simple.value(s), 999);
        }
    }

    @Test
    public void testFuncsFunctionalDispatch() {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment f = Funcs.allocate(arena);

            Funcs.zero.Function zeroLam = () -> 7;
            MemorySegment zeroPtr = Funcs.zero.allocate(zeroLam, arena);
            Funcs.zero(f, zeroPtr);
            assertEquals(Funcs.zero(f), 7);

            Funcs.one.Function oneLam = x -> x * 3;
            MemorySegment onePtr = Funcs.one.allocate(oneLam, arena);
            Funcs.one(f, onePtr);
            assertEquals(Funcs.one(f, 5), 15);

            Funcs.sum.Function sumLam = (a, b) -> a + b;
            MemorySegment sumPtr = Funcs.sum.allocate(sumLam, arena);
            Funcs.sum(f, sumPtr);
            assertEquals(Funcs.sum(f, 100L, 23L), 123L);

            Funcs.make_point.Function mpLam = (x, y) -> {
                MemorySegment p = Funcs.Point.allocate(arena);
                Funcs.Point.x(p, x);
                Funcs.Point.y(p, y);
                return p;
            };
            MemorySegment mpPtr = Funcs.make_point.allocate(mpLam, arena);
            Funcs.make_point(f, mpPtr);
            MemorySegment p = Funcs.make_point(f, arena, 8, 9);
            assertEquals(Funcs.Point.x(p), 8);
            assertEquals(Funcs.Point.y(p), 9);
        }
    }

    @Test
    public void testDupStructFunctional() {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment ds = DupStruct.allocate(arena);

            DupStruct.dup.Function dupLam = () -> {
                MemorySegment s = Simple.allocate(arena);
                Simple.value(s, 555);
                return s;
            };
            MemorySegment dupPtr = DupStruct.dup.allocate(dupLam, arena);
            DupStruct.dup(ds, dupPtr);
            MemorySegment result = DupStruct.dup(ds, arena);
            assertEquals(Simple.value(result), 555);
        }
    }
}
