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

import java.lang.foreign.MemoryLayout;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalLong;

import org.openjdk.jextract.Declaration;
import org.openjdk.jextract.Position;
import org.openjdk.jextract.Type;

public abstract class DeclarationImpl implements Declaration {

    private final String name;
    private final Position pos;
    private final Map<Class<?>, Record> attributes = new HashMap<>();

    DeclarationImpl(String name, Position pos) {
        this.name = name;
        this.pos = pos;
    }

    public String toString() {
        return new PrettyPrinter().print(this);
    }

    public String name() {
        return name;
    }

    @Override
    public Position pos() {
        return pos;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        return o instanceof Declaration decl &&
                name().equals(decl.name()) &&
                attributes.equals(decl.attributes());
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, attributes);
    }

    @Override
    public Collection<Record> attributes() {
        return attributes.values();
    }

    @Override
    @SuppressWarnings("unchecked")
    public <R extends Record> Optional<R> getAttribute(Class<R> attributeClass) {
        return Optional.ofNullable((R)attributes.get(attributeClass));
    }

    @Override
    public <R extends Record> void addAttribute(R attribute) {
        Record attr = attributes.get(attribute.getClass());
        if (attr != null && !attr.equals(attribute)) {
            throw new IllegalStateException("Attribute already exists: " + attribute.getClass().getSimpleName());
        }
        attributes.put(attribute.getClass(), attribute);
    }

    public static final class TypedefImpl extends DeclarationImpl implements Declaration.Typedef {
        final Type type;

        public TypedefImpl(Type type, String name, Position pos) {
            super(name, pos);
            this.type = Objects.requireNonNull(type);
        }

        @Override
        public <R, D> R accept(Visitor<R, D> visitor, D data) {
            return visitor.visitTypedef(this, data);
        }

        @Override
        public Type type() {
            return type;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            return o instanceof Declaration.Typedef other &&
                    super.equals(other) &&
                    type.equals(other.type());
        }

        @Override
        public int hashCode() {
            return Objects.hash(super.hashCode(), type);
        }
    }

    public static class VariableImpl extends DeclarationImpl implements Declaration.Variable {

        final Variable.Kind kind;
        final Type type;

        public VariableImpl(Type type, Variable.Kind kind, String name, Position pos) {
            super(name, pos);
            this.kind = Objects.requireNonNull(kind);
            this.type = Objects.requireNonNull(type);
        }

        @Override
        public Kind kind() {
            return kind;
        }

        @Override
        public <R, D> R accept(Visitor<R, D> visitor, D data) {
            return visitor.visitVariable(this, data);
        }

        @Override
        public Type type() {
            return type;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            return o instanceof Declaration.Variable variable &&
                    super.equals(o) &&
                    kind == variable.kind() &&
                    type.equals(variable.type());
        }

        @Override
        public int hashCode() {
            return Objects.hash(super.hashCode(), kind, type);
        }
    }

    public static final class BitfieldImpl extends VariableImpl implements Declaration.Bitfield {

        final long width;

        public BitfieldImpl(Type type, long width, String name, Position pos) {
            super(type, Kind.BITFIELD, name, pos);
            this.width = width;
        }

        @Override
        public long width() {
            return width;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            return o instanceof Declaration.Bitfield bitfield &&
                    super.equals(o) &&
                    width == bitfield.width();
        }

        @Override
        public int hashCode() {
            return Objects.hash(super.hashCode(), width);
        }
    }

    public static final class FunctionImpl extends DeclarationImpl implements Declaration.Function {

        final List<Variable> params;
        final Type.Function type;

        public FunctionImpl(Type.Function type, List<Variable> params, String name, Position pos) {
            super(name, pos);
            this.params = Objects.requireNonNull(params);
            this.type = Objects.requireNonNull(type);
        }

        @Override
        public <R, D> R accept(Visitor<R, D> visitor, D data) {
            return visitor.visitFunction(this, data);
        }

        @Override
        public List<Variable> parameters() {
            return params;
        }

        @Override
        public Type.Function type() {
            return type;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            return o instanceof Declaration.Function function &&
                    super.equals(o) &&
                    params.equals(function.parameters()) &&
                    type.equals(function.type());
        }

        @Override
        public int hashCode() {
            return Objects.hash(super.hashCode(), params, type);
        }
    }

    public static class ScopedImpl extends DeclarationImpl implements Declaration.Scoped {

        private final Scoped.Kind kind;
        private final List<Declaration> declarations;

        public ScopedImpl(Kind kind, List<Declaration> declarations, String name, Position pos) {
            super(name, pos);
            this.kind = Objects.requireNonNull(kind);
            this.declarations = Objects.requireNonNull(declarations);
        }

        @Override
        public <R, D> R accept(Visitor<R, D> visitor, D data) {
            return visitor.visitScoped(this, data);
        }

        @Override
        public List<Declaration> members() {
            return declarations;
        }

        @Override
        public Kind kind() {
            return kind;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            return o instanceof Declaration.Scoped scoped &&
                    super.equals(o) &&
                    kind == scoped.kind() &&
                    declarations.equals(scoped.members());
        }

        @Override
        public int hashCode() {
            return Objects.hash(super.hashCode(), kind, declarations);
        }
    }

    public static final class ConstantImpl extends DeclarationImpl implements Declaration.Constant {

        final Object value;
        final Type type;

        public ConstantImpl(Type type, Object value, String name, Position pos) {
            super(name, pos);
            this.value = Objects.requireNonNull(value);
            this.type = Objects.requireNonNull(type);
        }

        @Override
        public <R, D> R accept(Visitor<R, D> visitor, D data) {
            return visitor.visitConstant(this, data);
        }

        @Override
        public Object value() {
            return value;
        }

        @Override
        public Type type() {
            return type;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            return o instanceof Declaration.Constant constant &&
                    super.equals(o) &&
                    value == constant.value() &&
                    type.equals(constant.type());
        }

        @Override
        public int hashCode() {
            return Objects.hash(super.hashCode(), value, type);
        }
    }

    // attributes

    /**
     * An attribute to mark anonymous struct declarations.
     */
    record AnonymousStruct(OptionalLong offset) {
        public static void with(Scoped scoped, OptionalLong offset) {
            scoped.addAttribute(new AnonymousStruct(offset));
        }

        public static AnonymousStruct getOrThrow(Scoped scoped) {
            return scoped.getAttribute(AnonymousStruct.class).orElseThrow();
        }

        public static boolean isPresent(Scoped scoped) {
            return scoped.getAttribute(AnonymousStruct.class).isPresent();
        }

        public static String anonName(Scoped scoped) {
            return "$anon$" + scoped.pos().line() + ":" + scoped.pos().col();
        }
    }

    /**
     * An attribute to mark enum constants, with a link to the name of their parent enum.
     */
    record EnumConstant(String get) {
        public static void with(Constant constant, String enumName) {
            constant.addAttribute(new EnumConstant(enumName));
        }

        public static Optional<String> get(Constant constant) {
            return constant.getAttribute(EnumConstant.class)
                    .map(EnumConstant::get);
        }
    }

    /**
     * An attribute to mark declaration for which no code should be generated.
     */
    record Skip() {
        private static final Skip INSTANCE = new Skip();

        public static void with(Declaration declaration) {
            declaration.addAttribute(INSTANCE);
        }

        public static boolean isPresent(Declaration declaration) {
            return declaration.getAttribute(Skip.class).isPresent();
        }
    }

    /**
     * An attribute to attach a Java name to a C declaration.
     */
    record JavaName(List<String> names) {
        public static void with(Declaration declaration, List<String> names) {
            declaration.addAttribute(new JavaName(names));
        }

        public static String getOrThrow(Declaration declaration) {
            return declaration.getAttribute(JavaName.class)
                    .map(javaName -> javaName.names.getLast()).get();
        }

        public static String getFullNameOrThrow(Declaration declaration) {
            return declaration.getAttribute(JavaName.class)
                    .map(javaName -> String.join(".", javaName.names)).get();
        }

        public static boolean isPresent(Declaration declaration) {
            return declaration.getAttribute(JavaName.class).isPresent();
        }
    }

    /**
     * An attribute to attach a Java functional interface name to a C declaration.
     */
    record JavaFunctionalInterfaceName(String fiName) {
        public static void with(Declaration declaration, String fiName) {
            declaration.addAttribute(new JavaFunctionalInterfaceName(fiName));
        }

        public static Optional<String> get(Declaration declaration) {
            return declaration.getAttribute(JavaFunctionalInterfaceName.class)
                    .map(JavaFunctionalInterfaceName::fiName);
        }

        public static String getOrThrow(Declaration declaration) {
            return declaration.getAttribute(JavaFunctionalInterfaceName.class)
                    .map(JavaFunctionalInterfaceName::fiName).get();
        }
    }

    record ClangAlignOf(long align) {
        public static void with(Declaration declaration, long align) {
            declaration.addAttribute(new ClangAlignOf(align));
        }

        public static OptionalLong get(Declaration declaration) {
            return declaration.getAttribute(ClangAlignOf.class)
                    .stream().mapToLong(ClangAlignOf::align).findFirst();
        }

        public static long getOrThrow(Declaration declaration) {
            return declaration.getAttribute(ClangAlignOf.class)
                    .stream().mapToLong(ClangAlignOf::align).findFirst().getAsLong();
        }
    }

    record ClangSizeOf(long size) {
        public static void with(Declaration declaration, long size) {
            declaration.addAttribute(new ClangSizeOf(size));
        }

        public static OptionalLong get(Declaration declaration) {
            return declaration.getAttribute(ClangSizeOf.class)
                    .stream().mapToLong(ClangSizeOf::size).findFirst();
        }

        public static long getOrThrow(Declaration declaration) {
            return declaration.getAttribute(ClangSizeOf.class)
                    .stream().mapToLong(ClangSizeOf::size).findFirst().getAsLong();
        }
    }

    record ClangOffsetOf(long offset) {
        public static void with(Declaration declaration, long size) {
            declaration.addAttribute(new ClangOffsetOf(size));
        }

        public static OptionalLong get(Declaration declaration) {
            return declaration.getAttribute(ClangOffsetOf.class)
                    .stream().mapToLong(ClangOffsetOf::offset).findFirst();
        }

        public static long getOrThrow(Declaration declaration) {
            return declaration.getAttribute(ClangOffsetOf.class)
                    .stream().mapToLong(ClangOffsetOf::offset).findFirst().getAsLong();
        }
    }

    /**
     * An attribute to attach nested struct/union/enum declarations to other declarations.
     */
    record NestedTypes(List<Type.Declared> nestedTypes) {

        public static void with(Declaration declaration, List<Type.Declared> nestedTypes) {
            declaration.addAttribute(new NestedTypes(nestedTypes));
        }

        public static Optional<List<Type.Declared>> get(Declaration declaration) {
            return declaration.getAttribute(NestedTypes.class)
                    .stream().map(NestedTypes::nestedTypes).findFirst();
        }
    }

    record DeclarationString(String declString) {
        public static void with(Declaration declaration, String declString) {
            declaration.addAttribute(new DeclarationString(declString));
        }

        public static Optional<String> get(Declaration declaration) {
            return declaration.getAttribute(DeclarationString.class)
                    .stream().map(DeclarationString::declString).findFirst();
        }

        public static String getOrThrow(Declaration declaration) {
            return declaration.getAttribute(DeclarationString.class)
                    .stream().map(DeclarationString::declString).findFirst().get();
        }
    }
}
