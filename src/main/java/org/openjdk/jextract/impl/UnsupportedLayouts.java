/*
 * Copyright (c) 2020, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */
package org.openjdk.jextract.impl;

import java.lang.foreign.MemoryLayout;
import org.openjdk.jextract.Declaration;
import org.openjdk.jextract.Type;

import java.nio.ByteOrder;

/*
 * Layouts for the primitive types not supported by ABI implementations.
 */
public final class UnsupportedLayouts {
    private UnsupportedLayouts() {}

    public static final MemoryLayout __INT128 = makeUnsupportedLayout(16, "__int128");

    public static final MemoryLayout LONG_DOUBLE = makeUnsupportedLayout(16, "long double");

    public static final MemoryLayout _FLOAT128 = makeUnsupportedLayout(16, "_float128");

    public static final MemoryLayout __FP16 = makeUnsupportedLayout(2, "__fp16");

    public static final MemoryLayout CHAR16 = makeUnsupportedLayout(2, "char16");

    public static final MemoryLayout WCHAR_T = makeUnsupportedLayout(2, "wchar_t");

    static String firstUnsupportedType(Type type) {
        return type.accept(unsupportedVisitor, null);
    }

    private static MemoryLayout makeUnsupportedLayout(long size, String name) {
        return MemoryLayout.paddingLayout(size).withByteAlignment(size).withName(name);
    }

    static Type.Visitor<String, Void> unsupportedVisitor = new Type.Visitor<>() {
        @Override
        public String visitPrimitive(Type.Primitive t, Void unused) {
            MemoryLayout layout = t.kind().layout().orElse(MemoryLayout.paddingLayout(8));
            if (layout.equals(__INT128) || layout.equals(LONG_DOUBLE) || layout.equals(_FLOAT128) || layout.equals(__FP16)) {
                return layout.name().get();
            } else {
                return null;
            }
        }

        @Override
        public String visitFunction(Type.Function t, Void unused) {
            for (Type arg : t.argumentTypes()) {
                String unsupported = firstUnsupportedType(arg);
                if (unsupported != null) {
                    return unsupported;
                }
            }
            String unsupported = firstUnsupportedType(t.returnType());
            if (unsupported != null) {
                return unsupported;
            }
            return null;
        }

        @Override
        public String visitDeclared(Type.Declared t, Void unused) {
            for (Declaration d : t.tree().members()) {
                if (d instanceof Declaration.Variable variable) {
                    String unsupported = firstUnsupportedType(variable.type());
                    if (unsupported != null) {
                        return unsupported;
                    }
                }
            }
            return null;
        }

        @Override
        public String visitDelegated(Type.Delegated t, Void unused) {
            return t.kind() != Type.Delegated.Kind.POINTER ?
                    firstUnsupportedType(t.type()) :
                    null;
            //in principle we should always do this:
            // return firstUnsupportedType(t.type());
            // but if we do that, we might end up with infinite recursion (because of pointer types).
            // Unsupported pointer types (e.g. *long double) are not detected, but they are not problematic layout-wise
            // (e.g. they are always 32- or 64-bits, depending on the platform).
        }

        @Override
        public String visitArray(Type.Array t, Void unused) {
            return firstUnsupportedType(t.elementType());
        }

        @Override
        public String visitType(Type t, Void unused) {
            return null;
        }
    };
}
