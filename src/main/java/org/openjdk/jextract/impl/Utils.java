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

import org.openjdk.jextract.Type.Delegated;
import org.openjdk.jextract.clang.Cursor;
import org.openjdk.jextract.clang.CursorKind;
import org.openjdk.jextract.clang.SourceLocation;
import org.openjdk.jextract.clang.Type;

import javax.lang.model.SourceVersion;
import javax.tools.JavaFileObject;
import javax.tools.SimpleJavaFileObject;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * General utility functions
 */
class Utils {
    public static String qualifiedClassName(String packageName, String simpleName) {
        return (packageName.isEmpty() ? "" : packageName + ".") + simpleName;
    }

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

    static String javaSafeIdentifier(String name) {
        return javaSafeIdentifier(name, false);
    }

    static String javaSafeIdentifier(String name, boolean checkAllChars) {
        if (checkAllChars) {
            StringBuilder buf = new StringBuilder();
            char[] chars = name.toCharArray();
            if (Character.isJavaIdentifierStart(chars[0])) {
                buf.append(chars[0]);
            } else {
                buf.append('_');
            }
            if (chars.length > 1) {
                for (int i = 1; i < chars.length; i++) {
                    char ch = chars[i];
                    if (Character.isJavaIdentifierPart(ch)) {
                        buf.append(ch);
                    } else {
                        buf.append('_');
                    }
                }
            }
            return buf.toString();
        } else {
            // We never get the problem of Java non-identifiers (like 123, ab-xy) as
            // C identifiers. But we may have a java keyword used as a C identifier.
            assert SourceVersion.isIdentifier(name);

            return SourceVersion.isKeyword(name) || isRestrictedTypeName(name) || isJavaTypeName(name)? (name + "_") : name;
        }
    }

    private static boolean isRestrictedTypeName(String name) {
        return switch (name) {
            case "var", "yield", "record",
                "sealed", "permits" -> true;
            default -> false;
        };
    }

    private static boolean isJavaTypeName(String name) {
        // Java types that are used unqualified in the generated code
        return switch (name) {
            case "String", "MethodHandle",
                "VarHandle", "ByteOrder",
                "FunctionDescriptor", "LibraryLookup",
                "MemoryLayout",
                "Arena", "NativeArena", "MemorySegment", "ValueLayout",
                "RuntimeHelper" -> true;
            default -> false;
        };
    }

    static void validSimpleIdentifier(String name) {
        int length = name.length();
        if (length == 0) {
            throw new IllegalArgumentException();
        }

        int ch = name.codePointAt(0);
        if (length == 1 && ch == '_') {
            throw new IllegalArgumentException("'_' is no longer valid identifier.");
        }

        if (!Character.isJavaIdentifierStart(ch)) {
            throw new IllegalArgumentException("Invalid start character for an identifier: " + ch);
        }

        for (int i = 1; i < length; i++) {
            ch = name.codePointAt(i);
            if (!Character.isJavaIdentifierPart(ch)) {
                throw new IllegalArgumentException("Invalid character for an identifier: " + ch);
            }
        }
    }

    static void validPackageName(String name) {
        if (name.isEmpty()) {
            throw new IllegalArgumentException();
        }
        int idx = name.lastIndexOf('.');
        if (idx == -1) {
           validSimpleIdentifier(name);
        } else {
            validSimpleIdentifier(name.substring(idx + 1));
            validPackageName(name.substring(0, idx));
        }
    }

    static String toJavaIdentifier(String str) {
        final int size = str.length();
        StringBuilder sb = new StringBuilder(size);
        if (! Character.isJavaIdentifierStart(str.charAt(0))) {
            sb.append('_');
        }
        for (int i = 0; i < size; i++) {
            char ch = str.charAt(i);
            if (Character.isJavaIdentifierPart(ch)) {
                sb.append(ch);
            } else {
                sb.append('_');
            }
        }
        return sb.toString();
    }

    static String toSafeName(String name) {
        StringBuilder sb = new StringBuilder(name.length());
        name = toJavaIdentifier(name);
        sb.append(name);
        if (SourceVersion.isKeyword(name)) {
            sb.append("$");
        }
        return sb.toString();
    }

    static String toClassName(String cname) {
        return toSafeName(cname);
    }

    static String toMacroName(String mname) {
        return toSafeName(mname);
    }

    static String toInternalName(String pkg, String name, String... nested) {
        if ((pkg == null || pkg.isEmpty()) && nested == null) {
            return name;
        }

        StringBuilder sb = new StringBuilder();
        if (pkg != null && ! pkg.isEmpty()) {
            sb.append(pkg.replace('.', '/'));
            if (sb.charAt(sb.length() - 1) != '/') {
                sb.append('/');
            }
        }
        sb.append(name);
        for (String n: nested) {
            sb.append('$');
            sb.append(n);
        }
        return sb.toString();
    }

    static boolean isFlattenable(Cursor c) {
        return c.isAnonymousStruct() || c.kind() == CursorKind.FieldDecl;
    }

    // return builtin Record types accessible from the given Type
    static Stream<Cursor> getBuiltinRecordTypes(Type type) {
        List<Cursor> recordTypes = new ArrayList<>();
        fillBuiltinRecordTypes(type, recordTypes);
        return recordTypes.stream().distinct();
    }

    private static void fillBuiltinRecordTypes(Type type, List<Cursor> recordTypes) {
        Type canonicalType = type.canonicalType();
        switch (canonicalType.kind()) {
            case ConstantArray:
            case IncompleteArray:
                fillBuiltinRecordTypes(canonicalType.getElementType(), recordTypes);
                break;

            case FunctionProto:
            case FunctionNoProto: {
                final int numArgs = canonicalType.numberOfArgs();
                for (int i = 0; i < numArgs; i++) {
                    fillBuiltinRecordTypes(canonicalType.argType(i), recordTypes);
                }
                fillBuiltinRecordTypes(canonicalType.resultType(), recordTypes);
            }
            break;

            case Record: {
                Cursor c = canonicalType.getDeclarationCursor();
                if (c.isDefinition()) {
                    SourceLocation sloc = c.getSourceLocation();
                    if (sloc != null && sloc.getFileLocation().path() == null) {
                        recordTypes.add(c);
                    }
                }
            }
            break;

            case BlockPointer:
            case Pointer:
                fillBuiltinRecordTypes(canonicalType.getPointeeType(), recordTypes);
                break;

            case Unexposed:
                if (! canonicalType.equalType(type)) {
                    fillBuiltinRecordTypes(canonicalType, recordTypes);
                }
                break;

            case Elaborated:
            case Typedef:
                fillBuiltinRecordTypes(canonicalType, recordTypes);
                break;

            default: // nothing to do
        }
    }

    // return the absolute path of the library of given name by searching
    // in the given array of paths.
    static Optional<Path> findLibraryPath(Path[] paths, String libName) {
        return Arrays.stream(paths).
                map(p -> p.resolve(System.mapLibraryName(libName))).
                filter(Files::isRegularFile).map(Path::toAbsolutePath).findFirst();
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

    static boolean isPointerType(org.openjdk.jextract.Type type) {
        if (type instanceof Delegated delegated) {
            return delegated.kind() == Delegated.Kind.POINTER;
        } else {
            return false;
        }
    }

    /**
     * Is a character printable ASCII?
     */
    private static boolean isPrintableAscii(char ch) {
        return ch >= ' ' && ch <= '~';
    }
}
