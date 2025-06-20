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
import org.openjdk.jextract.Declaration.Scoped;
import org.openjdk.jextract.Declaration.Variable;
import org.openjdk.jextract.Type;
import org.openjdk.jextract.Type.Declared;
import org.openjdk.jextract.impl.DeclarationImpl.AnonymousStruct;
import org.openjdk.jextract.impl.DeclarationImpl.ClangAlignOf;
import org.openjdk.jextract.impl.DeclarationImpl.ClangOffsetOf;
import org.openjdk.jextract.impl.DeclarationImpl.ClangSizeOf;
import org.openjdk.jextract.impl.DeclarationImpl.JavaFunctionalInterfaceName;
import org.openjdk.jextract.impl.DeclarationImpl.JavaName;
import org.openjdk.jextract.impl.DeclarationImpl.Skip;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.stream.Collectors;
import java.util.Iterator;

/**
 * This class generates java enums.
 */
final class EnumBuilder extends ClassSourceBuilder implements OutputFactory.Builder {

    private final Declaration.Scoped tree;
    private final String memberType = "int";
    private final String className;

    EnumBuilder(SourceFileBuilder builder, String modifiers, String className,
                  ClassSourceBuilder enclosing, String runtimeHelperName, Declaration.Scoped tree) {
        super(builder, modifiers, Kind.ENUM, className, null, enclosing, runtimeHelperName);
        this.tree = tree;
        this.className = className;
    }

    void build() {
        appendBlankLine();
        emitDocComment(tree);
        classBegin();
        emitMembers(tree.members());
        appendBlankLine();
        emitVar();
        appendBlankLine();
        emitConstructor();
        appendBlankLine();
        emitGetter();
        classEnd();
    }

    void emitMembers(List<Declaration> members) {
        Iterator<Declaration> it = members.iterator();
        StringBuilder sb = new StringBuilder();
        while (it.hasNext()) {
            Declaration.Constant member = (Declaration.Constant)it.next();
            String memberName = member.name().toUpperCase();
            sb.append(STR."\{memberName}(\{member.value()})");
            if (it.hasNext()) {
                sb.append(",\n");
            }
        }
        appendIndentedLines(sb.append(";").toString());
    }

    void emitVar() {
        appendLines(STR."""
            private final \{memberType} value;
        """);
    }

    void emitConstructor() {
        appendLines(STR."""
            private \{className}(\{memberType} value) {;
                this.value = value;
            }
        """);
    }

    void emitGetter() {
        appendLines(STR."""
            public int getValue() {
                return this.value;
            }
        """);
    }
}