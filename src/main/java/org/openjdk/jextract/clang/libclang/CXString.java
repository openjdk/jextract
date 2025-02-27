/*
 *  Copyright (c) 2023, 2024, Oracle and/or its affiliates. All rights reserved.
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

import java.lang.invoke.*;
import java.lang.foreign.*;
import java.nio.ByteOrder;
import java.util.*;
import java.util.function.*;
import java.util.stream.*;

import static java.lang.foreign.ValueLayout.*;
import static java.lang.foreign.MemoryLayout.PathElement.*;

/**
 * {@snippet lang=c :
 * struct {
 *     const void *data;
 *     unsigned int private_flags;
 * }
 * }
 */
public class CXString {

    CXString() {
        // Should not be called directly
    }

    private static final GroupLayout $LAYOUT = MemoryLayout.structLayout(
        Index_h.C_POINTER.withName("data"),
        Index_h.C_INT.withName("private_flags"),
        MemoryLayout.paddingLayout(4)
    ).withName("$anon$37:9");

    /**
     * The layout of this struct
     */
    public static final GroupLayout layout() {
        return $LAYOUT;
    }

    private static final AddressLayout data$LAYOUT = (AddressLayout)$LAYOUT.select(groupElement("data"));

    /**
     * Layout for field:
     * {@snippet lang=c :
     * const void *data
     * }
     */
    public static final AddressLayout data$layout() {
        return data$LAYOUT;
    }

    private static final long data$OFFSET = 0;

    /**
     * Offset for field:
     * {@snippet lang=c :
     * const void *data
     * }
     */
    public static final long data$offset() {
        return data$OFFSET;
    }

    /**
     * Getter for field:
     * {@snippet lang=c :
     * const void *data
     * }
     */
    public static MemorySegment data(MemorySegment struct) {
        return struct.get(data$LAYOUT, data$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * const void *data
     * }
     */
    public static void data(MemorySegment struct, MemorySegment fieldValue) {
        struct.set(data$LAYOUT, data$OFFSET, fieldValue);
    }

    private static final OfInt private_flags$LAYOUT = (OfInt)$LAYOUT.select(groupElement("private_flags"));

    /**
     * Layout for field:
     * {@snippet lang=c :
     * unsigned int private_flags
     * }
     */
    public static final OfInt private_flags$layout() {
        return private_flags$LAYOUT;
    }

    private static final long private_flags$OFFSET = 8;

    /**
     * Offset for field:
     * {@snippet lang=c :
     * unsigned int private_flags
     * }
     */
    public static final long private_flags$offset() {
        return private_flags$OFFSET;
    }

    /**
     * Getter for field:
     * {@snippet lang=c :
     * unsigned int private_flags
     * }
     */
    public static int private_flags(MemorySegment struct) {
        return struct.get(private_flags$LAYOUT, private_flags$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * unsigned int private_flags
     * }
     */
    public static void private_flags(MemorySegment struct, int fieldValue) {
        struct.set(private_flags$LAYOUT, private_flags$OFFSET, fieldValue);
    }

    /**
     * Obtains a slice of {@code arrayParam} which selects the array element at {@code index}.
     * The returned segment has address {@code arrayParam.address() + index * layout().byteSize()}
     */
    public static MemorySegment asSlice(MemorySegment array, long index) {
        return array.asSlice(layout().byteSize() * index);
    }

    /**
     * The size (in bytes) of this struct
     */
    public static long sizeof() { return layout().byteSize(); }

    /**
     * Allocate a segment of size {@code layout().byteSize()} using {@code allocator}
     */
    public static MemorySegment allocate(SegmentAllocator allocator) {
        return allocator.allocate(layout());
    }

    /**
     * Allocate an array of size {@code elementCount} using {@code allocator}.
     * The returned segment has size {@code elementCount * layout().byteSize()}.
     */
    public static MemorySegment allocateArray(long elementCount, SegmentAllocator allocator) {
        return allocator.allocate(MemoryLayout.sequenceLayout(elementCount, layout()));
    }

    /**
     * Reinterprets {@code addr} using target {@code arena} and {@code cleanupAction} (if any).
     * The returned segment has size {@code layout().byteSize()}
     */
    public static MemorySegment reinterpret(MemorySegment addr, Arena arena, Consumer<MemorySegment> cleanup) {
        return reinterpret(addr, 1, arena, cleanup);
    }

    /**
     * Reinterprets {@code addr} using target {@code arena} and {@code cleanupAction} (if any).
     * The returned segment has size {@code elementCount * layout().byteSize()}
     */
    public static MemorySegment reinterpret(MemorySegment addr, long elementCount, Arena arena, Consumer<MemorySegment> cleanup) {
        return addr.reinterpret(layout().byteSize() * elementCount, arena, cleanup);
    }
}

