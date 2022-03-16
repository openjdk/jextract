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
 */

// Generated by jextract

package org.openjdk.jextract.clang.libclang;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.VarHandle;
import java.nio.ByteOrder;
import jdk.incubator.foreign.*;
import static jdk.incubator.foreign.ValueLayout.*;
class constants$17 {

    static final FunctionDescriptor clang_toggleCrashRecovery$FUNC = FunctionDescriptor.ofVoid(
        JAVA_INT
    );
    static final MethodHandle clang_toggleCrashRecovery$MH = RuntimeHelper.downcallHandle(
        "clang_toggleCrashRecovery",
        constants$17.clang_toggleCrashRecovery$FUNC, false
    );
    static final FunctionDescriptor clang_Cursor_Evaluate$FUNC = FunctionDescriptor.of(ADDRESS,
        MemoryLayout.structLayout(
            JAVA_INT.withName("kind"),
            JAVA_INT.withName("xdata"),
            MemoryLayout.sequenceLayout(3, ADDRESS).withName("data")
        )
    );
    static final MethodHandle clang_Cursor_Evaluate$MH = RuntimeHelper.downcallHandle(
        "clang_Cursor_Evaluate",
        constants$17.clang_Cursor_Evaluate$FUNC, false
    );
    static final FunctionDescriptor clang_EvalResult_getKind$FUNC = FunctionDescriptor.of(JAVA_INT,
        ADDRESS
    );
    static final MethodHandle clang_EvalResult_getKind$MH = RuntimeHelper.downcallHandle(
        "clang_EvalResult_getKind",
        constants$17.clang_EvalResult_getKind$FUNC, false
    );
    static final FunctionDescriptor clang_EvalResult_getAsInt$FUNC = FunctionDescriptor.of(JAVA_INT,
        ADDRESS
    );
    static final MethodHandle clang_EvalResult_getAsInt$MH = RuntimeHelper.downcallHandle(
        "clang_EvalResult_getAsInt",
        constants$17.clang_EvalResult_getAsInt$FUNC, false
    );
    static final FunctionDescriptor clang_EvalResult_getAsLongLong$FUNC = FunctionDescriptor.of(JAVA_LONG,
        ADDRESS
    );
    static final MethodHandle clang_EvalResult_getAsLongLong$MH = RuntimeHelper.downcallHandle(
        "clang_EvalResult_getAsLongLong",
        constants$17.clang_EvalResult_getAsLongLong$FUNC, false
    );
    static final FunctionDescriptor clang_EvalResult_isUnsignedInt$FUNC = FunctionDescriptor.of(JAVA_INT,
        ADDRESS
    );
    static final MethodHandle clang_EvalResult_isUnsignedInt$MH = RuntimeHelper.downcallHandle(
        "clang_EvalResult_isUnsignedInt",
        constants$17.clang_EvalResult_isUnsignedInt$FUNC, false
    );
}


