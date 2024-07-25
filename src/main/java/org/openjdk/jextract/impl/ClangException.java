/*
 *  Copyright (c) 2021, Oracle and/or its affiliates. All rights reserved.
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
package org.openjdk.jextract.impl;

import org.openjdk.jextract.Position;
import org.openjdk.jextract.clang.Diagnostic;

public class ClangException extends RuntimeException {
    private static final long serialVersionUID = 0L;

    private final Position position;
    private final int severity;
    private final String spelling;

    public ClangException(Position pos, int severity, String spelling) {
        super(spelling);
        this.position = pos;
        this.severity = severity;
        this.spelling = spelling;
    }

    public Position position() {
        return position;
    }

    public int severity() {
        return severity;
    }

    public String spelling() {
        return spelling;
    }

    public boolean isError() {
        return severity == Diagnostic.CXDiagnostic_Error;
    }

    public boolean isFatal() {
        return severity == Diagnostic.CXDiagnostic_Fatal;
    }
}
