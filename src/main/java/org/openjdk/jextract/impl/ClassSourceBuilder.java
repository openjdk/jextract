/*
 * Copyright (c) 2020, 2023, Oracle and/or its affiliates. All rights reserved.
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
import org.openjdk.jextract.impl.Constants.Constant;

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

    ClassSourceBuilder(SourceFileBuilder builder, String modifiers, Kind kind, String className, String superName, ClassSourceBuilder enclosing) {
        this.sb = builder;
        this.modifiers = modifiers;
        this.kind = kind;
        this.className = className;
        this.superName = superName;
        this.enclosing = enclosing;
    }

    final String className() {
        // for a (nested) class 'com.foo.package.A.B.C' this will return 'C'
        return className;
    }

    final String fullName() {
        // for a (nested) class 'com.foo.package.A.B.C' this will return 'A.B.C'
        return isNested() ? enclosing.fullName() + "." + className : className;
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
        appendLines(STR."""
            \{modifiers} \{kind.kindName} \{className}\{extendsExpr} {

            """);
    }

    final void classEnd() {
        indent();
        append("}\n\n");
    }

    // Internal generation helpers (used by other builders)

    final void append(String s) {
        sb.append(s);
    }

    final void append(char c) {
        sb.append(c);
    }

    final void append(long l) {
        sb.append(l);
    }

    final void indent() {
        sb.indent();
    }

    final void incrAlign() {
        sb.incrAlign();
    }

    final void decrAlign() {
        sb.decrAlign();
    }

    final int align() {
        return sb.align();
    }

    // append multiple lines (indentation is added automatically)
    void appendLines(String s) {
        sb.appendLines(s);
    }

    // increase indentation before appending lines
    // decrease afterwards
    void appendIndentedLines(String s) {
        sb.appendIndentedLines(s);
    }

    final void emitPrivateDefaultConstructor() {
        appendLines(STR."""
            // Suppresses default constructor, ensuring non-instantiability.
            private \{className}() {}
            """);
    }

    final void emitDocComment(Declaration decl) {
        emitDocComment(decl, "");
    }

    final void emitDocComment(Declaration decl, String header) {
        appendLines(STR."""
            /**
            \{!header.isEmpty() ? STR." * \{header}\n" : ""}\
             * {@snippet lang=c :
             \{CDeclarationPrinter.declaration(decl, " ".repeat(align()*4) + " * ")}
             * }
             */
            """);
    }

    final void emitDocComment(Type.Function funcType, String name) {
        appendLines(STR."""
            /**
             * {@snippet lang=c :
             * \{CDeclarationPrinter.declaration(funcType, name)};
             * }
             */
            """);
    }

    final void emitConstantGetter(String mods, String getterName, boolean nullCheck, String symbolName, Constant constant, Declaration decl) {
        incrAlign();
        if (decl != null) {
            emitDocComment(decl);
        }
        appendLines(STR."""
            \{mods} \{constant.type().getSimpleName()} \{getterName}() {
                return \{nullCheck ? STR."RuntimeHelper.requireNonNull(\{constant}, \"\{symbolName}\")" : constant};
            }
            """);
        decrAlign();
    }
}
