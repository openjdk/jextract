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
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.lang.foreign.MemoryLayout;
import org.openjdk.jextract.Declaration;
import org.openjdk.jextract.Position;
import org.openjdk.jextract.Type;

public abstract class DeclarationImpl implements Declaration {

    private final String name;
    private final Position pos;
    private final Optional<Map<String, List<Constable>>> attributes;

    public DeclarationImpl(String name, Position pos, Map<String, List<Constable>> attrs) {
        this.name = name;
        this.pos = pos;
        this.attributes = Optional.ofNullable(attrs);
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
    public Optional<List<Constable>> getAttribute(String name) {
        return attributes.map(attrs -> attrs.get(name));
    }

    @Override
    public Set<String> attributeNames() { return Collections.unmodifiableSet(
            attributes.map(Map::keySet).orElse(Collections.emptySet()));
    }

    @Override
    public Declaration withAttribute(String name, Constable... values) {
        if (values == null || values.length == 0) {
            return withAttributes(null);
        }
        var attrs = attributes.map(HashMap::new).orElseGet(HashMap::new);
        attrs.put(name, List.of(values));
        return withAttributes(attrs);
    }

    abstract protected Declaration withAttributes(Map<String, List<Constable>> attrs);

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
        public Typedef withAttributes(Map<String, List<Constable>> attrs) {
            return new TypedefImpl(type, name(), pos(), attrs);
        }

        @Override
        public Typedef stripAttributes() {
            return new TypedefImpl(type, name(), pos(), null);
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
        final Optional<MemoryLayout> layout;

        private VariableImpl(Type type, Optional<MemoryLayout> layout, Variable.Kind kind, String name, Position pos, Map<String, List<Constable>> attrs) {
            super(name, pos, attrs);
            this.kind = Objects.requireNonNull(kind);
            this.type = Objects.requireNonNull(type);
            this.layout = Objects.requireNonNull(layout);
        }

        public VariableImpl(Type type, Variable.Kind kind, String name, Position pos) {
            this(type, TypeImpl.getLayout(type), kind, name, pos, null);
        }

        public VariableImpl(Type type, MemoryLayout layout, Variable.Kind kind, String name, Position pos) {
            this(type, Optional.of(layout), kind, name, pos, null);
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
        public Variable withAttributes(Map<String, List<Constable>> attrs) {
            return new VariableImpl(type, layout, kind, name(), pos(), attrs);
        }

        @Override
        public Variable stripAttributes() {
            return new VariableImpl(type, layout, kind, name(), pos(), null);
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

    public static final class BitfieldImpl extends VariableImpl implements Declaration.Bitfield {

        final long offset;
        final long width;

        private BitfieldImpl(Type type, long offset, long width, String name, Position pos, Map<String, List<Constable>> attrs) {
            super(type, Optional.<MemoryLayout>empty(), Kind.BITFIELD, name, pos, attrs);
            this.offset = offset;
            this.width = width;
        }

        public BitfieldImpl(Type type, long offset, long width, String name, Position pos) {
            this(type, offset, width, name, pos, null);
        }

        @Override
        public long offset() {
            return offset;
        }

        @Override
        public long width() {
            return width;
        }

        @Override
        public Variable withAttributes(Map<String, List<Constable>> attrs) {
            return new BitfieldImpl(type, offset, width, name(), pos(), attrs);
        }

        @Override
        public Variable stripAttributes() {
            return new BitfieldImpl(type, offset, width, name(), pos(), null);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof BitfieldImpl bitfield)) return false;
            if (!super.equals(o)) return false;
            return offset == bitfield.offset &&
                    width == bitfield.width;
        }

        @Override
        public int hashCode() {
            return Objects.hash(super.hashCode(), offset, width);
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
        public Function withAttributes(Map<String, List<Constable>> attrs) {
            return new FunctionImpl(type, params, name(), pos(), attrs);
        }

        @Override
        public Function stripAttributes() {
            return new FunctionImpl(type, params, name(), pos(), null);
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
        private final Optional<MemoryLayout> optLayout;

        public ScopedImpl(Kind kind, MemoryLayout layout, List<Declaration> declarations, String name, Position pos) {
            this(kind, Optional.of(layout), declarations, name, pos, null);
        }

        public ScopedImpl(Kind kind, List<Declaration> declarations, String name, Position pos) {
            this(kind, Optional.empty(), declarations, name, pos, null);
        }

        ScopedImpl(Kind kind, Optional<MemoryLayout> optLayout, List<Declaration> declarations,
                String name, Position pos, Map<String, List<Constable>> attrs) {
            super(name, pos, attrs);
            this.kind = Objects.requireNonNull(kind);
            this.declarations = Objects.requireNonNull(declarations);
            this.optLayout = Objects.requireNonNull(optLayout);
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
        public Optional<MemoryLayout> layout() {
            return optLayout;
        }

        @Override
        public Kind kind() {
            return kind;
        }

        @Override
        public Scoped withAttributes(Map<String, List<Constable>> attrs) {
            return new ScopedImpl(kind, optLayout, declarations, name(), pos(), attrs);
        }

        @Override
        public Scoped stripAttributes() {
            return new ScopedImpl(kind, optLayout, declarations, name(), pos(), null);
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
        public Constant withAttributes(Map<String, List<Constable>> attrs) {
            return new ConstantImpl(type, value, name(), pos(), attrs);
        }

        @Override
        public Constant stripAttributes() {
            return new ConstantImpl(type, value, name(), pos(), null);
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
}
