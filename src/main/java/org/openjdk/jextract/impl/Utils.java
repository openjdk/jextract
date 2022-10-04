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
import java.lang.foreign.MemoryLayout;
import java.io.IOException;
import java.net.URI;

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

    /**
     * Returns the type that should be used in declarations of various
     * memory layout implementations.
     * <p>
     * For example, the concrete layout implementation class {@code OfLongImpl} should be
     * declared as {@code OfLong} and not {@code OfLongImpl}.
     *
     * @param layout to generate a declaring type string for.
     * @return the unqualified type
     */
    static Class<?> layoutDeclarationType(MemoryLayout layout) {
        if (!layout.getClass().isInterface()) {
            Class<?> ifs[] = layout.getClass().getInterfaces();
            if (ifs.length != 1) {
                throw new IllegalStateException("The class" + layout.getClass() + " does not implement exactly one interface");
            }
            return ifs[0];
        }
        return layout.getClass();
    }

    static boolean isStructOrUnion(Declaration.Scoped scoped) {
        return switch (scoped.kind()) {
            case STRUCT, UNION -> true;
            default -> false;
        };
    }

    static boolean isPointerType(Type type) {
        if (type instanceof Delegated delegated) {
            return delegated.kind() == Delegated.Kind.POINTER;
        } else {
            return false;
        }
    }

    static Function getAsFunctionPointer(Type type) {
        if (type instanceof Function function) {
            /*
             * // pointer to function declared as function like this
             *
             * typedef void CB(int);
             * void func(CB cb);
             */
            return function;
        } else if (isPointerType(type)) {
            return getAsFunctionPointer(((Delegated)type).type());
        } else {
            return null;
        }
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
}
