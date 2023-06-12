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

import java.lang.foreign.AddressLayout;
import java.lang.foreign.GroupLayout;
import java.lang.foreign.MemoryLayout;
import org.openjdk.jextract.Declaration;
import org.openjdk.jextract.clang.Cursor;
import org.openjdk.jextract.clang.CursorKind;
import org.openjdk.jextract.clang.Type;
import org.openjdk.jextract.clang.TypeKind;

import java.lang.foreign.SequenceLayout;
import java.lang.foreign.StructLayout;
import java.lang.foreign.ValueLayout;
import java.util.ArrayList;
import java.util.List;

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
            Declaration.struct(TreeMaker.CursorPosition.NO_POSITION, "", MemoryLayout.paddingLayout(8)));

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
        cursor.forEach(fc -> {
            if (Utils.isFlattenable(fc)) {
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
                } else {
                    processField(fc);
                }
            }
        });

        String declName = recordName();
        Declaration.Scoped declaration = finishRecord(anonName != null ? anonName : declName, declName);
        if (cursor.isAnonymousStruct()) {
            // record this with a declaration attribute, so we don't have to rely on the cursor again later
            declaration = (Declaration.Scoped)declaration.withAttribute("ANONYMOUS", true);
        }
        return org.openjdk.jextract.Type.declared(declaration);
    }

    abstract void startBitfield();
    abstract void processField(Cursor c);
    abstract Declaration.Scoped finishRecord(String layoutName, String declName);

    void addField(long offset, Declaration declaration) {
        fieldDecls.add(declaration);
        MemoryLayout layout = null;
        if (declaration instanceof Declaration.Scoped scoped) {
            layout = scoped.layout().orElse(null);
        } else if (declaration instanceof Declaration.Variable var) {
            layout = org.openjdk.jextract.Type.layoutFor(var.type()).orElse(null);
        }
        if (layout != null) {
            fieldLayouts.add(declaration.name().isEmpty() ? layout : layout.withName(declaration.name()));
        }
    }

    void addPadding(long bits) {
        fieldLayouts.add(MemoryLayout.paddingLayout(bits / 8));
    }

    void addField(long offset, Type parent, Cursor c) {
        if (c.isAnonymousStruct()) {
            addField(offset, ((org.openjdk.jextract.Type.Declared)computeAnonymous(typeMaker, offset, parent, c.type(), nextAnonymousName())).tree());
        } else {
            addField(offset, field(offset, c));
        }
    }

    private String nextAnonymousName() {
        return "$anon$" + anonCount++;
    }

    Declaration field(long offset, Cursor c) {
        org.openjdk.jextract.Type type = typeMaker.makeType(c.type());
        String name = c.spelling();
        if (c.isBitField()) {
            return Declaration.bitfield(TreeMaker.CursorPosition.of(c), name, type, offset, c.getBitFieldWidth());
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

    Declaration.Scoped bitfield(Declaration.Variable... declarations) {
        return Declaration.bitfields(declarations[0].pos(), declarations);
    }

    long offsetOf(Type parent, Cursor c) {
        if (c.kind() == CursorKind.FieldDecl) {
            return parent.getOffsetOf(c.spelling());
        } else {
            List<Long> offsets = new ArrayList<>();
            c.forEach(child -> {
                if (Utils.isFlattenable(child)) {
                    offsets.add(offsetOf(parent, child));
                }
            });
            return offsets.stream().findFirst()
                    .orElseThrow(() -> new IllegalStateException(
                            "Can not find offset of: " + c + ", in: " + parent));
        }
    }

    void checkSize(GroupLayout layout) {
        // sanity check
        if (cursor.type().size() != layout.byteSize()) {
            throw new AssertionError(
                    String.format("Unexpected size for layout %s. Found %d ; expected %d",
                            layout, layout.byteSize(), cursor.type().size()));
        }
    }

    private String recordName() {
        if (cursor.isAnonymous()) {
            return "";
        } else {
            return cursor.spelling();
        }
    }

    MemoryLayout[] alignFields() {
        long align = cursor.type().align();
        return fieldLayouts.stream()
                .map(l -> forceAlign(l, align))
                .toArray(MemoryLayout[]::new);
    }

    private static MemoryLayout forceAlign(MemoryLayout layout, long align) {
        if (align >= layout.byteAlignment()) {
            return layout; // fast-path
        }
        MemoryLayout res = switch (layout) {
            case GroupLayout groupLayout -> {
                MemoryLayout[] newMembers = groupLayout.memberLayouts()
                        .stream().map(l -> forceAlign(l, align)).toArray(MemoryLayout[]::new);
                yield groupLayout instanceof StructLayout ?
                        MemoryLayout.structLayout(newMembers) :
                        MemoryLayout.unionLayout(newMembers);
            }
            case SequenceLayout sequenceLayout ->
                MemoryLayout.sequenceLayout(sequenceLayout.elementCount(),
                        forceAlign(sequenceLayout.elementLayout(), align));
            default -> layout.withByteAlignment(align);
        };
        // copy name and target layout, if present
        if (layout.name().isPresent()) {
            res = res.withName(layout.name().get());
        }
        if (layout instanceof AddressLayout addressLayout && addressLayout.targetLayout().isPresent()) {
            ((AddressLayout)res).withTargetLayout(addressLayout.targetLayout().get());
        }
        return res;
    }
}
