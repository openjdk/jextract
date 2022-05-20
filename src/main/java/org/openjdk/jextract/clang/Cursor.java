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

import java.lang.foreign.MemoryAddress;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.MemorySession;
import java.lang.foreign.SegmentAllocator;
import org.openjdk.jextract.clang.libclang.CXCursorVisitor;
import org.openjdk.jextract.clang.libclang.Index_h;

import java.util.ArrayList;
import java.util.stream.Stream;

import static org.openjdk.jextract.clang.LibClang.IMPLICIT_ALLOCATOR;

public final class Cursor {

    private final MemorySegment cursor;
    private final int kind;

    Cursor(MemorySegment cursor) {
        this.cursor = cursor;
        kind = Index_h.clang_getCursorKind(cursor);
    }

    public boolean isDeclaration() {
        return Index_h.clang_isDeclaration(kind) != 0;
    }

    public boolean isPreprocessing() {
        return Index_h.clang_isPreprocessing(kind) != 0;
    }

    public boolean isInvalid() {
        return Index_h.clang_isInvalid(kind) != 0;
    }

    public boolean isDefinition() {
        return Index_h.clang_isCursorDefinition(cursor) != 0;
    }

    public boolean isAttribute() { return Index_h.clang_isAttribute(kind) != 0; }

    public boolean isAnonymousStruct() {
        return Index_h.clang_Cursor_isAnonymousRecordDecl(cursor) != 0;
    }

    public boolean isMacroFunctionLike() {
        return Index_h.clang_Cursor_isMacroFunctionLike(cursor) != 0;
    }

    public String spelling() {
        return LibClang.CXStrToString(allocator ->
            Index_h.clang_getCursorSpelling(allocator, cursor));
    }

    public String USR() {
        return LibClang.CXStrToString(allocator ->
                Index_h.clang_getCursorUSR(allocator, cursor));
    }

    public String prettyPrinted(PrintingPolicy policy) {
        return LibClang.CXStrToString(allocator ->
            Index_h.clang_getCursorPrettyPrinted(allocator, cursor, policy.ptr()));
    }

    public String prettyPrinted() {
        try (PrintingPolicy policy = getPrintingPolicy()) {
            return prettyPrinted(policy);
        }
    }

    public String displayName() {
        return LibClang.CXStrToString(allocator ->
                Index_h.clang_getCursorDisplayName(allocator, cursor));
    }

    public boolean equalCursor(Cursor other) {
        return Index_h.clang_equalCursors(cursor, other.cursor) != 0;
    }

    public Type type() {
        return new Type(Index_h.clang_getCursorType(IMPLICIT_ALLOCATOR, cursor));
    }

    public Type getEnumDeclIntegerType() {
        return new Type(Index_h.clang_getEnumDeclIntegerType(IMPLICIT_ALLOCATOR, cursor));
    }

    public Cursor getDefinition() {
        return new Cursor(Index_h.clang_getCursorDefinition(IMPLICIT_ALLOCATOR, cursor));
    }

    public SourceLocation getSourceLocation() {
        MemorySegment loc = Index_h.clang_getCursorLocation(IMPLICIT_ALLOCATOR, cursor);
        try (MemorySession session = MemorySession.openConfined()) {
            if (Index_h.clang_equalLocations(loc, Index_h.clang_getNullLocation(session)) != 0) {
                return null;
            }
        }
        return new SourceLocation(loc);
    }

    public SourceRange getExtent() {
        MemorySegment range = Index_h.clang_getCursorExtent(IMPLICIT_ALLOCATOR, cursor);
        if (Index_h.clang_Range_isNull(range) != 0) {
            return null;
        }
        return new SourceRange(range);
    }

    public int numberOfArgs() {
        return Index_h.clang_Cursor_getNumArguments(cursor);
    }

    public Cursor getArgument(int idx) {
        return new Cursor(Index_h.clang_Cursor_getArgument(IMPLICIT_ALLOCATOR, cursor, idx));
    }

    // C long long, 64-bit
    public long getEnumConstantValue() {
        return Index_h.clang_getEnumConstantDeclValue(cursor);
    }

    // C unsigned long long, 64-bit
    public long getEnumConstantUnsignedValue() {
        return Index_h.clang_getEnumConstantDeclUnsignedValue(cursor);
    }

    public boolean isBitField() {
        return Index_h.clang_Cursor_isBitField(cursor) != 0;
    }

    public int getBitFieldWidth() {
        return Index_h.clang_getFieldDeclBitWidth(cursor);
    }

    public CursorKind kind() {
        return CursorKind.valueOf(kind);
    }

    public CursorLanguage language() {
        return CursorLanguage.valueOf(Index_h.clang_getCursorLanguage(cursor));
    }

    public int kind0() {
        return kind;
    }

    /**
     * For a cursor that is a reference, retrieve a cursor representing the entity that it references.
     */
    public Cursor getCursorReferenced() {
        return new Cursor(Index_h.clang_getCursorReferenced(
                IMPLICIT_ALLOCATOR, cursor));
    }

    private static class CursorChildren {
        private static final ArrayList<Cursor> children = new ArrayList<>();
        private static final MemorySegment callback = CXCursorVisitor.allocate((c, p, d) -> {
            MemorySegment copy = MemorySegment.allocateNative(c.byteSize(), MemorySession.openImplicit());
            copy.copyFrom(c);
            Cursor cursor = new Cursor(copy);
            children.add(cursor);
            return Index_h.CXChildVisit_Continue();
        }, MemorySession.openImplicit());

        synchronized static Stream<Cursor> get(Cursor c) {
            try {
                Index_h.clang_visitChildren(c.cursor, callback, MemoryAddress.NULL);
                return new ArrayList<>(children).stream();
            } finally {
                children.clear();
            }
        }
    }

    public Stream<Cursor> children() {
        return CursorChildren.get(this);
    }

    public Stream<Cursor> allChildren() {
        return children().flatMap(c -> Stream.concat(Stream.of(c), c.children()));
    }

    public String getMangling() {
        return LibClang.CXStrToString(allocator ->
                Index_h.clang_Cursor_getMangling(allocator, cursor));
    }

    public TranslationUnit getTranslationUnit() {
        return new TranslationUnit(Index_h.clang_Cursor_getTranslationUnit(cursor));
    }

    private MemoryAddress eval0() {
        return Index_h.clang_Cursor_Evaluate(cursor);
    }

    public EvalResult eval() {
        MemoryAddress ptr = eval0();
        return ptr == MemoryAddress.NULL ? EvalResult.erroneous : new EvalResult(ptr);
    }

    public PrintingPolicy getPrintingPolicy() {
        return new PrintingPolicy(Index_h.clang_getCursorPrintingPolicy(cursor));
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        return other instanceof Cursor otherCursor &&
                (Index_h.clang_equalCursors(cursor, otherCursor.cursor) != 0);
    }

    @Override
    public int hashCode() {
        return spelling().hashCode();
    }
}
