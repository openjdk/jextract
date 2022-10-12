/*
 * Copyright (c) 2020, Oracle and/or its affiliates. All rights reserved.
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
import java.util.function.Consumer;
import org.openjdk.jextract.Declaration;
import org.openjdk.jextract.Type;

/**
 * Superclass for .java source generator classes.
 */
abstract class ClassSourceBuilder extends JavaSourceBuilder {

    private static final boolean SHOW_GENERATING_CLASS = Boolean.getBoolean("jextract.showGeneratingClass");

    enum Kind {
        CLASS("class"),
        INTERFACE("interface");

        final String kindName;

        Kind(String kindName) {
            this.kindName = kindName;
        }
    }

    final Kind kind;
    final ClassDesc desc;
    protected final JavaSourceBuilder enclosing;

    // code buffer
    private StringBuilder sb = new StringBuilder();
    // current line alignment (number of 4-spaces)
    private int align;

    ClassSourceBuilder(JavaSourceBuilder enclosing, Kind kind, String name) {
        this.enclosing = enclosing;
        this.align = (enclosing instanceof ClassSourceBuilder classSourceBuilder)
                ? classSourceBuilder.align : 0;
        this.kind = kind;
        this.desc = ClassDesc.of(enclosing.packageName(), name);
    }

    boolean isNested() {
        return enclosing instanceof ClassSourceBuilder;
    }

    String className() {
        return desc.displayName();
    }

    String fullName() {
        return isNested() ?
                ((ClassSourceBuilder)enclosing).className() + "." + className() :
                className();
    }

    @Override
    public final String packageName() {
        return desc.packageName();
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
        emitPackagePrefix();
        emitImportSection();

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

    JavaSourceBuilder classEnd() {
        indent();
        append("}\n\n");
        if (isNested()) {
            decrAlign();
            ((ClassSourceBuilder)enclosing).append(build());
            sb = null;
        }
        return enclosing;
    }

    @Override
    public List<JavaFileObject> toFiles() {
        if (isNested()) {
            throw new UnsupportedOperationException("Nested builder!");
        }
        String res = build();
        sb = null;
        return List.of(Utils.fileFromString(packageName(), className(), res));
    }

    // Internal generation helpers (used by other builders)

    void append(String s) {
        sb.append(s);
    }

    void append(char c) {
        sb.append(c);
    }

    void append(boolean b) {
        sb.append(b);
    }

    void append(long l) {
        sb.append(l);
    }

    void indent() {
        for (int i = 0; i < align; i++) {
            append("    ");
        }
    }

    void incrAlign() {
        align++;
    }

    void decrAlign() {
        align--;
    }

    String build() {
        String s = sb.toString();
        return s;
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
        append(" * {@snippet :\n");
        append(CDeclarationPrinter.declaration(decl, " ".repeat(align*4) + " * "));
        indent();
        append(" * }\n");
        indent();
        append(" */\n");
    }

    void emitDocComment(Type.Function funcType, String name) {
        indent();
        append("/**\n");
        indent();
        append(" * {@snippet :\n");
        append(" * ");
        append(CDeclarationPrinter.declaration(funcType, name));
        append(";\n");
        indent();
        append(" * }\n");
        indent();
        append(" */\n");
    }

    // is the name enclosed enclosed by a class of the same name?
    boolean isEnclosedBySameName(String name) {
        return className().equals(name) ||
                (isNested() && enclosing.isEnclosedBySameName(name));
    }

    protected void emitPackagePrefix() {
        if (!isNested()) {
            assert packageName().indexOf('/') == -1 : "package name invalid: " + packageName();
            append("// Generated by jextract");
            if (SHOW_GENERATING_CLASS) {
                append(" (via ");
                append(getClass().getName());
                append(")");
            }
            append("\n\n");
            if (!packageName().isEmpty()) {
                append("package ");
                append(packageName());
                append(";\n\n");
            }
        }
    }

    protected void emitImportSection() {
        if (!isNested()) {
            append("import java.lang.invoke.MethodHandle;\n");
            append("import java.lang.invoke.VarHandle;\n");
            append("import java.nio.ByteOrder;\n");
            append("import java.lang.foreign.*;\n");
            append("import static java.lang.foreign.ValueLayout.*;\n");
        }
    }

    protected void emitGetter(String mods, Class<?> type, String name, String access, boolean nullCheck, String symbolName) {
        incrAlign();
        indent();
        append(mods + " " + type.getSimpleName() + " " +name + "() {\n");
        incrAlign();
        indent();
        append("return ");
        if (nullCheck) {
            append("RuntimeHelper.requireNonNull(");
        }
        append(access);
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

    protected void emitGetter(String mods, Class<?> type, String name, String access) {
        emitGetter(mods, type, name, access, false, null);
    }

    ToplevelBuilder toplevel() {
        JavaSourceBuilder encl = enclosing;
        while (encl instanceof ClassSourceBuilder classSourceBuilder) {
            encl = classSourceBuilder.enclosing;
        }
        return (ToplevelBuilder)encl;
    }

    @Override
    protected void emitWithConstantClass(Consumer<ConstantBuilder> constantConsumer) {
        enclosing.emitWithConstantClass(constantConsumer);
    }
}
