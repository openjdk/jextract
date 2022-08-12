/*
 *  Copyright (c) 2022, Oracle and/or its affiliates. All rights reserved.
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
 */

// Generated by jextract

package org.openjdk.jextract.clang.libclang;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.VarHandle;
import java.nio.ByteOrder;
import java.lang.foreign.*;
import static java.lang.foreign.ValueLayout.*;
public interface CXCursorVisitor {

    int apply(java.lang.foreign.MemorySegment cursor, java.lang.foreign.MemorySegment parent, java.lang.foreign.MemorySegment client_data);
    static MemorySegment allocate(CXCursorVisitor fi, MemorySession session) {
        return RuntimeHelper.upcallStub(CXCursorVisitor.class, fi, constants$13.CXCursorVisitor$FUNC, session);
    }
    static CXCursorVisitor ofAddress(MemorySegment addr, MemorySession session) {
        MemorySegment symbol = MemorySegment.ofAddress(addr.address(), 0, session);
        return (java.lang.foreign.MemorySegment _cursor, java.lang.foreign.MemorySegment _parent, java.lang.foreign.MemorySegment _client_data) -> {
            try {
                return (int)constants$13.CXCursorVisitor$MH.invokeExact((MemorySegment)symbol, _cursor, _parent, (java.lang.foreign.MemorySegment)_client_data);
            } catch (Throwable ex$) {
                throw new AssertionError("should not reach here", ex$);
            }
        };
    }
}


