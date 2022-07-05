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
import org.openjdk.jextract.clang.libclang.CXCursorVisitor;
import org.openjdk.jextract.clang.libclang.Index_h;

import java.lang.foreign.NativeArena;
import java.util.function.Consumer;

public final class Cursor extends ClangDisposable.Owned {

    private final int kind;

    Cursor(MemorySegment segment, ClangDisposable owner) {
        super(segment, owner);
        kind = Index_h.clang_getCursorKind(segment);
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
        return Index_h.clang_isCursorDefinition(segment) != 0;
    }

    public boolean isAttribute() { return Index_h.clang_isAttribute(kind) != 0; }

    public boolean isAnonymousStruct() {
        return Index_h.clang_Cursor_isAnonymousRecordDecl(segment) != 0;
    }

    public boolean isMacroFunctionLike() {
        return Index_h.clang_Cursor_isMacroFunctionLike(segment) != 0;
    }

    public String spelling() {
        var spelling = Index_h.clang_getCursorSpelling(LibClang.STRING_ALLOCATOR, segment);
        return LibClang.CXStrToString(spelling);
    }

    public String USR() {
        var USR = Index_h.clang_getCursorUSR(LibClang.STRING_ALLOCATOR, segment);
        return LibClang.CXStrToString(USR);
    }

    public String prettyPrinted(PrintingPolicy policy) {
        var prettyOutput = Index_h.clang_getCursorPrettyPrinted(LibClang.STRING_ALLOCATOR, segment, policy.ptr());
        return LibClang.CXStrToString(prettyOutput);
    }

    public String prettyPrinted() {
        try (PrintingPolicy policy = getPrintingPolicy()) {
            return prettyPrinted(policy);
        }
    }

    public String displayName() {
        var displayName = Index_h.clang_getCursorDisplayName(LibClang.STRING_ALLOCATOR, segment);
        return LibClang.CXStrToString(displayName);
    }

    public boolean equalCursor(Cursor other) {
        return Index_h.clang_equalCursors(segment, other.segment) != 0;
    }

    public Type type() {
        var cursorType = Index_h.clang_getCursorType(owner, segment);
        return new Type(cursorType, owner);
    }

    public Type getEnumDeclIntegerType() {
        var enumType = Index_h.clang_getEnumDeclIntegerType(owner, segment);
        return new Type(enumType, owner);
    }

    public Cursor getDefinition() {
        var cursorDef = Index_h.clang_getCursorDefinition(owner, segment);
        return new Cursor(cursorDef, owner);
    }

    public SourceLocation getSourceLocation() {
        MemorySegment loc = Index_h.clang_getCursorLocation(owner, segment);
        try (NativeArena arena = NativeArena.openConfined()) {
            if (Index_h.clang_equalLocations(loc, Index_h.clang_getNullLocation(arena)) != 0) {
                return null;
            }
        }
        return new SourceLocation(loc, owner);
    }

    public SourceRange getExtent() {
        MemorySegment range = Index_h.clang_getCursorExtent(owner, segment);
        if (Index_h.clang_Range_isNull(range) != 0) {
            return null;
        }
        return new SourceRange(range, owner);
    }

    public int numberOfArgs() {
        return Index_h.clang_Cursor_getNumArguments(segment);
    }

    public Cursor getArgument(int idx) {
        var cursorArg = Index_h.clang_Cursor_getArgument(owner, segment, idx);
        return new Cursor(cursorArg, owner);
    }

    // C long long, 64-bit
    public long getEnumConstantValue() {
        return Index_h.clang_getEnumConstantDeclValue(segment);
    }

    // C unsigned long long, 64-bit
    public long getEnumConstantUnsignedValue() {
        return Index_h.clang_getEnumConstantDeclUnsignedValue(segment);
    }

    public boolean isBitField() {
        return Index_h.clang_Cursor_isBitField(segment) != 0;
    }

    public int getBitFieldWidth() {
        return Index_h.clang_getFieldDeclBitWidth(segment);
    }

    public CursorKind kind() {
        return CursorKind.valueOf(kind);
    }

    public CursorLanguage language() {
        return CursorLanguage.valueOf(Index_h.clang_getCursorLanguage(segment));
    }

    public int kind0() {
        return kind;
    }

    /**
     * For a segment that is a reference, retrieve a segment representing the entity that it references.
     */
    public Cursor getCursorReferenced() {
        var referenced = Index_h.clang_getCursorReferenced(owner, segment);
        return new Cursor(referenced, owner);
    }

    public void forEach(Consumer<Cursor> action) {
        CursorChildren.forEach(this, action);
    }

    /**
     * We run the visitor action inside the upcall, so that we do not have to worry about
     * having to copy cursors into separate off-heap storage. To do this, we have to setup
     * some context for the upcall, so that the upcall code can call the "correct" user-defined visitor action.
     * Note: exceptions must be delayed until after the upcall has returned; this is necessary as upcalls
     * cannot throw (if they do, they cause a JVM crash).
     */
    private static class CursorChildren {

        static class Context {
            private final Consumer<Cursor> action;
            private final ClangDisposable owner;
            private RuntimeException exception;

            Context(Consumer<Cursor> action, ClangDisposable owner) {
                this.action = action;
                this.owner = owner;
            }

            boolean visit(MemorySegment segment) {
                // Note: the session of this cursor is smaller than that of the translation unit
                // this is because the cursor will be destroyed when the upcall ends. This means
                // that the cursor passed by the visitor must NOT be leaked into a field and accessed
                // at a later time (or the liveness check will fail with IllegalStateException).
                try {
                    // run the visitor action
                    action.accept(new Cursor(segment, owner));
                    return true;
                } catch (RuntimeException ex) {
                    // if we fail, record the exception, and return false to stop the visit
                    exception = ex;
                    return false;
                }
            }

            void handleExceptions() {
                if (exception != null) {
                    throw exception;
                }
            }
        }

        static Context pendingContext = null;

        private static final MemorySegment callback = CXCursorVisitor.allocate((c, p, d) -> {
            if (pendingContext.visit(c)) {
                return Index_h.CXChildVisit_Continue();
            } else {
                return Index_h.CXChildVisit_Break();
            }
        }, Arena.global());

        synchronized static void forEach(Cursor c, Consumer<Cursor> op) {
            // everything is confined, no need to synchronize
            Context prevContext = pendingContext;
            try {
                pendingContext = new Context(op, c.owner);
                Index_h.clang_visitChildren(c.segment, callback, MemorySegment.NULL);
                pendingContext.handleExceptions();
            } finally {
                pendingContext = prevContext;
            }
        }
    }

    public TranslationUnit getTranslationUnit() {
        return new TranslationUnit(Index_h.clang_Cursor_getTranslationUnit(segment));
    }

    private MemorySegment eval0() {
        return Index_h.clang_Cursor_Evaluate(segment);
    }

    public EvalResult eval() {
        MemorySegment ptr = eval0();
        return ptr == MemorySegment.NULL ? EvalResult.erroneous : new EvalResult(ptr);
    }

    public PrintingPolicy getPrintingPolicy() {
        return new PrintingPolicy(Index_h.clang_getCursorPrintingPolicy(segment));
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        return other instanceof Cursor otherCursor &&
                (Index_h.clang_equalCursors(segment, otherCursor.segment) != 0);
    }

    @Override
    public int hashCode() {
        return spelling().hashCode();
    }
}
