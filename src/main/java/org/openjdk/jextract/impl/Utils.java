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

package org.openjdk.jextract.impl;

import org.openjdk.jextract.Declaration;
import org.openjdk.jextract.Type;
import org.openjdk.jextract.Type.Delegated;
import org.openjdk.jextract.Type.Function;
import org.openjdk.jextract.clang.Cursor;
import org.openjdk.jextract.clang.CursorKind;

import javax.tools.JavaFileObject;
import javax.tools.SimpleJavaFileObject;
import java.lang.foreign.AddressLayout;
import java.io.IOException;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.lang.invoke.MethodType;
import java.net.URI;
import java.util.Map;

/**
 * General utility functions
 */
class Utils {
    private static URI fileName(String pkgName, String clsName, String extension) {
        String pkgPrefix = pkgName.isEmpty() ? "" : pkgName.replaceAll("\\.", "/") + "/";
        return URI.create(pkgPrefix + clsName + extension);
    }

    static JavaFileObject fileFromString(String pkgName, String clsName, String contents) {
        return new SimpleJavaFileObject(fileName(pkgName, clsName, ".java"), JavaFileObject.Kind.SOURCE) {
            @Override
            public CharSequence getCharContent(boolean ignoreEncodingErrors) throws IOException {
                return contents;
            }
        };
    }

    static boolean isFlattenable(Cursor c) {
        return c.isAnonymousStruct() || c.kind() == CursorKind.FieldDecl;
    }

    /*
     * FIXME: when we add jdk.compiler dependency from jdk.jextract module, revisit
     * the following. The following methods 'quote', 'quote' and 'isPrintableAscii'
     * are from javac source. See also com.sun.tools.javac.util.Convert.java.
     */

