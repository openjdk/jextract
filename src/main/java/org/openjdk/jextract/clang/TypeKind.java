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

import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;
import static org.openjdk.jextract.clang.libclang.Index_h.*;

public enum TypeKind {

    Invalid(CXType_Invalid()),
    Unexposed(CXType_Unexposed()),
    Void(CXType_Void()),
    Bool(CXType_Bool()),
    Char_U(CXType_Char_U()),
    UChar(CXType_UChar()),
    Char16(CXType_Char16()),
    Char32(CXType_Char32()),
    UShort(CXType_UShort()),
    UInt(CXType_UInt()),
    ULong(CXType_ULong()),
    ULongLong(CXType_ULongLong()),
    UInt128(CXType_UInt128()),
    Char_S(CXType_Char_S()),
    SChar(CXType_SChar()),
    WChar(CXType_WChar()),
    Short(CXType_Short()),
    Int(CXType_Int()),
    Long(CXType_Long()),
    LongLong(CXType_LongLong()),
    Int128(CXType_Int128()),
    Float(CXType_Float()),
    Double(CXType_Double()),
    LongDouble(CXType_LongDouble()),
    NullPtr(CXType_NullPtr()),
    Overload(CXType_Overload()),
    Dependent(CXType_Dependent()),
    ObjCId(CXType_ObjCId()),
    ObjCClass(CXType_ObjCClass()),
    ObjCSel(CXType_ObjCSel()),
    Float128(CXType_Float128()),
    Half(CXType_Half()),
    Float16(CXType_Float16()),
    ShortAccum(CXType_ShortAccum()),
    Accum(CXType_Accum()),
    LongAccum(CXType_LongAccum()),
    UShortAccum(CXType_UShortAccum()),
    UAccum(CXType_UAccum()),
    ULongAccum(CXType_ULongAccum()),
    Complex(CXType_Complex()),
    Pointer(CXType_Pointer()),
    BlockPointer(CXType_BlockPointer()),
    LValueReference(CXType_LValueReference()),
    RValueReference(CXType_RValueReference()),
    Record(CXType_Record()),
    Enum(CXType_Enum()),
    Typedef(CXType_Typedef()),
    ObjCInterface(CXType_ObjCInterface()),
    ObjCObjectPointer(CXType_ObjCObjectPointer()),
    FunctionNoProto(CXType_FunctionNoProto()),
    FunctionProto(CXType_FunctionProto()),
    ConstantArray(CXType_ConstantArray()),
    Vector(CXType_Vector()),
    IncompleteArray(CXType_IncompleteArray()),
    VariableArray(CXType_VariableArray()),
    DependentSizedArray(CXType_DependentSizedArray()),
    MemberPointer(CXType_MemberPointer()),
    Auto(CXType_Auto()),
    Elaborated(CXType_Elaborated()),
    Pipe(CXType_Pipe()),
    OCLImage1dRO(CXType_OCLImage1dRO()),
    OCLImage1dArrayRO(CXType_OCLImage1dArrayRO()),
    OCLImage1dBufferRO(CXType_OCLImage1dBufferRO()),
    OCLImage2dRO(CXType_OCLImage2dRO()),
    OCLImage2dArrayRO(CXType_OCLImage2dArrayRO()),
    OCLImage2dDepthRO(CXType_OCLImage2dDepthRO()),
    OCLImage2dArrayDepthRO(CXType_OCLImage2dArrayDepthRO()),
    OCLImage2dMSAARO(CXType_OCLImage2dMSAARO()),
    OCLImage2dArrayMSAARO(CXType_OCLImage2dArrayMSAARO()),
    OCLImage2dMSAADepthRO(CXType_OCLImage2dMSAADepthRO()),
    OCLImage2dArrayMSAADepthRO(CXType_OCLImage2dArrayMSAADepthRO()),
    OCLImage3dRO(CXType_OCLImage3dRO()),
    OCLImage1dWO(CXType_OCLImage1dWO()),
    OCLImage1dArrayWO(CXType_OCLImage1dArrayWO()),
    OCLImage1dBufferWO(CXType_OCLImage1dBufferWO()),
    OCLImage2dWO(CXType_OCLImage2dWO()),
    OCLImage2dArrayWO(CXType_OCLImage2dArrayWO()),
    OCLImage2dDepthWO(CXType_OCLImage2dDepthWO()),
    OCLImage2dArrayDepthWO(CXType_OCLImage2dArrayDepthWO()),
    OCLImage2dMSAAWO(CXType_OCLImage2dMSAAWO()),
    OCLImage2dArrayMSAAWO(CXType_OCLImage2dArrayMSAAWO()),
    OCLImage2dMSAADepthWO(CXType_OCLImage2dMSAADepthWO()),
    OCLImage2dArrayMSAADepthWO(CXType_OCLImage2dArrayMSAADepthWO()),
    OCLImage3dWO(CXType_OCLImage3dWO()),
    OCLImage1dRW(CXType_OCLImage1dRW()),
    OCLImage1dArrayRW(CXType_OCLImage1dArrayRW()),
    OCLImage1dBufferRW(CXType_OCLImage1dBufferRW()),
    OCLImage2dRW(CXType_OCLImage2dRW()),
    OCLImage2dArrayRW(CXType_OCLImage2dArrayRW()),
    OCLImage2dDepthRW(CXType_OCLImage2dDepthRW()),
    OCLImage2dArrayDepthRW(CXType_OCLImage2dArrayDepthRW()),
    OCLImage2dMSAARW(CXType_OCLImage2dMSAARW()),
    OCLImage2dArrayMSAARW(CXType_OCLImage2dArrayMSAARW()),
    OCLImage2dMSAADepthRW(CXType_OCLImage2dMSAADepthRW()),
    OCLImage2dArrayMSAADepthRW(CXType_OCLImage2dArrayMSAADepthRW()),
    OCLImage3dRW(CXType_OCLImage3dRW()),
    OCLSampler(CXType_OCLSampler()),
    OCLEvent(CXType_OCLEvent()),
    OCLQueue(CXType_OCLQueue()),
    OCLReserveID(CXType_OCLReserveID()),
    ObjCObject(CXType_ObjCObject()),
    ObjCTypeParam(CXType_ObjCTypeParam()),
    Attributed(CXType_Attributed()),
    OCLIntelSubgroupAVCMcePayload(CXType_OCLIntelSubgroupAVCMcePayload()),
    OCLIntelSubgroupAVCImePayload(CXType_OCLIntelSubgroupAVCImePayload()),
    OCLIntelSubgroupAVCRefPayload(CXType_OCLIntelSubgroupAVCRefPayload()),
    OCLIntelSubgroupAVCSicPayload(CXType_OCLIntelSubgroupAVCSicPayload()),
    OCLIntelSubgroupAVCMceResult(CXType_OCLIntelSubgroupAVCMceResult()),
    OCLIntelSubgroupAVCImeResult(CXType_OCLIntelSubgroupAVCImeResult()),
    OCLIntelSubgroupAVCRefResult(CXType_OCLIntelSubgroupAVCRefResult()),
    OCLIntelSubgroupAVCSicResult(CXType_OCLIntelSubgroupAVCSicResult()),
    OCLIntelSubgroupAVCImeResultSingleRefStreamout(CXType_OCLIntelSubgroupAVCImeResultSingleRefStreamout()),
    OCLIntelSubgroupAVCImeResultDualRefStreamout(CXType_OCLIntelSubgroupAVCImeResultDualRefStreamout()),
    OCLIntelSubgroupAVCImeSingleRefStreamin(CXType_OCLIntelSubgroupAVCImeSingleRefStreamin()),
    OCLIntelSubgroupAVCImeDualRefStreamin(CXType_OCLIntelSubgroupAVCImeDualRefStreamin()),
    ExtVector(CXType_ExtVector()),
    Atomic(177);  // This is missing in auto-generated code

    private final int value;

    TypeKind(int value) {
        this.value = value;
    }

    public int value() {
        return value;
    }

    private final static Map<Integer, TypeKind> lookup;

    static {
        lookup = new HashMap<>();
        for (TypeKind e: TypeKind.values()) {
            lookup.put(e.value(), e);
        }
    }

    public final static TypeKind valueOf(int value) {
        TypeKind x = lookup.get(value);
        if (null == x) {
            throw new NoSuchElementException("kind = " + value);
        }
        return x;
    }
}
