/*
 * Copyright (c) 2021, Oracle and/or its affiliates. All rights reserved.
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

import java.lang.foreign.MemorySegment;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.MemorySession;
import org.testng.annotations.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.testng.Assert.*;

import static test.jextract.funcpointers.func_h.*;
import test.jextract.funcpointers.*;

/*
 * @test id=classes
  * @library /lib
 * @run main/othervm JtregJextract -l Func -t test.jextract.funcpointers func.h
 * @run testng/othervm --enable-native-access=ALL-UNNAMED TestFuncPointerInvokers
 */
/*
 * @test id=sources
  * @library /lib
 * @run main/othervm JtregJextract -l Func -t test.jextract.funcpointers func.h
 * @run testng/othervm --enable-native-access=ALL-UNNAMED TestFuncPointerInvokers
 */
public class TestFuncPointerInvokers {
    @Test
    public void testStructFieldTypedef() {
        try (MemorySession session = MemorySession.openConfined()) {
            AtomicInteger val = new AtomicInteger(-1);
            MemorySegment bar = Bar.allocate(session);
            Bar.foo$set(bar, Foo.allocate((i) -> val.set(i), session));
            Bar.foo(bar, session).apply(42);
            assertEquals(val.get(), 42);
        }
    }

    @Test
    public void testStructFieldFITypedef() {
        try (MemorySession session = MemorySession.openConfined()) {
            AtomicInteger val = new AtomicInteger(-1);
            MemorySegment bar = Bar.allocate(session);
            Bar.foo$set(bar, Foo.allocate((i) -> val.set(i), session));
            Foo.ofAddress(Bar.foo$get(bar), session).apply(42);
            assertEquals(val.get(), 42);
        }
    }

    @Test
    public void testGlobalTypedef() {
        try (MemorySession session = MemorySession.openConfined()) {
            AtomicInteger val = new AtomicInteger(-1);
            f$set(Foo.allocate((i) -> val.set(i), session));
            f().apply(42);
            assertEquals(val.get(), 42);
        }
    }

    @Test
    public void testGlobalFITypedef() {
        try (MemorySession session = MemorySession.openConfined()) {
            AtomicInteger val = new AtomicInteger(-1);
            f$set(Foo.allocate((i) -> val.set(i), session));
            Foo.ofAddress(f$get(), session).apply(42);
            assertEquals(val.get(), 42);
        }
    }

    @Test
    public void testStructFieldFunctionPointer() {
        try (MemorySession session = MemorySession.openConfined()) {
            AtomicInteger val = new AtomicInteger(-1);
            MemorySegment baz = Baz.allocate(session);
            Baz.fp$set(baz, Baz.fp.allocate((i) -> val.set(i), session));
            Baz.fp(baz, session).apply(42);
            assertEquals(val.get(), 42);
        }
    }

    @Test
    public void testStructFieldFIFunctionPointer() {
        try (MemorySession session = MemorySession.openConfined()) {
            AtomicInteger val = new AtomicInteger(-1);
            MemorySegment baz = Baz.allocate(session);
            Baz.fp$set(baz, Baz.fp.allocate((i) -> val.set(i), session));
            Baz.fp.ofAddress(Baz.fp$get(baz), session).apply(42);
            assertEquals(val.get(), 42);
        }
    }

    @Test
    public void testGlobalFunctionPointer() {
        try (MemorySession session = MemorySession.openConfined()) {
            AtomicInteger val = new AtomicInteger(-1);
            fp$set(fp.allocate((i) -> val.set(i), session));
            fp().apply(42);
            assertEquals(val.get(), 42);
        }
    }

    @Test
    public void testGlobalFIFunctionPointer() {
        try (MemorySession session = MemorySession.openConfined()) {
            AtomicInteger val = new AtomicInteger(-1);
            fp$set(fp.allocate((i) -> val.set(i), session));
            fp.ofAddress(fp$get(), session).apply(42);
            assertEquals(val.get(), 42);
        }
    }

    @Test
    public void testGlobalFIFunctionPointerAddress() {
        try (MemorySession session = MemorySession.openConfined()) {
            fp_addr$set(fp_addr.allocate((addr) -> MemorySegment.ofAddress(addr.address() + 1), session));
            assertEquals(fp_addr.ofAddress(fp_addr$get(), session).apply(MemorySegment.ofAddress(42)), MemorySegment.ofAddress(43));
        }
    }
}
