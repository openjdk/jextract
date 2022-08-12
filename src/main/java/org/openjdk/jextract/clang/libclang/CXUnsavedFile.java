/*
 *  Copyright (c) 2022, Oracle and/or its affiliates. All rights reserved.
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
import java.lang.invoke.VarHandle;
import java.nio.ByteOrder;
import java.lang.foreign.*;
import static java.lang.foreign.ValueLayout.*;
public class CXUnsavedFile {

    static final  GroupLayout $struct$LAYOUT = MemoryLayout.structLayout(
        Constants$root.C_POINTER$LAYOUT.withName("Filename"),
        Constants$root.C_POINTER$LAYOUT.withName("Contents"),
        Constants$root.C_LONG_LONG$LAYOUT.withName("Length")
    ).withName("CXUnsavedFile");
    public static MemoryLayout $LAYOUT() {
        return CXUnsavedFile.$struct$LAYOUT;
    }
    static final VarHandle Filename$VH = $struct$LAYOUT.varHandle(MemoryLayout.PathElement.groupElement("Filename"));
    public static VarHandle Filename$VH() {
        return CXUnsavedFile.Filename$VH;
    }
    public static MemorySegment Filename$get(MemorySegment seg) {
        return (java.lang.foreign.MemorySegment)CXUnsavedFile.Filename$VH.get(seg);
    }
    public static void Filename$set( MemorySegment seg, MemorySegment x) {
        CXUnsavedFile.Filename$VH.set(seg, x);
    }
    public static MemorySegment Filename$get(MemorySegment seg, long index) {
        return (java.lang.foreign.MemorySegment)CXUnsavedFile.Filename$VH.get(seg.asSlice(index*sizeof()));
    }
    public static void Filename$set(MemorySegment seg, long index, MemorySegment x) {
        CXUnsavedFile.Filename$VH.set(seg.asSlice(index*sizeof()), x);
    }
    static final VarHandle Contents$VH = $struct$LAYOUT.varHandle(MemoryLayout.PathElement.groupElement("Contents"));
    public static VarHandle Contents$VH() {
        return CXUnsavedFile.Contents$VH;
    }
    public static MemorySegment Contents$get(MemorySegment seg) {
        return (java.lang.foreign.MemorySegment)CXUnsavedFile.Contents$VH.get(seg);
    }
    public static void Contents$set( MemorySegment seg, MemorySegment x) {
        CXUnsavedFile.Contents$VH.set(seg, x);
    }
    public static MemorySegment Contents$get(MemorySegment seg, long index) {
        return (java.lang.foreign.MemorySegment)CXUnsavedFile.Contents$VH.get(seg.asSlice(index*sizeof()));
    }
    public static void Contents$set(MemorySegment seg, long index, MemorySegment x) {
        CXUnsavedFile.Contents$VH.set(seg.asSlice(index*sizeof()), x);
    }
    static final VarHandle Length$VH = $struct$LAYOUT.varHandle(MemoryLayout.PathElement.groupElement("Length"));
    public static VarHandle Length$VH() {
        return CXUnsavedFile.Length$VH;
    }
    public static long Length$get(MemorySegment seg) {
        return (long)CXUnsavedFile.Length$VH.get(seg);
    }
    public static void Length$set( MemorySegment seg, long x) {
        CXUnsavedFile.Length$VH.set(seg, x);
    }
    public static long Length$get(MemorySegment seg, long index) {
        return (long)CXUnsavedFile.Length$VH.get(seg.asSlice(index*sizeof()));
    }
    public static void Length$set(MemorySegment seg, long index, long x) {
        CXUnsavedFile.Length$VH.set(seg.asSlice(index*sizeof()), x);
    }
    public static long sizeof() { return $LAYOUT().byteSize(); }
    public static MemorySegment allocate(SegmentAllocator allocator) { return allocator.allocate($LAYOUT()); }
    public static MemorySegment allocateArray(int len, SegmentAllocator allocator) {
        return allocator.allocate(MemoryLayout.sequenceLayout(len, $LAYOUT()));
    }
    public static MemorySegment ofAddress(MemorySegment addr, MemorySession session) { return RuntimeHelper.asArray(addr, $LAYOUT(), 1, session); }
}


