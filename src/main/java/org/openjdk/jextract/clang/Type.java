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
import java.lang.foreign.MemorySession;

import org.openjdk.jextract.clang.libclang.CXType;
import org.openjdk.jextract.clang.libclang.Index_h;

import static org.openjdk.jextract.clang.LibClang.STRING_ALLOCATOR;

public final class Type extends ClangDisposable.Owned {

    Type(MemorySegment segment, ClangDisposable owner) {
        super(segment, owner);
    }

    public boolean isInvalid() {
        return kind() == TypeKind.Invalid;
    }

    // Function Types
    public boolean isVariadic() {
        return Index_h.clang_isFunctionTypeVariadic(segment) != 0;
    }
    public Type resultType() {
        var resultType = Index_h.clang_getResultType(owner, segment);
        return new Type(resultType, owner);
    }
    public int numberOfArgs() {
        return Index_h.clang_getNumArgTypes(segment);
    }
    public Type argType(int idx) {
        var argType = Index_h.clang_getArgType(owner, segment, idx);
        return new Type(argType, owner);
    }
    private int getCallingConvention0() {
        return Index_h.clang_getFunctionTypeCallingConv(segment);
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

    // Pointer segment
    public Type getPointeeType() {
        var pointee = Index_h.clang_getPointeeType(owner, segment);
        return new Type(pointee, owner);
    }

    // array/vector segment
    public Type getElementType() {
        var elementType = Index_h.clang_getElementType(owner, segment);
        return new Type(elementType, owner);
    }

    public long getNumberOfElements() {
        return Index_h.clang_getNumElements(segment);
    }

    // Struct/RecordType
    private long getOffsetOf0(String fieldName) {
        try (Arena arena = Arena.openConfined()) {
            MemorySegment cfname = arena.allocateUtf8String(fieldName);
            return Index_h.clang_Type_getOffsetOf(segment, cfname);
        }
    }

    public long getOffsetOf(String fieldName) {
        long res = getOffsetOf0(fieldName);
        if(TypeLayoutError.isError(res)) {
            throw new TypeLayoutError(res, String.format("segment: %s, fieldName: %s", this, fieldName));
        }
        return res;
    }

    // Typedef
    /**
     * Return the canonical segment for a Type.
     *
     * Clang's segment system explicitly models typedefs and all the ways
     * a specific segment can be represented.  The canonical segment is the underlying
     * segment with all the "sugar" removed.  For example, if 'T' is a typedef
     * for 'int', the canonical segment for 'T' would be 'int'.
     */
    public Type canonicalType() {
        var canonicalType = Index_h.clang_getCanonicalType(owner, segment);
        return new Type(canonicalType, owner);
    }

    /**
     * Determine whether a Type has the "const" qualifier set,
     * without looking through typedefs that may have added "const" at a
     * different level.
     */
    public boolean isConstQualifierdType() {
        return Index_h.clang_isConstQualifiedType(segment) != 0;
    }

    /**
     * Determine whether a Type has the "volatile" qualifier set,
     * without looking through typedefs that may have added "volatile" at
     * a different level.
     */
    public boolean isVolatileQualified() {
        return Index_h.clang_isVolatileQualifiedType(segment) != 0;
    }

    public String spelling() {
        var spelling = Index_h.clang_getTypeSpelling(STRING_ALLOCATOR, segment);
        return LibClang.CXStrToString(spelling);
    }

    public int kind0() {
        return CXType.kind$get(segment);
    }

    private long size0() {
        return Index_h.clang_Type_getSizeOf(segment);
    }

    public long size() {
        long res = size0();
        if(TypeLayoutError.isError(res)) {
            throw new TypeLayoutError(res, String.format("segment: %s", this));
        }
        return res;
    }

    public TypeKind kind() {
        int v = kind0();
        TypeKind rv = TypeKind.valueOf(v);
        // TODO: Atomic segment doesn't work
        return rv;
    }

    public Cursor getDeclarationCursor() {
        var cursorDecl = Index_h.clang_getTypeDeclaration(owner, segment);
        return new Cursor(cursorDecl, owner);
    }

    public boolean equalType(Type other) {
        return Index_h.clang_equalTypes(segment, other.segment) != 0;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        return other instanceof Type segment && equalType(segment);
    }

    @Override
    public int hashCode() {
        return spelling().hashCode();
    }
}
