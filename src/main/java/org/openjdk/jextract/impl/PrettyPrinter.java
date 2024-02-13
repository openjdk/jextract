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

import java.util.Collection;
import java.util.stream.Collectors;
import org.openjdk.jextract.Declaration;
import org.openjdk.jextract.Declaration.Bitfield;
import org.openjdk.jextract.Position;
import org.openjdk.jextract.Type;

public class PrettyPrinter implements Declaration.Visitor<Void, Void> {

    private static String SPACES = " ".repeat(92);
    int align = 0;

    void incr() {
        align += 4;
    }

    void decr() {
        align -= 4;
    }

    void indent() {
        builder.append(SPACES.substring(0, align));
    }

    StringBuilder builder = new StringBuilder();

    private void getAttributes(Declaration decl) {
        Collection<Record> attrs = decl.attributes();
        if (!attrs.isEmpty()) {
            incr();
            indent();
            builder.append("Attributes: ");
            String sep = "\n";
            for (Record attr : attrs) {
                builder.append(sep);
                incr();
                indent();
                builder.append(attr);
                decr();
                sep = ",\n";
            }
            builder.append("\n");
            decr();
        }
    }

    public String print(Declaration decl) {
        decl.accept(this, null);
        return builder.toString();
    }

    @Override
    public Void visitScoped(Declaration.Scoped d, Void aVoid) {
        indent();
        builder.append("Scoped: " + d.kind() + " " + d.name() + "\n");
        getAttributes(d);
        incr();
        d.members().forEach(m -> m.accept(this, null));
        decr();
        return null;
    }

    @Override
    public Void visitFunction(Declaration.Function d, Void aVoid) {
        indent();
        builder.append("Function: " + d.name() + " type = " + d.type().accept(typeVisitor, null));
        builder.append("\n");
        getAttributes(d);
        incr();
        d.parameters().forEach(m -> m.accept(this, null));
        decr();
        return null;
    }

    @Override
    public Void visitVariable(Declaration.Variable d, Void aVoid) {
        indent();
        if (d instanceof Bitfield bitfield) {
            builder.append("Bitfield: " + " type = " + d.type().accept(typeVisitor, null) + ", name = " + bitfield.name()
                    + ", width = " + bitfield.width());
        } else {
            builder.append("Variable: " + d.kind() + " " + d.name() + " type = " + d.type().accept(typeVisitor, null));
        }
        builder.append("\n");
        getAttributes(d);
        return null;
    }

    @Override
    public Void visitConstant(Declaration.Constant d, Void aVoid) {
        indent();
        builder.append("Constant: " + d.name() + " " + d.value() + " type = " + d.type().accept(typeVisitor, null));
        builder.append("\n");
        getAttributes(d);
        return null;
    }

    @Override
    public Void visitTypedef(Declaration.Typedef d, Void aVoid) {
        indent();
        builder.append("Typedef: ").append(d.name()).append(" = ")
               .append(d.type().accept(typeVisitor, null)).append("\n");
        getAttributes(d);
        return null;
    }

    private static Type.Visitor<String, Void> typeVisitor = new Type.Visitor<>() {
        @Override
        public String visitPrimitive(Type.Primitive t, Void aVoid) {
            return t.kind().toString();
        }

        @Override
        public String visitDelegated(Type.Delegated t, Void aVoid) {
            switch (t.kind()) {
                case TYPEDEF:
                    return "typedef " + t.name() + " = " + t.type().accept(this, null);
                case POINTER:
                    return "(" + t.type().accept(this, null) + ")*";
                default:
                    return t.kind() + " = " + t.type().accept(this, null);
            }
        }

        @Override
        public String visitFunction(Type.Function t, Void aVoid) {
            String res = t.returnType().accept(this, null);
            String args = t.argumentTypes().stream()
                    .map(a -> a.accept(this, null))
                    .collect(Collectors.joining(",", "(", ")"));
            return res + args;
        }

        @Override
        public String visitDeclared(Type.Declared t, Void aVoid) {
            return "Declared(" + t.tree().name() + ")";
        }

        @Override
        public String visitArray(Type.Array t, Void aVoid) {
            String brackets = String.format("%s[%s]", t.kind() == Type.Array.Kind.VECTOR ? "v" : "",
                    t.elementCount().isPresent() ? t.elementCount().getAsLong() : "");
            return t.elementType().accept(this, null) + brackets;
        }

        @Override
        public String visitType(Type t, Void aVoid) {
            return t.isErroneous() ?
                    STR."<error: \{((TypeImpl.ErronrousTypeImpl)t).erroneousName}>" :
                    STR."<unknown: \{t.getClass().getName()}>";
        }
    };

    public static String type(Type type) {
        return type.accept(typeVisitor, null);
    }

    public static String position(Position pos) {
        return String.format("%s:%d:%d",
                pos.path() == null ? "N/A" : pos.path().toString(),
                pos.line(), pos.col());
    }
}
