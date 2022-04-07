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

public enum CursorKind {

    UnexposedDecl(CXCursor_UnexposedDecl()),
    StructDecl(CXCursor_StructDecl()),
    UnionDecl(CXCursor_UnionDecl()),
    ClassDecl(CXCursor_ClassDecl()),
    EnumDecl(CXCursor_EnumDecl()),
    FieldDecl(CXCursor_FieldDecl()),
    EnumConstantDecl(CXCursor_EnumConstantDecl()),
    FunctionDecl(CXCursor_FunctionDecl()),
    VarDecl(CXCursor_VarDecl()),
    ParmDecl(CXCursor_ParmDecl()),
    TypedefDecl(CXCursor_TypedefDecl()),
    Namespace(CXCursor_Namespace()),
    TypeRef(CXCursor_TypeRef()),
    IntegerLiteral(CXCursor_IntegerLiteral()),
    FloatingLiteral(CXCursor_FloatingLiteral()),
    ImaginaryLiteral(CXCursor_ImaginaryLiteral()),
    StringLiteral(CXCursor_StringLiteral()),
    CharacterLiteral(CXCursor_CharacterLiteral()),
    UnexposedAttr(CXCursor_UnexposedAttr()),
    IBActionAttr(CXCursor_IBActionAttr()),
    IBOutletAttr(CXCursor_IBOutletAttr()),
    IBOutletCollectionAttr(CXCursor_IBOutletCollectionAttr()),
    CXXFinalAttr(CXCursor_CXXFinalAttr()),
    CXXOverrideAttr(CXCursor_CXXOverrideAttr()),
    AnnotateAttr(CXCursor_AnnotateAttr()),
    AsmLabelAttr(CXCursor_AsmLabelAttr()),
    PackedAttr(CXCursor_PackedAttr()),
    PureAttr(CXCursor_PureAttr()),
    ConstAttr(CXCursor_ConstAttr()),
    NoDuplicateAttr(CXCursor_NoDuplicateAttr()),
    CUDAConstantAttr(CXCursor_CUDAConstantAttr()),
    CUDADeviceAttr(CXCursor_CUDADeviceAttr()),
    CUDAGlobalAttr(CXCursor_CUDAGlobalAttr()),
    CUDAHostAttr(CXCursor_CUDAHostAttr()),
    CUDASharedAttr(CXCursor_CUDASharedAttr()),
    VisibilityAttr(CXCursor_VisibilityAttr()),
    DLLExport(CXCursor_DLLExport()),
    DLLImport(CXCursor_DLLImport()),
    NSReturnsRetained(CXCursor_NSReturnsRetained()),
    NSReturnsNotRetained(CXCursor_NSReturnsNotRetained()),
    NSReturnsAutoreleased(CXCursor_NSReturnsAutoreleased()),
    NSConsumesSelf(CXCursor_NSConsumesSelf()),
    NSConsumed(CXCursor_NSConsumed()),
    ObjCException(CXCursor_ObjCException()),
    ObjCNSObject(CXCursor_ObjCNSObject()),
    ObjCIndependentClass(CXCursor_ObjCIndependentClass()),
    ObjCPreciseLifetime(CXCursor_ObjCPreciseLifetime()),
    ObjCReturnsInnerPointer(CXCursor_ObjCReturnsInnerPointer()),
    ObjCRequiresSuper(CXCursor_ObjCRequiresSuper()),
    ObjCRootClass(CXCursor_ObjCRootClass()),
    ObjCSubclassingRestricted(CXCursor_ObjCSubclassingRestricted()),
    ObjCExplicitProtocolImpl(CXCursor_ObjCExplicitProtocolImpl()),
    ObjCDesignatedInitializer(CXCursor_ObjCDesignatedInitializer()),
    ObjCRuntimeVisible(CXCursor_ObjCRuntimeVisible()),
    ObjCBoxable(CXCursor_ObjCBoxable()),
    FlagEnum(CXCursor_FlagEnum()),
    ConvergentAttr(CXCursor_ConvergentAttr()),
    WarnUnusedAttr(CXCursor_WarnUnusedAttr()),
    WarnUnusedResultAttr(CXCursor_WarnUnusedResultAttr()),
    AlignedAttr(CXCursor_AlignedAttr()),
    MacroDefinition(CXCursor_MacroDefinition()),
    MacroExpansion(CXCursor_MacroExpansion()),
    MacroInstantiation(CXCursor_MacroInstantiation()),
    InclusionDirective(CXCursor_InclusionDirective()),
    /*
     * Per libclang API docs, clang returns this CursorKind
     * for both C11 _Static_assert and C++11 static_assert
     */
    StaticAssert(CXCursor_StaticAssert());


    private final int value;

    CursorKind(int value) {
        this.value = value;
    }

    public int value() {
        return value;
    }

    private final static Map<Integer, CursorKind> lookup;

    static {
        lookup = new HashMap<>();
        for (CursorKind e: CursorKind.values()) {
            lookup.put(e.value(), e);
        }
    }

    public final static CursorKind valueOf(int value) {
        CursorKind x = lookup.get(value);
        if (null == x) {
            throw new NoSuchElementException("Invalid Cursor kind value: " + value);
        }
        return x;
    }
}
