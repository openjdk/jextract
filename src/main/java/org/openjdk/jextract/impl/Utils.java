/*
 *  Copyright (c) 2020, 2025, Oracle and/or its affiliates. All rights reserved.
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
import org.openjdk.jextract.Type.Delegated.Kind;
import org.openjdk.jextract.Type.Function;
import org.openjdk.jextract.clang.Cursor;
import org.openjdk.jextract.clang.CursorKind;
import org.openjdk.jextract.impl.DeclarationImpl.ClangEnumType;
import org.openjdk.jextract.impl.DeclarationImpl.NestedDeclarations;

import java.lang.foreign.AddressLayout;
import java.lang.foreign.GroupLayout;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.SequenceLayout;
import java.lang.foreign.ValueLayout;
import java.lang.invoke.MethodType;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * General utility functions
 */
class Utils {
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

    static void forEachNested(Declaration declaration, Consumer<Declaration> nestedDeclAction) {
        NestedDeclarations.get(declaration).ifPresent(decls ->
            decls.forEach(nestedDeclAction));
    }

    static boolean isStructOrUnion(Declaration declaration) {
        return declaration instanceof Declaration.Scoped scoped &&
                (scoped.kind() == Declaration.Scoped.Kind.STRUCT ||
                 scoped.kind() == Declaration.Scoped.Kind.UNION);
    }

    static boolean isEnum(Declaration declaration) {
        return declaration instanceof Declaration.Scoped scoped &&
                scoped.kind() == Declaration.Scoped.Kind.ENUM;
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
        return structOrUnionDecl(type) != null;
    }

    static Declaration.Scoped structOrUnionDecl(Type type) {
        return switch (type) {
            case Type.Declared declared when isStructOrUnion(declared.tree()) -> declared.tree();
            case Type.Delegated delegated when delegated.kind() == Delegated.Kind.TYPEDEF ->
                    structOrUnionDecl(delegated.type());
            default -> null;
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

    static boolean isFunctionPointer(Type type) {
        return switch (type) {
            case Type.Delegated delegated when delegated.kind() == Delegated.Kind.POINTER ->
                    delegated.type() instanceof Type.Function;
            case Type.Delegated delegated when delegated.kind() == Delegated.Kind.TYPEDEF ->
                    isFunctionPointer(delegated.type());
            default -> false;
        };
    }


    static boolean isPrimitive(Type type) {
        return switch (type) {
            case Type.Declared declared when declared.tree().kind() == Declaration.Scoped.Kind.ENUM -> true;
            case Type.Delegated delegated when delegated.kind() != Kind.POINTER -> isPrimitive(delegated.type());
            case Type.Primitive _ -> true;
            default -> false;
        };
    }

    static Function getAsFunctionPointer(Type type) {
        return switch (type) {
            case Type.Delegated delegated when delegated.kind() == Kind.POINTER -> getAsFunctionPointer(delegated.type());
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

    static List<Long> dimensions(Type type) {
        List<Long> dims = new ArrayList<>();
        while (type instanceof Type.Array array) {
            if (array.elementCount().isEmpty()) return List.of();
            dims.add(array.elementCount().getAsLong());
            type = array.elementType();
        }
        return dims;
    }

    static Type typeOrElemType(Type type) {
        return switch (type) {
            case Type.Array array -> typeOrElemType(array.elementType());
            default -> type;
        };
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
                    carrierFor(ClangEnumType.get(declared.tree()).get()) :
                    MemorySegment.class;
            case Type.Delegated delegated -> delegated.kind() == Type.Delegated.Kind.POINTER ?
                    MemorySegment.class :
                    carrierFor(delegated.type());
            case Type.Function _ -> MemorySegment.class;
            default -> throw new UnsupportedOperationException(type.toString());
        };
    }

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

    public static Class<?> layoutCarrierFor(Type t) {
        return switch (t) {
            case Type.Array _ -> SequenceLayout.class;
            case Delegated delegated when delegated.kind() == Kind.POINTER -> AddressLayout.class;
            case Delegated delegated -> layoutCarrierFor(delegated.type());
            case Type.Primitive primitive -> {
                Class<?> clazz = carrierFor(primitive);
                yield CARRIERS_TO_LAYOUT_CARRIERS.get(clazz);
            }
            case Type.Declared declared when isStructOrUnion(declared) -> GroupLayout.class;
            case Type.Declared declared when isEnum(declared) -> layoutCarrierFor(ClangEnumType.get(declared.tree()).get());
            default -> throw new UnsupportedOperationException(t.toString());
        };
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
