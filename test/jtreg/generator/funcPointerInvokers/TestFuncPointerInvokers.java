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

import java.lang.foreign.MemoryAddress;
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
        try (MemorySession scope = MemorySession.openConfined()) {
            AtomicInteger val = new AtomicInteger(-1);
            MemorySegment bar = Bar.allocate(scope);
            Bar.foo$set(bar, Foo.allocate((i) -> val.set(i), scope).address());
            Bar.foo(bar, scope).apply(42);
            assertEquals(val.get(), 42);
        }
    }

    @Test
    public void testStructFieldFITypedef() {
        try (MemorySession scope = MemorySession.openConfined()) {
            AtomicInteger val = new AtomicInteger(-1);
            MemorySegment bar = Bar.allocate(scope);
            Bar.foo$set(bar, Foo.allocate((i) -> val.set(i), scope).address());
            Foo.ofAddress(Bar.foo$get(bar), scope).apply(42);
            assertEquals(val.get(), 42);
        }
    }

    @Test
    public void testGlobalTypedef() {
        try (MemorySession scope = MemorySession.openConfined()) {
            AtomicInteger val = new AtomicInteger(-1);
            f$set(Foo.allocate((i) -> val.set(i), scope).address());
            f().apply(42);
            assertEquals(val.get(), 42);
        }
    }

    @Test
    public void testGlobalFITypedef() {
        try (MemorySession scope = MemorySession.openConfined()) {
            AtomicInteger val = new AtomicInteger(-1);
            f$set(Foo.allocate((i) -> val.set(i), scope).address());
            Foo.ofAddress(f$get(), scope).apply(42);
            assertEquals(val.get(), 42);
        }
    }

    @Test
    public void testStructFieldFunctionPointer() {
        try (MemorySession scope = MemorySession.openConfined()) {
            AtomicInteger val = new AtomicInteger(-1);
            MemorySegment baz = Baz.allocate(scope);
            Baz.fp$set(baz, Baz.fp.allocate((i) -> val.set(i), scope).address());
            Baz.fp(baz, scope).apply(42);
            assertEquals(val.get(), 42);
        }
    }

    @Test
    public void testStructFieldFIFunctionPointer() {
        try (MemorySession scope = MemorySession.openConfined()) {
            AtomicInteger val = new AtomicInteger(-1);
            MemorySegment baz = Baz.allocate(scope);
            Baz.fp$set(baz, Baz.fp.allocate((i) -> val.set(i), scope).address());
            Baz.fp.ofAddress(Baz.fp$get(baz), scope).apply(42);
            assertEquals(val.get(), 42);
        }
    }

    @Test
    public void testGlobalFunctionPointer() {
        try (MemorySession scope = MemorySession.openConfined()) {
            AtomicInteger val = new AtomicInteger(-1);
            fp$set(fp.allocate((i) -> val.set(i), scope).address());
            fp().apply(42);
            assertEquals(val.get(), 42);
        }
    }

    @Test
    public void testGlobalFIFunctionPointer() {
        try (MemorySession scope = MemorySession.openConfined()) {
            AtomicInteger val = new AtomicInteger(-1);
            fp$set(fp.allocate((i) -> val.set(i), scope).address());
            fp.ofAddress(fp$get(), scope).apply(42);
            assertEquals(val.get(), 42);
        }
    }

    @Test
    public void testGlobalFIFunctionPointerAddress() {
        try (MemorySession scope = MemorySession.openConfined()) {
            fp_addr$set(fp_addr.allocate((addr) -> MemoryAddress.ofLong(addr.toRawLongValue() + 1), scope).address());
            assertEquals(fp_addr.ofAddress(fp_addr$get(), scope).apply(MemoryAddress.ofLong(42)), MemoryAddress.ofLong(43));
        }
    }
}
