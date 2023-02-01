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
import java.lang.foreign.MemorySegment;
import org.openjdk.jextract.clang.libclang.Index_h;

public class EvalResult implements AutoCloseable {
    private MemorySegment ptr;

    public EvalResult(MemorySegment ptr) {
        this.ptr = ptr;
    }

    public enum Kind {
        Integral,
        FloatingPoint,
        StrLiteral,
        Erroneous,
        Unknown
    }

    private int getKind0() {
        return Index_h.clang_EvalResult_getKind(ptr);
    }

    public Kind getKind() {
        int code = getKind0();
        switch (code) {
            case 1: return Kind.Integral;
            case 2: return Kind.FloatingPoint;
            case 3: case 4: case 5:
                return Kind.StrLiteral;
            default:
                return Kind.Unknown;
        }
    }

    private long getAsInt0() {
        return Index_h.clang_EvalResult_getAsLongLong(ptr);
    }

    public long getAsInt() {
        Kind kind = getKind();
        switch (kind) {
            case Integral:
                return getAsInt0();
            default:
                throw new IllegalStateException("Unexpected kind: " + kind);
        }
    }

    private double getAsFloat0() {
        return Index_h.clang_EvalResult_getAsDouble(ptr);
    }

    public double getAsFloat() {
        Kind kind = getKind();
        switch (kind) {
            case FloatingPoint:
                return getAsFloat0();
            default:
                throw new IllegalStateException("Unexpected kind: " + kind);
        }
    }

    private String getAsString0() {
        MemorySegment value = Index_h.clang_EvalResult_getAsStr(ptr);
        return value.getUtf8String(0);
    }

    public String getAsString() {
        Kind kind = getKind();
        switch (kind) {
            case StrLiteral:
                return getAsString0();
            default:
                throw new IllegalStateException("Unexpected kind: " + kind);
        }
    }

    @Override
    public void close() {
        if (ptr != MemorySegment.NULL) {
            Index_h.clang_EvalResult_dispose(ptr);
            ptr = MemorySegment.NULL;
        }
    }

    final static EvalResult erroneous = new EvalResult(MemorySegment.NULL) {
        @Override
        public Kind getKind() {
            return Kind.Erroneous;
        }

        @Override
        public void close() {
            //do nothing
        }
    };
}
