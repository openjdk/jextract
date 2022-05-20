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
import org.openjdk.jextract.Declaration;
import org.openjdk.jextract.clang.Cursor;
import org.openjdk.jextract.clang.CursorKind;
import org.openjdk.jextract.clang.Type;
import org.openjdk.jextract.clang.TypeKind;

import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Base class for C struct, union MemoryLayout computer helper classes.
 */
abstract class RecordLayoutComputer {
    // enclosing struct type (or this struct type for top level structs)
    final Type parent;
    // this struct type
    final Type type;
    // cursor of this struct
    final Cursor cursor;
    final List<Declaration> fieldDecls;
    final List<MemoryLayout> fieldLayouts;

    final TypeMaker typeMaker;

    private int anonCount = 0;

    RecordLayoutComputer(TypeMaker typeMaker, Type parent, Type type) {
        this.parent = parent;
        this.type = type;
        this.cursor = type.getDeclarationCursor().getDefinition();
        this.fieldDecls = new ArrayList<>();
        this.fieldLayouts = new ArrayList<>();
        this.typeMaker = typeMaker;
    }

    static org.openjdk.jextract.Type compute(TypeMaker typeMaker, long offsetInParent, Type parent, Type type) {
        return computeInternal(typeMaker, offsetInParent, parent, type, null);
    }

    private static org.openjdk.jextract.Type computeAnonymous(TypeMaker typeMaker, long offsetInParent, Type parent, Type type, String name) {
        return computeInternal(typeMaker, offsetInParent, parent, type, name);
    }

    static final org.openjdk.jextract.Type.Declared ERRONEOUS = org.openjdk.jextract.Type.declared(
            Declaration.struct(TreeMaker.CursorPosition.NO_POSITION, "", MemoryLayout.paddingLayout(64)));

    private static org.openjdk.jextract.Type computeInternal(TypeMaker typeMaker, long offsetInParent, Type parent, Type type, String name) {
        Cursor cursor = type.getDeclarationCursor().getDefinition();
        if (cursor.isInvalid()) {
            return ERRONEOUS;
        }

        final boolean isUnion = cursor.kind() == CursorKind.UnionDecl;
        return isUnion? new UnionLayoutComputer(typeMaker, offsetInParent, parent, type).compute(name) :
                new StructLayoutComputer(typeMaker, offsetInParent, parent, type).compute(name);
    }

    final org.openjdk.jextract.Type.Declared compute(String anonName) {
        Stream<Cursor> fieldCursors = Utils.flattenableChildren(cursor);
        for (Cursor fc : fieldCursors.collect(Collectors.toList())) {
            /*
             * Ignore bitfields of zero width.
             *
             * struct Foo {
             *     int i:0;
             * }
             *
             * And bitfields without a name.
             * (padding is computed automatically)
             */
            if (fc.isBitField() && (fc.getBitFieldWidth() == 0 || fc.spelling().isEmpty())) {
                startBitfield();
                continue;
            }

            processField(fc);
        }

        return finishRecord(anonName);
    }

    abstract void startBitfield();
    abstract void processField(Cursor c);
    abstract org.openjdk.jextract.Type.Declared finishRecord(String anonName);

    void addField(Declaration declaration) {
        fieldDecls.add(declaration);
        MemoryLayout layout = null;
        if (declaration instanceof Declaration.Scoped scoped) {
            layout = scoped.layout().orElse(null);
        } else if (declaration instanceof Declaration.Variable var) {
            layout = var.layout().orElse(null);
        }
        if (layout != null) {
            //fieldLayouts.add(layout.name().isEmpty() ? layout.withName(declaration.name()) : layout);
            fieldLayouts.add(declaration.name().isEmpty() ? layout : layout.withName(declaration.name()));
        }
    }

    void addPadding(long bits) {
        fieldLayouts.add(MemoryLayout.paddingLayout(bits));
    }

    void addField(long offset, Type parent, Cursor c) {
        if (c.isAnonymousStruct()) {
            addField(((org.openjdk.jextract.Type.Declared)computeAnonymous(typeMaker, offset, parent, c.type(), nextAnonymousName())).tree());
        } else {
            addField(field(c));
        }
    }

    private String nextAnonymousName() {
        return "$anon$" + anonCount++;
    }

    Declaration field(Cursor c) {
        org.openjdk.jextract.Type type = typeMaker.makeType(c.type());
        String name = c.spelling();
        if (c.isBitField()) {
            MemoryLayout sublayout = MemoryLayout.paddingLayout(c.getBitFieldWidth());
            return Declaration.bitfield(TreeMaker.CursorPosition.of(c), name, type, sublayout.withName(name));
        } else if (c.isAnonymousStruct() && type instanceof org.openjdk.jextract.Type.Declared decl) {
            return decl.tree();
        } else {
            return Declaration.field(TreeMaker.CursorPosition.of(c), name, type);
        }
    }

    long fieldSize(Cursor c) {
        if (c.type().kind() == TypeKind.IncompleteArray) {
            return 0;
        }
        return c.isBitField() ? c.getBitFieldWidth() : c.type().size() * 8;
    }

    Declaration.Scoped bitfield(List<MemoryLayout> sublayouts, Declaration.Variable... declarations) {
        return Declaration.bitfields(declarations[0].pos(), MemoryLayout.structLayout(sublayouts.toArray(new MemoryLayout[0])), declarations);
    }

    long offsetOf(Type parent, Cursor c) {
        if (c.kind() == CursorKind.FieldDecl) {
            return parent.getOffsetOf(c.spelling());
        } else {
            return Utils.flattenableChildren(c)
                    .mapToLong(child -> offsetOf(parent, child))
                    .findFirst()
                    .orElseThrow(() -> new IllegalStateException(
                            "Can not find offset of: " + c + ", in: " + parent));
        }
    }
}
