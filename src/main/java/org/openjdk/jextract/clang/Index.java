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
import java.lang.foreign.NativeArena;
import java.lang.foreign.SegmentAllocator;
import org.openjdk.jextract.clang.libclang.Index_h;

import java.nio.file.Path;
import java.util.function.Consumer;

import static org.openjdk.jextract.clang.libclang.Index_h.C_POINTER;

public class Index extends ClangDisposable {

    Index(MemorySegment addr) {
        super(addr, () -> Index_h.clang_disposeIndex(addr));
    }

    public static class UnsavedFile {
        final String file;
        final String contents;

        private UnsavedFile(Path path, String contents) {
            this.file = path.toAbsolutePath().toString();
            this.contents = contents;
        }

        public static UnsavedFile of(Path path, String contents) {
            return new UnsavedFile(path, contents);
        }
    }

    public static class ParsingFailedException extends RuntimeException {
        private static final long serialVersionUID = -1L;
        private final String srcFile;
        private final ErrorCode code;

        public ParsingFailedException(Path srcFile, ErrorCode code) {
            super("Failed to parse " + srcFile.toAbsolutePath().toString() + ": " + code);
            this.srcFile = srcFile.toAbsolutePath().toString();
            this.code = code;
        }
    }

    public TranslationUnit parseTU(String file, Consumer<Diagnostic> dh, int options, String... args)
            throws ParsingFailedException {
        try (NativeArena session = NativeArena.openConfined()) {
            SegmentAllocator allocator = SegmentAllocator.bumpAllocator(session);
            MemorySegment src = allocator.allocateUtf8String(file);
            MemorySegment cargs = args.length == 0 ? null : allocator.allocateArray(C_POINTER, args.length);
            for (int i = 0 ; i < args.length ; i++) {
                cargs.set(C_POINTER, i * C_POINTER.byteSize(), allocator.allocateUtf8String(args[i]));
            }
            MemorySegment outAddress = allocator.allocate(C_POINTER);
            ErrorCode code = ErrorCode.valueOf(Index_h.clang_parseTranslationUnit2(
                    ptr,
                    src,
                    cargs == null ? MemorySegment.NULL : cargs,
                    args.length, MemorySegment.NULL,
                    0,
                    options,
                    outAddress));

            MemorySegment tu = outAddress.get(C_POINTER, 0);
            TranslationUnit rv = new TranslationUnit(tu);
            // even if we failed to parse, we might still have diagnostics
            rv.processDiagnostics(dh);

            if (code != ErrorCode.Success) {
                throw new ParsingFailedException(Path.of(file).toAbsolutePath(), code);
            }

            return rv;
        }
    }

    private int defaultOptions(boolean detailedPreprocessorRecord) {
        int rv = Index_h.CXTranslationUnit_ForSerialization();
        rv |= Index_h.CXTranslationUnit_SkipFunctionBodies();
        if (detailedPreprocessorRecord) {
            rv |= Index_h.CXTranslationUnit_DetailedPreprocessingRecord();
        }
        return rv;
    }

    public TranslationUnit parse(String file, Consumer<Diagnostic> dh, boolean detailedPreprocessorRecord, String... args)
    throws ParsingFailedException {
        return parseTU(file, dh, defaultOptions(detailedPreprocessorRecord), args);
    }

    public TranslationUnit parse(String file, boolean detailedPreprocessorRecord, String... args)
    throws ParsingFailedException {
        return parse(file, dh -> {}, detailedPreprocessorRecord, args);
    }

}
