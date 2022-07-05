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

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.Linker;
import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.SegmentAllocator;
import org.openjdk.jextract.clang.libclang.CXString;
import org.openjdk.jextract.clang.libclang.Index_h;

import java.lang.invoke.MethodHandle;

import static org.openjdk.jextract.clang.libclang.Index_h.C_INT;
import static org.openjdk.jextract.clang.libclang.Index_h.C_POINTER;

public class LibClang {
    private static final boolean DEBUG = Boolean.getBoolean("libclang.debug");
    private static final boolean IS_WINDOWS = System.getProperty("os.name").startsWith("Windows");
    // crash recovery is not an issue on Windows, so enable it there by default to work around a libclang issue with reparseTranslationUnit
    private static final boolean CRASH_RECOVERY = IS_WINDOWS || Boolean.getBoolean("libclang.crash_recovery");

    private static final SegmentAllocator IMPLICIT_ALLOCATOR = MemorySegment::allocateNative;

    private final static MemorySegment disableCrashRecovery =
            IMPLICIT_ALLOCATOR.allocateUtf8String("LIBCLANG_DISABLE_CRASH_RECOVERY=" + CRASH_RECOVERY);

    static {
        if (!CRASH_RECOVERY) {
            //this is an hack - needed because clang_toggleCrashRecovery only takes effect _after_ the
            //first call to createIndex.
            try {
                Linker linker = Linker.nativeLinker();
                String putenv = IS_WINDOWS ? "_putenv" : "putenv";
                MethodHandle PUT_ENV = linker.downcallHandle(linker.defaultLookup().lookup(putenv).get(),
                                FunctionDescriptor.of(C_INT, C_POINTER));
                int res = (int) PUT_ENV.invokeExact((MemorySegment)disableCrashRecovery);
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

    public static String CXStrToString(MemorySegment cxstr) {
        MemorySegment buf = Index_h.clang_getCString(cxstr);
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
    public final static SegmentAllocator STRING_ALLOCATOR = SegmentAllocator.prefixAllocator(
            MemorySegment.allocateNative(CXString.sizeof(), 8));

    public static String version() {
        var clangVersion = Index_h.clang_getClangVersion(STRING_ALLOCATOR);
        return CXStrToString(clangVersion);
    }
}
