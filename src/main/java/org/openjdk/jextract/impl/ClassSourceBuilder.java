/*
 * Copyright (c) 2020, 2024, Oracle and/or its affiliates. All rights reserved.
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

import org.openjdk.jextract.Declaration;
import org.openjdk.jextract.Type;
import org.openjdk.jextract.Type.Array;
import org.openjdk.jextract.Type.Declared;
import org.openjdk.jextract.Type.Delegated;
import org.openjdk.jextract.Type.Function;
import org.openjdk.jextract.Type.Primitive;
import org.openjdk.jextract.impl.DeclarationImpl.ClangAlignOf;
import org.openjdk.jextract.impl.DeclarationImpl.ClangEnumType;
import org.openjdk.jextract.impl.DeclarationImpl.DeclarationString;
import org.openjdk.jextract.impl.DeclarationImpl.JavaName;

import java.lang.foreign.MemoryLayout;
import java.lang.foreign.MemorySegment;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodType;
import java.lang.invoke.VarHandle;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Superclass for .java source generator classes.
 */
abstract class ClassSourceBuilder {
    enum Kind {
        CLASS("class"),
        INTERFACE("interface");

        final String kindName;

        Kind(String kindName) {
            this.kindName = kindName;
        }
    }

    private final SourceFileBuilder sb;
    private final String modifiers;
    private final Kind kind;
    private final String className;
    private final String superName;
    private final ClassSourceBuilder enclosing;
    private final String runtimeHelperName;

    ClassSourceBuilder(SourceFileBuilder builder, String modifiers, Kind kind, String className, String superName,
                       ClassSourceBuilder enclosing, String runtimeHelperName) {
        this.sb = builder;
        this.modifiers = modifiers;
        this.kind = kind;
        this.className = className;
        this.superName = superName;
        this.enclosing = enclosing;
        this.runtimeHelperName = runtimeHelperName;
    }

    final String className() {
        // for a (nested) class 'com.foo.package.A.B.C' this will return 'C'
        return className;
    }

    final String runtimeHelperName() {
        return runtimeHelperName;
    }

    // is the name enclosed by a class of the same name?
    protected final boolean isEnclosedBySameName(String name) {
        return className().equals(name) || (isNested() && enclosing.isEnclosedBySameName(name));
    }

    protected final boolean isNested() {
        return enclosing != null;
    }

    final SourceFileBuilder sourceFileBuilder() {
        return sb;
    }

    final void classBegin() {
        String extendsExpr = "";
        if (superName != null) {
            extendsExpr = " extends " + superName;
        }
        appendLines("%1$s %2$s %3$s%4$s {", modifiers, kind.kindName, className, extendsExpr);
    }

    final void classEnd() {
        appendLines("""
            }
            """);
    }

    // Internal generation helpers (used by other builders)

    final void incrAlign() {
        sb.incrAlign();
    }

    final void decrAlign() {
        sb.decrAlign();
    }

    String format(String s, String... args) {
        return String.format(s, (Object[]) args);
    }

    // append multiple lines (indentation is added automatically)
    void appendLines(String s) {
        sb.appendLines(s);
    }
    void appendLines(String s, String... args) {
        sb.appendLines(format(s, args));
    }

    void appendBlankLine() {
        appendLines("\n");
    }

    // increase indentation before appending lines
    // decrease afterwards
    void appendIndentedLines(String s) {
        sb.appendIndentedLines(s);
    }

    void appendIndentedLines(String s, String... args) {
        sb.appendIndentedLines(format(s, args));
    }

    final void emitDefaultConstructor() {
        appendIndentedLines("""

            %1$s() {
                // Should not be called directly
            }
            """, className);
    }

    final void emitDocComment(Declaration decl) {
        emitDocComment(decl, "");
    }

    final void emitDocComment(Declaration decl, String header) {
        appendLines("""
            /**
            %1$s\
             * {@snippet lang=c :
            %2$s
             * }
             */
            """, !header.isEmpty() ? String.format(" * %1$s\n", header) : "", declarationComment(decl));
    }

    public String mangleName(String javaName, Class<?> type) {
        return javaName + nameSuffix(type);
    }

    String nameSuffix(Class<?> type) {
        if (type.equals(MemorySegment.class)) {
            return "$SEGMENT";
        } else if (type.equals(MemoryLayout.class)) {
            return "$LAYOUT";
        } else if (type.equals(MethodHandle.class)) {
            return "$MH";
        } else if (type.equals(VarHandle.class)) {
            return "$VH";
        }
        throw new IllegalArgumentException("Not handled: " + type);
    }

    String layoutString(Type type) {
        return layoutString(type, Long.MAX_VALUE);
    }

