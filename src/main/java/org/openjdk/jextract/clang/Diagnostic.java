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

import static org.openjdk.jextract.clang.LibClang.STRING_ALLOCATOR;

public class Diagnostic extends ClangDisposable {

    // Various Diagnostic severity levels - from Clang enum CXDiagnosticSeverity

    /**
     * A diagnostic that has been suppressed, e.g., by a command-line
     * option.
     */
    public static final int CXDiagnostic_Ignored = Index_h.CXDiagnostic_Ignored();

    /**
     * This diagnostic is a note that should be attached to the
     * previous (non-note) diagnostic.
     */
    public static final int CXDiagnostic_Note    = Index_h.CXDiagnostic_Note();

    /**
     * This diagnostic indicates suspicious code that may not be
     * wrong.
     */
    public static final int CXDiagnostic_Warning = Index_h.CXDiagnostic_Warning();

    /**
     * This diagnostic indicates that the code is ill-formed.
     */
    public static final int CXDiagnostic_Error   = Index_h.CXDiagnostic_Error();

    /**
     * This diagnostic indicates that the code is ill-formed such
     * that future parser recovery is unlikely to produce useful
     * results.
     */
    public static final int CXDiagnostic_Fatal   = Index_h.CXDiagnostic_Fatal();

    Diagnostic(MemorySegment ptr) {
        super(ptr, () -> Index_h.clang_disposeDiagnostic(ptr));
    }

    public int severity() {
        return Index_h.clang_getDiagnosticSeverity(ptr);
    }

    public SourceLocation location() {
        var loc = Index_h.clang_getDiagnosticLocation(arena, ptr);
        return new SourceLocation(loc, this);
    }

    public String spelling() {
       var spelling = Index_h.clang_getDiagnosticSpelling(STRING_ALLOCATOR, ptr);
       return LibClang.CXStrToString(spelling);
    }

    @Override
    public String toString() {
        var diagString = Index_h.clang_formatDiagnostic(arena, ptr,
                Index_h.clang_defaultDiagnosticDisplayOptions());
        return LibClang.CXStrToString(diagString);
    }
}
