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

import java.lang.foreign.MemorySegment;
import java.lang.foreign.MemoryLayout;
import java.lang.foreign.NativeArena;
import java.lang.foreign.SegmentAllocator;
import org.openjdk.jextract.clang.libclang.CXToken;
import org.openjdk.jextract.clang.libclang.Index_h;
import org.openjdk.jextract.clang.libclang.CXUnsavedFile;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Objects;
import java.util.function.Consumer;

import static org.openjdk.jextract.clang.LibClang.STRING_ALLOCATOR;
import static org.openjdk.jextract.clang.libclang.Index_h.C_INT;
import static org.openjdk.jextract.clang.libclang.Index_h.C_POINTER;

public class TranslationUnit extends ClangDisposable {
    private static final int MAX_RETRIES = 10;

    TranslationUnit(MemorySegment addr) {
        super(addr, () -> Index_h.clang_disposeTranslationUnit(addr));
    }

    public Cursor getCursor() {
        var cursor = Index_h.clang_getTranslationUnitCursor(arena, ptr);
        return new Cursor(cursor, this);
    }

    public final void save(Path path) throws TranslationUnitSaveException {
        try (NativeArena session = NativeArena.openConfined()) {
            var allocator = session;
            MemorySegment pathStr = allocator.allocateUtf8String(path.toAbsolutePath().toString());
            SaveError res = SaveError.valueOf(Index_h.clang_saveTranslationUnit(ptr, pathStr, 0));
            if (res != SaveError.None) {
                throw new TranslationUnitSaveException(path, res);
            }
        }
    }

    void processDiagnostics(Consumer<Diagnostic> dh) {
        Objects.requireNonNull(dh);
        int cntDiags = Index_h.clang_getNumDiagnostics(ptr);
        for (int i = 0; i < cntDiags; i++) {
            MemorySegment diag = Index_h.clang_getDiagnostic(ptr, i);
            dh.accept(new Diagnostic(diag));
        }
    }

    static long FILENAME_OFFSET = CXUnsavedFile.$LAYOUT().byteOffset(MemoryLayout.PathElement.groupElement("Filename"));
    static long CONTENTS_OFFSET = CXUnsavedFile.$LAYOUT().byteOffset(MemoryLayout.PathElement.groupElement("Contents"));
    static long LENGTH_OFFSET = CXUnsavedFile.$LAYOUT().byteOffset(MemoryLayout.PathElement.groupElement("Length"));

    public void reparse(Index.UnsavedFile... inMemoryFiles) {
        try (NativeArena session = NativeArena.openConfined()) {
            var allocator = SegmentAllocator.bumpAllocator(session);
            MemorySegment files = inMemoryFiles.length == 0 ?
                    null :
                    allocator.allocateArray(CXUnsavedFile.$LAYOUT(), inMemoryFiles.length);
            for (int i = 0; i < inMemoryFiles.length; i++) {
                MemorySegment start = files.asSlice(i * CXUnsavedFile.$LAYOUT().byteSize());
                start.set(C_POINTER, FILENAME_OFFSET, allocator.allocateUtf8String(inMemoryFiles[i].file));
                start.set(C_POINTER, CONTENTS_OFFSET, allocator.allocateUtf8String(inMemoryFiles[i].contents));
                start.set(C_INT, LENGTH_OFFSET, inMemoryFiles[i].contents.length());
            }
            ErrorCode code;
            int tries = 0;
            do {
                code = ErrorCode.valueOf(Index_h.clang_reparseTranslationUnit(
                        ptr,
                        inMemoryFiles.length,
                        files == null ? MemorySegment.NULL : files,
                        Index_h.clang_defaultReparseOptions(ptr)));
            } while(code == ErrorCode.Crashed && (++tries) < MAX_RETRIES); // this call can crash on Windows. Retry in that case.

            if (code != ErrorCode.Success) {
                throw new IllegalStateException("Re-parsing failed: " + code);
            }
        }
    }

    public void reparse(Consumer<Diagnostic> dh, Index.UnsavedFile... inMemoryFiles) {
        reparse(inMemoryFiles);
        processDiagnostics(dh);
    }

    public String[] tokens(SourceRange range) {
        try (Tokens tokens = tokenize(range)) {
            String rv[] = new String[tokens.size()];
            for (int i = 0; i < rv.length; i++) {
                rv[i] = tokens.getToken(i).spelling();
            }
            return rv;
        }
    }

    public Tokens tokenize(SourceRange range) {
        try (NativeArena session = NativeArena.openConfined()) {
            MemorySegment p = session.allocate(C_POINTER);
            MemorySegment pCnt = session.allocate(C_INT);
            Index_h.clang_tokenize(ptr, range.segment, p, pCnt);
            Tokens rv = new Tokens(p.get(C_POINTER, 0), pCnt.get(C_INT, 0));
            return rv;
        }
    }

    public class Tokens extends ClangDisposable {
        private final int size;

        Tokens(MemorySegment addr, int size) {
            super(addr, size * CXToken.$LAYOUT().byteSize(),
                    () -> Index_h.clang_disposeTokens(TranslationUnit.this.ptr, addr, size));
            this.size = size;
        }

        public int size() {
            return size;
        }

        public MemorySegment getTokenSegment(int idx) {
            return ptr.asSlice(idx * CXToken.$LAYOUT().byteSize());
        }

        public Token getToken(int index) {
            return new Token(getTokenSegment(index), this);
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < size; i++) {
                sb.append("Token[");
                sb.append(i);
                sb.append("]=");
                sb.append(getToken(i).spelling());
                sb.append("\n");
            }
            return sb.toString();
        }

        public class Token extends ClangDisposable.Owned {
            Token(MemorySegment token, ClangDisposable owner) {
                super(token, owner);
            }

            public int kind() {
                return Index_h.clang_getTokenKind(segment);
            }

            public String spelling() {
                var spelling = Index_h.clang_getTokenSpelling(STRING_ALLOCATOR, TranslationUnit.this.ptr, segment);
                return LibClang.CXStrToString(spelling);
            }

            public SourceLocation getLocation() {
                var tokenLoc = Index_h.clang_getTokenLocation(owner, TranslationUnit.this.ptr, segment);
                return new SourceLocation(tokenLoc, owner);
            }

            public SourceRange getExtent() {
                var tokenExt = Index_h.clang_getTokenExtent(owner, TranslationUnit.this.ptr, segment);
                return new SourceRange(tokenExt, owner);
            }
        }
    }

    public static class TranslationUnitSaveException extends IOException {

        static final long serialVersionUID = 1L;

        private final SaveError error;

        TranslationUnitSaveException(Path path, SaveError error) {
            super("Cannot save translation unit to: " + path.toAbsolutePath() + ". Error: " + error);
            this.error = error;
        }
    }
}
