/*
 *  Copyright (c) 2020, Oracle and/or its affiliates. All rights reserved.
 *  DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 *  This code is free software; you can redistribute it and/or modify it
 *  under the terms of the GNU General Public License version 2 only, as
 *  published by the Free Software Foundation.  Oracle designates this
 *  particular file as subject to the "Classpath" exception as provided
 *  by Oracle in the LICENSE file that accompanied this code.
 *
 *  This code is distributed in the hope that it will be useful, but WITHOUT
 *  ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 *  FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 *  version 2 for more details (a copy is included in the LICENSE file that
 *  accompanied this code).
 *
 *  You should have received a copy of the GNU General Public License version
 *  2 along with this work; if not, write to the Free Software Foundation,
 *  Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 *   Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 *  or visit www.oracle.com if you need additional information or have any
 *  questions.
 *
 */
package org.openjdk.jextract.clang;

import jdk.incubator.foreign.Addressable;
import jdk.incubator.foreign.CLinker;
import jdk.incubator.foreign.FunctionDescriptor;
import jdk.incubator.foreign.MemoryAddress;
import jdk.incubator.foreign.MemorySegment;
import jdk.incubator.foreign.ResourceScope;
import jdk.incubator.foreign.SegmentAllocator;
import org.openjdk.jextract.clang.libclang.CXString;
import org.openjdk.jextract.clang.libclang.Index_h;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodType;
import java.util.function.Function;
import java.util.function.Supplier;

import static org.openjdk.jextract.clang.libclang.Index_h.C_INT;
import static org.openjdk.jextract.clang.libclang.Index_h.C_POINTER;

public class LibClang {
    private static final boolean DEBUG = Boolean.getBoolean("libclang.debug");
    private static final boolean CRASH_RECOVERY = Boolean.getBoolean("libclang.crash_recovery");
    private static final boolean IS_WINDOWS = System.getProperty("os.name").startsWith("Windows");

    final static SegmentAllocator IMPLICIT_ALLOCATOR =
            (size, align) -> MemorySegment.allocateNative(size, align, ResourceScope.newImplicitScope());

    private final static MemorySegment disableCrashRecovery =
            IMPLICIT_ALLOCATOR.allocateUtf8String("LIBCLANG_DISABLE_CRASH_RECOVERY=" + CRASH_RECOVERY);

    static {
        if (!CRASH_RECOVERY) {
            //this is an hack - needed because clang_toggleCrashRecovery only takes effect _after_ the
            //first call to createIndex.
            try {
                CLinker linker = CLinker.systemCLinker();
                String putenv = IS_WINDOWS ? "_putenv" : "putenv";
                MethodHandle PUT_ENV = linker.downcallHandle(linker.lookup(putenv).get(),
                                FunctionDescriptor.of(C_INT, C_POINTER));
                int res = (int) PUT_ENV.invokeExact((Addressable)disableCrashRecovery);
            } catch (Throwable ex) {
                throw new ExceptionInInitializerError(ex);
            }
        }
    }

    public static Index createIndex(boolean local) {
        Index index = new Index(Index_h.clang_createIndex(local ? 1 : 0, 0));
        if (DEBUG) {
            System.err.println("LibClang crash recovery " + (CRASH_RECOVERY ? "enabled" : "disabled"));
        }
        return index;
    }

    public static String CXStrToString(Function<SegmentAllocator, MemorySegment> segmentSupplier) {
        MemorySegment cxstr = segmentSupplier.apply(STRING_ALLOCATOR);
        MemoryAddress buf = Index_h.clang_getCString(cxstr);
        String str = buf.getUtf8String(0);
        Index_h.clang_disposeString(cxstr);
        return str;
    }

    /**
     * This is an allocator for temporary CXString structs. CXStrToString needs to save the CXString somewhere,
     * so that we can extract a Java string out of it. Once that's done, we can dispose the CXString, and the
     * associated segment. Since jextract is single-threaded, we can use a prefix allocator, to speed up string
     * conversion. The size of the prefix segment is set to 256, which should be enough to hold a CXString.
     */
    private final static SegmentAllocator STRING_ALLOCATOR = SegmentAllocator.prefixAllocator(
            MemorySegment.allocateNative(CXString.sizeof(), 8, ResourceScope.newImplicitScope()));

    public static String version() {
        return CXStrToString(Index_h::clang_getClangVersion);
    }
}
