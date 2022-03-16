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

import jdk.incubator.foreign.MemorySegment;
import jdk.incubator.foreign.ResourceScope;
import jdk.incubator.foreign.SegmentAllocator;
import org.openjdk.jextract.clang.libclang.CXType;
import org.openjdk.jextract.clang.libclang.Index_h;

import static org.openjdk.jextract.clang.LibClang.IMPLICIT_ALLOCATOR;

public final class Type {
    private final MemorySegment type;
    Type(MemorySegment type) {
        this.type = type;
    }

    public boolean isInvalid() {
        return kind() == TypeKind.Invalid;
    }

    // Function Types
    public boolean isVariadic() {
        return Index_h.clang_isFunctionTypeVariadic(type) != 0;
    }
    public Type resultType() {
        return new Type(Index_h.clang_getResultType(IMPLICIT_ALLOCATOR, type));
    }
    public int numberOfArgs() {
        return Index_h.clang_getNumArgTypes(type);
    }
    public Type argType(int idx) {
        return new Type(Index_h.clang_getArgType(IMPLICIT_ALLOCATOR, type, idx));
    }
    private int getCallingConvention0() {
        return Index_h.clang_getFunctionTypeCallingConv(type);
    }

    public CallingConvention getCallingConvention() {
        int v = getCallingConvention0();
        return CallingConvention.valueOf(v);
    }

    public boolean isPointer() {
        var kind = kind();
        return kind == TypeKind.Pointer ||
            kind == TypeKind.BlockPointer || kind == TypeKind.MemberPointer;
    }

    public boolean isReference() {
        var kind = kind();
        return kind == TypeKind.LValueReference || kind == TypeKind.RValueReference;
    }

    public boolean isArray() {
        var kind = kind();
        return kind == TypeKind.ConstantArray ||
           kind == TypeKind.IncompleteArray ||
           kind == TypeKind.VariableArray ||
           kind == TypeKind.DependentSizedArray;
    }

    // Pointer type
    public Type getPointeeType() {
        return new Type(Index_h.clang_getPointeeType(IMPLICIT_ALLOCATOR, type));
    }

    // array/vector type
    public Type getElementType() {
        return new Type(Index_h.clang_getElementType(IMPLICIT_ALLOCATOR, type));
    }

    public long getNumberOfElements() {
        return Index_h.clang_getNumElements(type);
    }

    // Struct/RecordType
    private long getOffsetOf0(String fieldName) {
        try (ResourceScope scope = ResourceScope.newConfinedScope()) {
            var allocator = SegmentAllocator.nativeAllocator(scope);
            MemorySegment cfname = allocator.allocateUtf8String(fieldName);
            return Index_h.clang_Type_getOffsetOf(type, cfname);
        }
    }

    public long getOffsetOf(String fieldName) {
        long res = getOffsetOf0(fieldName);
        if(TypeLayoutError.isError(res)) {
            throw new TypeLayoutError(res, String.format("type: %s, fieldName: %s", this, fieldName));
        }
        return res;
    }

    // Typedef
    /**
     * Return the canonical type for a Type.
     *
     * Clang's type system explicitly models typedefs and all the ways
     * a specific type can be represented.  The canonical type is the underlying
     * type with all the "sugar" removed.  For example, if 'T' is a typedef
     * for 'int', the canonical type for 'T' would be 'int'.
     */
    public Type canonicalType() {
        return new Type(Index_h.clang_getCanonicalType(IMPLICIT_ALLOCATOR, type));
    }

    /**
     * Determine whether a Type has the "const" qualifier set,
     * without looking through typedefs that may have added "const" at a
     * different level.
     */
    public boolean isConstQualifierdType() {
        return Index_h.clang_isConstQualifiedType(type) != 0;
    }

    /**
     * Determine whether a Type has the "volatile" qualifier set,
     * without looking through typedefs that may have added "volatile" at
     * a different level.
     */
    public boolean isVolatileQualified() {
        return Index_h.clang_isVolatileQualifiedType(type) != 0;
    }

    public String spelling() {
        return LibClang.CXStrToString(allocator ->
                Index_h.clang_getTypeSpelling(allocator, type));
    }

    public int kind0() {
        return CXType.kind$get(type);
    }

    private long size0() {
        return Index_h.clang_Type_getSizeOf(type);
    }

    public long size() {
        long res = size0();
        if(TypeLayoutError.isError(res)) {
            throw new TypeLayoutError(res, String.format("type: %s", this));
        }
        return res;
    }

    public TypeKind kind() {
        int v = kind0();
        TypeKind rv = TypeKind.valueOf(v);
        // TODO: Atomic type doesn't work
        return rv;
    }

    public Cursor getDeclarationCursor() {
        return new Cursor(Index_h.clang_getTypeDeclaration(IMPLICIT_ALLOCATOR, type));
    }

    public boolean equalType(Type other) {
        return Index_h.clang_equalTypes(type, other.type) != 0;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof Type)) {
            return false;
        }
        return equalType((Type) other);
    }

    @Override
    public int hashCode() {
        return spelling().hashCode();
    }
}