    /**
     * Escapes each character in a string that has an escape sequence or
     * is non-printable ASCII.  Leaves non-ASCII characters alone.
     */
    static String quote(String s) {
        StringBuilder buf = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            buf.append(quote(s.charAt(i)));
        }
        return buf.toString();
    }

    /**
     * Escapes a character if it has an escape sequence or is
     * non-printable ASCII.  Leaves non-ASCII characters alone.
     */
    static String quote(char ch) {
        switch (ch) {
        case '\b':  return "\\b";
        case '\f':  return "\\f";
        case '\n':  return "\\n";
        case '\r':  return "\\r";
        case '\t':  return "\\t";
        case '\'':  return "\\'";
        case '\"':  return "\\\"";
        case '\\':  return "\\\\";
        default:
            return (isPrintableAscii(ch))
                ? String.valueOf(ch)
                : String.format("\\u%04x", (int) ch);
        }
    }

    static boolean isStructOrUnion(Declaration.Scoped scoped) {
        return switch (scoped.kind()) {
            case STRUCT, UNION -> true;
            default -> false;
        };
    }

    static boolean isEnum(Declaration.Scoped scoped) {
        return switch (scoped.kind()) {
            case ENUM -> true;
            default -> false;
        };
    }

    static boolean isArray(Type type) {
        return switch (type) {
            case Type.Array _ -> true;
            case Type.Delegated delegated when delegated.kind() == Delegated.Kind.TYPEDEF ->
                    isArray(delegated.type());
            default -> false;
        };
    }

    static boolean isEnum(Type type) {
        return switch (type) {
            case Type.Declared declared -> isEnum(declared.tree());
            case Type.Delegated delegated when delegated.kind() == Delegated.Kind.TYPEDEF ->
                    isEnum(delegated.type());
            default -> false;
        };
    }

    static boolean isStructOrUnion(Type type) {
        return switch (type) {
            case Type.Declared declared -> isStructOrUnion(declared.tree());
            case Type.Delegated delegated when delegated.kind() == Delegated.Kind.TYPEDEF ->
                isStructOrUnion(delegated.type());
            default -> false;
        };
    }

    static boolean isPointer(Type type) {
        return switch (type) {
            case Type.Delegated delegated when delegated.kind() == Delegated.Kind.TYPEDEF ->
                    isPointer(delegated.type());
            case Type.Delegated delegated when delegated.kind() == Delegated.Kind.POINTER ->
                    true;
            default -> false;
        };
    }

    static boolean isPrimitive(Type type) {
        return switch (type) {
            case Type.Declared declared when declared.tree().kind() == Declaration.Scoped.Kind.ENUM ->
                isPrimitive(((Declaration.Constant)declared.tree().members().get(0)).type());
            case Type.Delegated delegated -> isPrimitive(delegated.type());
            case Type.Primitive _ -> true;
            default -> false;
        };
    }

    static Function getAsFunctionPointer(Type type) {
        return switch (type) {
            case Type.Delegated delegated -> getAsFunctionPointer(delegated.type());
            case Type.Function function -> function;
            default -> null;
        };
    }

    static Type.Primitive getAsSignedOrUnsigned(Type type) {
        if (type instanceof Type.Delegated delegated &&
            delegated.type() instanceof Type.Primitive primitive) {
            var kind = delegated.kind();
            if (kind == Type.Delegated.Kind.SIGNED ||
                kind == Type.Delegated.Kind.UNSIGNED) {
                return primitive;
            }
        }
        return null;
    }

    /**
     * Is a character printable ASCII?
     */
    private static boolean isPrintableAscii(char ch) {
        return ch >= ' ' && ch <= '~';
    }

    public static Class<?> carrierFor(Type type) {
        return switch (type) {
            case Type.Array _ -> MemorySegment.class;
            case Type.Primitive p -> Utils.carrierFor(p);
            case Type.Declared declared -> declared.tree().kind() == Declaration.Scoped.Kind.ENUM ?
                    carrierFor(((Declaration.Constant) declared.tree().members().get(0)).type()) :
                    MemorySegment.class;
            case Type.Delegated delegated -> delegated.kind() == Type.Delegated.Kind.POINTER ?
                    MemorySegment.class :
                    carrierFor(delegated.type());
            case Type.Function _ -> MemorySegment.class;
            default -> throw new UnsupportedOperationException(type.toString());
        };
    };

    public static Class<?> carrierFor(Type.Primitive p) {
        return switch (p.kind()) {
            case Void -> void.class;
            case Bool -> boolean.class;
            case Char -> byte.class;
            case Short -> short.class;
            case Int -> int.class;
            case Long -> TypeImpl.IS_WINDOWS ? int.class : long.class;
            case LongLong -> long.class;
            case Float -> float.class;
            case Double -> double.class;
            case LongDouble -> {
                if (TypeImpl.IS_WINDOWS) {
                    yield (Class<?>) double.class;
                } else {
                    throw new UnsupportedOperationException(p.toString());
                }
            }
            default -> throw new UnsupportedOperationException(p.toString());
        };
    }

    public static Class<?> valueLayoutCarrierFor(Type t) {
        if (t instanceof Delegated delegated && delegated.kind() == Delegated.Kind.POINTER) {
            return AddressLayout.class;
        } else if (t instanceof Type.Primitive p) {
            Class<?> clazz = carrierFor(p);
            return CARRIERS_TO_LAYOUT_CARRIERS.get(clazz);
        } else {
            throw new UnsupportedOperationException(t.toString());
        }
    }

    static final Map<Class<?>, Class<?>> CARRIERS_TO_LAYOUT_CARRIERS = Map.of(
            byte.class, ValueLayout.OfByte.class,
            boolean.class, ValueLayout.OfBoolean.class,
            char.class, ValueLayout.OfChar.class,
            short.class, ValueLayout.OfShort.class,
            int.class, ValueLayout.OfInt.class,
            float.class, ValueLayout.OfFloat.class,
            long.class, ValueLayout.OfLong.class,
            double.class, ValueLayout.OfDouble.class
    );

    public static MethodType methodTypeFor(Type.Function type) {
        return MethodType.methodType(
                carrierFor(type.returnType()),
                type.argumentTypes().stream().map(Utils::carrierFor).toList()
        );
    }
}