    String layoutString(Type type, long align) {
        return switch (type) {
            case Primitive p -> primitiveLayoutString(p, align);
            case Declared d when Utils.isEnum(d) -> layoutString(ClangEnumType.get(d.tree()).get(), align);
            case Declared d when Utils.isStructOrUnion(d) -> alignIfNeeded(JavaName.getFullNameOrThrow(d.tree()) + ".layout()", ClangAlignOf.getOrThrow(d.tree()) / 8, align);
            case Delegated d when d.kind() == Delegated.Kind.POINTER -> alignIfNeeded(runtimeHelperName() + ".C_POINTER", 8, align);
            case Delegated d -> layoutString(d.type(), align);
            case Function _ -> alignIfNeeded(runtimeHelperName() + ".C_POINTER", 8, align);
            case Array a -> String.format("MemoryLayout.sequenceLayout(%1$d, %2$s)", a.elementCount().orElse(0L), layoutString(a.elementType(), align));
            default -> throw new UnsupportedOperationException();
        };
    }

    String functionDescriptorString(int textBoxIndent, Type.Function functionType) {
        final MethodType type = Utils.methodTypeFor(functionType);
        boolean noArgs = type.parameterCount() == 0;
        StringBuilder builder = new StringBuilder();
        if (!type.returnType().equals(void.class)) {
            builder.append("FunctionDescriptor.of(");
            builder.append("\n");
            builder.append(String.format("%1$s%2$s", indentString(textBoxIndent + 1), layoutString(functionType.returnType())));
            if (!noArgs) {
                builder.append(",");
            }
        } else {
            builder.append("FunctionDescriptor.ofVoid(");
        }
        if (!noArgs) {
            builder.append("\n");
            String delim = "";
            for (Type arg : functionType.argumentTypes()) {
                builder.append(delim);
                builder.append(String.format("%1$s%2$s", indentString(textBoxIndent + 1), layoutString(arg)));
                delim = ",\n";
            }
            builder.append("\n");
        }
        builder.append(indentString(textBoxIndent)).append(")");
        return builder.toString();
    }

    String indentString(int size) {
        return " ".repeat(size * 4);
    }

    private String primitiveLayoutString(Primitive primitiveType, long align) {
        return switch (primitiveType.kind()) {
            case Bool -> runtimeHelperName() + ".C_BOOL";
            case Char -> runtimeHelperName() + ".C_CHAR";
            case Short -> alignIfNeeded(runtimeHelperName() + ".C_SHORT", 2, align);
            case Int -> alignIfNeeded(runtimeHelperName() + ".C_INT", 4, align);
            case Long -> alignIfNeeded(runtimeHelperName() + ".C_LONG", TypeImpl.IS_WINDOWS ? 4 : 8, align);
            case LongLong -> alignIfNeeded(runtimeHelperName() + ".C_LONG_LONG", 8, align);
            case Float -> alignIfNeeded(runtimeHelperName() + ".C_FLOAT", 4, align);
            case Double -> alignIfNeeded(runtimeHelperName() + ".C_DOUBLE", 8, align);
            case LongDouble -> TypeImpl.IS_WINDOWS ?
                    alignIfNeeded(runtimeHelperName() + ".C_LONG_DOUBLE", 8, align) :
                    paddingLayoutString(8, 0);
            case HalfFloat, Char16, WChar -> paddingLayoutString(2, 0); // unsupported
            case Float128, Int128 -> paddingLayoutString(16, 0); // unsupported
            default -> throw new UnsupportedOperationException(primitiveType.toString());
        };
    }

    private String alignIfNeeded(String layoutPrefix, long align, long expectedAlign) {
        return align > expectedAlign ?
                String.format("%1$s.align(%2$s, %3$d)", runtimeHelperName(), layoutPrefix, expectedAlign) :
                layoutPrefix;
    }

    String paddingLayoutString(long size, int indent) {
        return String.format("%1$sMemoryLayout.paddingLayout(%2$d)", indentString(indent), size);
    }

    // Return C source style signature for the given declaration.
    // A " * " prefix is emitted for every line.
    static String declarationComment(Declaration decl) {
        Objects.requireNonNull(decl);
        String declString = DeclarationString.getOrThrow(decl);
        return declString.lines().collect(Collectors.joining("\n * ", " * ", ""));
    }

    record IndexList(String decl, String use) {
        static IndexList of(int dims) {
            List<String> indexNames = IntStream.range(0, dims).mapToObj(i -> "index" + i).toList();
            String indexDecls = indexNames.stream()
                    .map(i -> "long " + i)
                    .collect(Collectors.joining(", "));
            String indexUses = indexNames.stream()
                    .collect(Collectors.joining(", "));
            return new IndexList(indexDecls, indexUses);
        }
    }
}
