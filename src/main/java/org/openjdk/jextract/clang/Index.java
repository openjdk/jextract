/*
 *  Copyright (c) 2020, 2024, Oracle and/or its affiliates. All rights reserved.
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
import org.openjdk.jextract.clang.libclang.Index_h;
import org.openjdk.jextract.clang.libclang.CXUnsavedFile;

import java.nio.file.Path;
import java.util.function.Consumer;

import static org.openjdk.jextract.clang.libclang.Index_h.C_POINTER;

public class Index extends ClangDisposable {

    Index(MemorySegment addr) {
        super(addr, Index_h::clang_disposeIndex);
    }

    public static class UnsavedFile {
        final String file;
        final String contents;

        private UnsavedFile(String file, String contents) {
            this.file = file;
            this.contents = contents;
        }

        public static UnsavedFile of(String file, String contents) {
            return new UnsavedFile(file, contents);
        }
    }

    public static class ParsingFailedException extends RuntimeException {
        private static final long serialVersionUID = -1L;
        private final String srcFile;
        private final ErrorCode code;

        public ParsingFailedException(Path srcFile, ErrorCode code) {
            this(srcFile.toAbsolutePath().toString(), code);
        }

        public ParsingFailedException(String srcFile, ErrorCode code) {
            super("Failed to parse " + srcFile + ": " + code);
            this.srcFile = srcFile;
            this.code = code;
        }
    }

    private TranslationUnit parseTUImpl(String file, String content,
                Consumer<Diagnostic> dh, int options, String... args)
                throws ParsingFailedException {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment fileSeg = arena.allocateFrom(file);
            MemorySegment contentSeg = content == null ? null : arena.allocateFrom(content);
            MemorySegment cargs = args.length == 0 ? null : arena.allocate(C_POINTER, args.length);
            for (int i = 0 ; i < args.length ; i++) {
                cargs.set(C_POINTER, i * C_POINTER.byteSize(), arena.allocateFrom(args[i]));
            }

            MemorySegment unsavedFile = contentSeg == null ?
                null : CXUnsavedFile.allocate(arena);
            if (unsavedFile != null) {
                CXUnsavedFile.Filename(unsavedFile, fileSeg);
                CXUnsavedFile.Contents(unsavedFile, contentSeg);
                CXUnsavedFile.Length(unsavedFile, content.length());
            }

            MemorySegment outAddress = arena.allocate(C_POINTER);
            ErrorCode code = ErrorCode.valueOf(Index_h.clang_parseTranslationUnit2(
                    ptr,
                    fileSeg,
                    cargs == null ? MemorySegment.NULL : cargs,
                    args.length,
                    unsavedFile == null ? MemorySegment.NULL : unsavedFile,
                    unsavedFile == null ? 0 : 1,
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


    public TranslationUnit parseTU(String file, Consumer<Diagnostic> dh, int options, String... args)
            throws ParsingFailedException {
        return parseTUImpl(file, null, dh, options, args);
    }

    public TranslationUnit parseTU(String filename, String content, Consumer<Diagnostic> dh, int options, String... args)
            throws ParsingFailedException {
        return parseTUImpl(filename, content, dh, options, args);
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

    public TranslationUnit parse(String filename, String content, Consumer<Diagnostic> dh,
            boolean detailedPreprocessorRecord, String... args) throws ParsingFailedException {
        return parseTU(filename, content, dh, defaultOptions(detailedPreprocessorRecord), args);
    }

    public TranslationUnit parse(String filename, String content, boolean detailedPreprocessorRecord, String... args)
            throws ParsingFailedException {
        return parse(filename, content, dh -> {}, detailedPreprocessorRecord, args);
    }
}
