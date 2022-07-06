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
import org.openjdk.jextract.clang.libclang.Index_h;

import java.lang.foreign.NativeArena;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;

import static org.openjdk.jextract.clang.LibClang.STRING_ALLOCATOR;
import static org.openjdk.jextract.clang.libclang.Index_h.C_INT;
import static org.openjdk.jextract.clang.libclang.Index_h.C_POINTER;

public class SourceLocation extends ClangDisposable.Owned {

    private final MemorySegment loc;

    SourceLocation(MemorySegment loc, ClangDisposable owner) {
        super(loc, owner);
        this.loc = loc;
    }

    @FunctionalInterface
    private interface LocationFactory {
        void get(MemorySegment loc, MemorySegment file,
                 MemorySegment line, MemorySegment column, MemorySegment offset);
    }

    @SuppressWarnings("unchecked")
    private Location getLocation(LocationFactory fn) {
        try (var session = NativeArena.openConfined()) {
             MemorySegment file = session.allocate(C_POINTER);
             MemorySegment line = session.allocate(C_INT);
             MemorySegment col = session.allocate(C_INT);
             MemorySegment offset = session.allocate(C_INT);

            fn.get(loc, file, line, col, offset);
            MemorySegment fname = file.get(C_POINTER, 0);
            String str = fname == MemorySegment.NULL ?  null : getFileName(fname);

            return new Location(str, line.get(C_INT, 0),
                col.get(C_INT, 0), offset.get(C_INT, 0));
        }
    }

    private static String getFileName(MemorySegment fname) {
        var filename = Index_h.clang_getFileName(STRING_ALLOCATOR, fname);
        return LibClang.CXStrToString(filename);
    }

    public Location getFileLocation() { return getLocation(Index_h::clang_getFileLocation); }
    public Location getExpansionLocation() { return getLocation(Index_h::clang_getExpansionLocation); }
    public Location getSpellingLocation() { return getLocation(Index_h::clang_getSpellingLocation); }
    public boolean isInSystemHeader() {
        return Index_h.clang_Location_isInSystemHeader(loc) != 0;
    }

    public boolean isFromMainFile() {
        return Index_h.clang_Location_isFromMainFile(loc) != 0;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        return other instanceof SourceLocation sloc &&
                Objects.equals(getFileLocation(), sloc.getFileLocation());
    }

    @Override
    public int hashCode() {
        return getFileLocation().hashCode();
    }

    public final static class Location {
        private final Path path;
        private final int line;
        private final int column;
        private final int offset;

        private Location(String filename, int line, int column, int offset) {
            if (filename == null || filename.isEmpty()) {
                this.path = null;
            } else {
                this.path = Paths.get(filename);
            }

            this.line = line;
            this.column = column;
            this.offset = offset;
        }

        public Path path() {
            return path;
        }

        public int line() {
            return line;
        }

        public int column() {
            return column;
        }

        public int offset() {
            return offset;
        }

        @Override
        public boolean equals(Object other) {
            if (this == other) {
                return true;
            }
            return other instanceof Location loc &&
                Objects.equals(path, loc.path) &&
                line == loc.line && column == loc.column &&
                offset == loc.offset;
        }

        @Override
        public int hashCode() {
            return Objects.hashCode(path) ^ line ^ column ^ offset;
        }

        @Override
        public String toString() {
            return Objects.toString(path) + ":" + line + ":" + column + ":" + offset;
        }
    }
}
