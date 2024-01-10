/*
 *  Copyright (c) 2023, Oracle and/or its affiliates. All rights reserved.
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
 */

// Generated by jextract

package org.openjdk.jextract.clang.libclang;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.nio.ByteOrder;
import java.lang.foreign.*;
import static java.lang.foreign.ValueLayout.*;

/**
 * {@snippet lang=c :
 * struct {
 *     enum CXTypeKind kind;
 *     void* data[2];
 * };
 * }
 */
public class CXType {

    CXType() {
        // Suppresses public default constructor, ensuring non-instantiability,
        // but allows generated subclasses in same package.
    }

    private static final GroupLayout $LAYOUT = MemoryLayout.structLayout(
        Index_h.C_INT.withName("kind"),
        MemoryLayout.paddingLayout(4),
        MemoryLayout.sequenceLayout(2, Index_h.C_POINTER).withName("data")
    ).withName("$anon$3431:9");

    public static final GroupLayout $LAYOUT() {
        return $LAYOUT;
    }

    private static final long kind$OFFSET = 0;

    /**
     * Getter for field:
     * {@snippet lang=c :
     * enum CXTypeKind kind;
     * }
     */
    public static int kind$get(MemorySegment seg) {
        return seg.get(Index_h.C_INT, kind$OFFSET);
    }

    public static int kind$get(MemorySegment seg, long index) {
        return seg.get(Index_h.C_INT, kind$OFFSET + (index * sizeof()));
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * enum CXTypeKind kind;
     * }
     */
    public static void kind$set(MemorySegment seg, int x) {
        seg.set(Index_h.C_INT, kind$OFFSET, x);
    }

    public static void kind$set(MemorySegment seg, long index, int x) {
        seg.set(Index_h.C_INT, kind$OFFSET + (index * sizeof()), x);
    }

    private static final long data$OFFSET = 8;
    private static final long data$SIZE = 16;

    public static MemorySegment data$slice(MemorySegment seg) {
        return seg.asSlice(data$OFFSET, data$SIZE);
    }

    public static long sizeof() { return $LAYOUT().byteSize(); }
    public static MemorySegment allocate(SegmentAllocator allocator) { return allocator.allocate($LAYOUT()); }

    public static MemorySegment allocateArray(long len, SegmentAllocator allocator) {
        return allocator.allocate(MemoryLayout.sequenceLayout(len, $LAYOUT()));
    }

    public static MemorySegment ofAddress(MemorySegment addr, Arena scope) {
        return addr.reinterpret($LAYOUT().byteSize(), scope, null);
    }
}

