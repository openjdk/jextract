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

import jdk.incubator.foreign.Addressable;
import jdk.incubator.foreign.ValueLayout;
import org.openjdk.jextract.Type.Primitive;
import org.openjdk.jextract.Type;
import jdk.incubator.foreign.MemoryAddress;
import jdk.incubator.foreign.MemoryLayout;
import jdk.incubator.foreign.MemorySegment;

import java.lang.invoke.MethodType;

public class TypeTranslator implements Type.Visitor<Class<?>, Boolean> {
    @Override
    public Class<?> visitPrimitive(Type.Primitive t, Boolean isArg) {
        if (t.kind().layout().isEmpty()) {
            return void.class;
        } else {
            return layoutToClass(isFloatingPoint(t), t.kind().layout().orElseThrow(UnsupportedOperationException::new));
        }
    }

    private boolean isFloatingPoint(Type.Primitive t) {
        switch (t.kind()) {
            case Float:
            case Float128:
            case HalfFloat:
            case Double:
            case LongDouble:
                return true;
            default:
                return false;
        }
    }

    static Class<?> layoutToClass(boolean fp, MemoryLayout layout) {
        switch ((int)layout.bitSize()) {
            case 8: return byte.class;
            case 16: return short.class;
            case 32: return !fp ? int.class : float.class;
            case 64:
            case 128: return !fp ? long.class : double.class;
            default:
                throw new UnsupportedOperationException("size: " + (int)layout.bitSize());
        }
    }

    @Override
    public Class<?> visitDelegated(Type.Delegated t, Boolean isArg) {
        return t.kind() == Type.Delegated.Kind.POINTER ?
                (isArg ? Addressable.class : MemoryAddress.class) :
                t.type().accept(this, isArg);
    }

    @Override
    public Class<?> visitFunction(Type.Function t, Boolean isArg) {
        return isArg ? Addressable.class : MemoryAddress.class; // function pointer
    }

    @Override
    public Class<?> visitDeclared(Type.Declared t, Boolean isArg) {
        return switch (t.tree().kind()) {
            case UNION, STRUCT -> MemorySegment.class;
            case ENUM -> layoutToClass(false, t.tree().layout().orElseThrow(UnsupportedOperationException::new));
            default -> throw new UnsupportedOperationException("declaration kind: " + t.tree().kind());
        };
    }

    @Override
    public Class<?> visitArray(Type.Array t, Boolean isArg) {
        if (t.kind() == Type.Array.Kind.VECTOR) {
            throw new UnsupportedOperationException("vector");
        } else {
            return MemorySegment.class;
        }
    }

    @Override
    public Class<?> visitType(Type t, Boolean isArg) {
        throw new UnsupportedOperationException(t.getClass().toString());
    }

    Class<?> getJavaType(Type t, boolean isArg) {
        return t.accept(this, isArg);
    }

    MethodType getMethodType(Type.Function type, boolean downcall) {
        MethodType mtype = MethodType.methodType(getJavaType(type.returnType(), !downcall));
        for (Type arg : type.argumentTypes()) {
            mtype = mtype.appendParameterTypes(getJavaType(arg, downcall));
        }
        if (downcall && type.varargs()) {
            mtype = mtype.appendParameterTypes(Object[].class);
        }
        return mtype;
    }
}
