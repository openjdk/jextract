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

import javax.tools.JavaFileObject;
import java.lang.constant.ClassDesc;
import java.util.List;

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

    protected final SourceFileBuilder sb;
    private final boolean isNested;
    final Kind kind;
    final ClassDesc desc;

    ClassSourceBuilder(SourceFileBuilder builder, boolean isNested, Kind kind, String name) {
        this.sb = builder;
        this.isNested = isNested;
        this.kind = kind;
        this.desc = ClassDesc.of(builder.packageName(), name);
    }

    boolean isNested() {
        return isNested;
    }

    String className() {
        return desc.displayName();
    }

    String fullName() {
        return isNested() ?
                sb.className() + "." + className() :
                className();
    }

    String superClass() {
        return null;
    }

    String mods() {
        if (kind == Kind.INTERFACE) {
            return "public ";
        }
        return (isNested() ? "public static " : "public ") +
                (isClassFinal() ? "final " : "");
    }

    boolean isClassFinal() {
        return true;
    }

    void classBegin() {
        if (isNested()) {
            incrAlign();
        }

        classDeclBegin();
        indent();
        append(mods());
        append(kind.kindName + " " + className());
        if (superClass() != null) {
            append(" extends ");
            append(superClass());
        }
        append(" {\n\n");
        if (kind != Kind.INTERFACE) {
            emitConstructor();
        }
    }

    void classDeclBegin() {}

    void emitConstructor() {
        incrAlign();
        indent();
        append("// Suppresses default constructor, ensuring non-instantiability.\n");
        indent();
        append("private ");
        append(className());
        append("() {}");
        append('\n');
        decrAlign();
    }

    void classEnd() {
        indent();
        append("}\n\n");
        if (isNested()) {
            decrAlign();
        }
    }

    // Internal generation helpers (used by other builders)

    void append(String s) {
        sb.append(s);
    }

    void append(char c) {
        sb.append(c);
    }

    void append(long l) {
        sb.append(l);
    }

    void indent() {
        sb.indent();
    }

    void incrAlign() {
        sb.incrAlign();
    }

    void decrAlign() {
        sb.decrAlign();
    }

    int align() {
        return sb.align();
    }

    void emitDocComment(Declaration decl) {
        emitDocComment(decl, "");
    }

    void emitDocComment(Declaration decl, String header) {
        indent();
        append("/**\n");
        if (!header.isEmpty()) {
            indent();
            append(" * ");
            append(header);
            append("\n");
        }
        indent();
        append(" * {@snippet lang=c :\n");
        append(CDeclarationPrinter.declaration(decl, " ".repeat(align()*4) + " * "));
        indent();
        append(" * }\n");
        indent();
        append(" */\n");
    }

    void emitDocComment(Type.Function funcType, String name) {
        indent();
        append("/**\n");
        indent();
        append(" * {@snippet lang=c :\n");
        append(" * ");
        append(CDeclarationPrinter.declaration(funcType, name));
        append(";\n");
        indent();
        append(" * }\n");
        indent();
        append(" */\n");
    }

    void emitConstantGetter(String mods, String getterName, boolean nullCheck, String symbolName, Constant constant) {
        incrAlign();
        indent();
        append(mods + " " + constant.type().getSimpleName() + " " + getterName + "() {\n");
        incrAlign();
        indent();
        append("return ");
        if (nullCheck) {
            append("RuntimeHelper.requireNonNull(");
        }
        append(constant.accessExpression());
        if (nullCheck) {
            append(",\"");
            append(symbolName);
            append("\")");
        }
        append(";\n");
        decrAlign();
        indent();
        append("}\n");
        decrAlign();
    }
}
