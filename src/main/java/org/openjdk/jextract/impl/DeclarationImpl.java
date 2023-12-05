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

import java.lang.constant.Constable;
import java.lang.foreign.GroupLayout;
import java.lang.foreign.MemoryLayout;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalLong;

import org.openjdk.jextract.Declaration;
import org.openjdk.jextract.Position;
import org.openjdk.jextract.Type;

public abstract class DeclarationImpl extends AttributedImpl implements Declaration {

    private final String name;
    private final Position pos;

    public DeclarationImpl(String name, Position pos, Map<String, List<Constable>> attrs) {
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
        return o instanceof Declaration decl && name().equals(decl.name());
    }

    @Override
    public int hashCode() {
        return Objects.hash(name);
    }

    public static final class TypedefImpl extends DeclarationImpl implements Declaration.Typedef {
        final Type type;

        public TypedefImpl(Type type, String name, Position pos, Map<String, List<Constable>> attrs) {
            super(name, pos, attrs);
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
                    name().equals(other.name()) &&
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

        private VariableImpl(Type type, Variable.Kind kind, String name,
                             Position pos, Map<String, List<Constable>> attrs) {
            super(name, pos, attrs);
            this.kind = Objects.requireNonNull(kind);
            this.type = Objects.requireNonNull(type);
        }

        public VariableImpl(Type type, Variable.Kind kind, String name, Position pos) {
            this(type, kind, name, pos, null);
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
            if (!(o instanceof Declaration.Variable variable)) return false;
            if (!super.equals(o)) return false;
            return kind == variable.kind() &&
                    type.equals(variable.type());
        }

        @Override
        public int hashCode() {
            return Objects.hash(super.hashCode(), kind, type);
        }
    }

    public static final class FunctionImpl extends DeclarationImpl implements Declaration.Function {

        final List<Variable> params;
        final Type.Function type;

        public FunctionImpl(Type.Function type, List<Variable> params, String name, Position pos) {
            this(type, params, name, pos, null);
        }

        public FunctionImpl(Type.Function type, List<Variable> params, String name, Position pos, Map<String, List<Constable>> attrs) {
            super(name, pos, attrs);
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
            if (!(o instanceof Declaration.Function function)) return false;
            if (!super.equals(o)) return false;
            return type.equals(function.type());
        }

        @Override
        public int hashCode() {
            return Objects.hash(super.hashCode(), type);
        }
    }

    public static class ScopedImpl extends DeclarationImpl implements Declaration.Scoped {

        private final Scoped.Kind kind;
        private final List<Declaration> declarations;

        public ScopedImpl(Kind kind, List<Declaration> declarations, String name, Position pos) {
            this(kind, declarations, name, pos, null);
        }

        ScopedImpl(Kind kind, List<Declaration> declarations,
                String name, Position pos, Map<String, List<Constable>> attrs) {
            super(name, pos, attrs);
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
        public Optional<MemoryLayout> layout() {
            return ScopedLayout.get(this);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Declaration.Scoped scoped)) return false;
            if (!super.equals(o)) return false;
            return kind == scoped.kind() &&
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
            this(type, value, name, pos, null);
        }

        public ConstantImpl(Type type, Object value, String name, Position pos, Map<String, List<Constable>> attrs) {
            super(name, pos, attrs);
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
            if (!(o instanceof Declaration.Constant constant)) return false;
            if (!super.equals(o)) return false;
            return value.equals(constant.value()) &&
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
    record AnonymousStruct() {
        private static final AnonymousStruct INSTANCE = new AnonymousStruct();

        public static void with(Scoped scoped) {
            scoped.addAttribute(INSTANCE);
        }

        public static boolean isPresent(Scoped scoped) {
            return scoped.getAttribute(AnonymousStruct.class).isPresent();
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

        public static void with(Type type) {
            type.addAttribute(INSTANCE);
        }

        public static boolean isPresent(Declaration declaration) {
            return declaration.getAttribute(Skip.class).isPresent();
        }

        public static boolean isPresent(Type type) {
            return type.getAttribute(Skip.class).isPresent();
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
     * An attribute to attach a list of Java parameter names to a C function type.
     */
    record JavaParameterNames(List<String> parameterNames) {
        public static void with(Type.Function function, List<String> parameterNames) {
            function.addAttribute(new JavaParameterNames(parameterNames));
        }

        public static Optional<List<String>> get(Type.Function function) {
            return function.getAttribute(JavaParameterNames.class)
                    .map(JavaParameterNames::parameterNames);
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

    record ScopedLayout(MemoryLayout layout) {
        public static void with(Scoped declaration, MemoryLayout layout) {
            declaration.addAttribute(new ScopedLayout(layout));
        }

        public static Optional<MemoryLayout> get(Scoped declaration) {
            return declaration.getAttribute(ScopedLayout.class)
                    .map(ScopedLayout::layout);
        }

        public static MemoryLayout getOrThrow(Scoped declaration) {
            return declaration.getAttribute(ScopedLayout.class)
                    .map(ScopedLayout::layout).get();
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
}
