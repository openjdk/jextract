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

import java.lang.foreign.GroupLayout;
import java.lang.foreign.MemoryLayout;
import org.openjdk.jextract.Declaration;
import org.openjdk.jextract.clang.Cursor;
import org.openjdk.jextract.clang.Type;

import java.util.ArrayList;
import java.util.List;

/**
 * MemoryLayout computer for C structs.
 */
final class StructLayoutComputer extends RecordLayoutComputer {
    private long offset;
    private long actualSize = 0L;
    // List to collect bitfield fields to process later, may be null
    private List<Declaration> bitfieldDecls;
    private long bitfieldSize;

    StructLayoutComputer(TypeMaker typeMaker, long offsetInParent, Type parent, Type type) {
        super(typeMaker, parent, type);
        this.offset = offsetInParent;
    }

    @Override
    void addField(long offset, Declaration declaration) {
        if (bitfieldDecls != null) {
            bitfieldDecls.add(declaration);
            bitfieldSize += ((Declaration.Bitfield)declaration).width();
        } else {
            super.addField(offset, declaration);
        }
    }

    @Override
    void addPadding(long bits) {
        if (bitfieldDecls == null) {
            super.addPadding(bits);
        } else {
            bitfieldSize += bits;
        }
    }

    @Override
    void startBitfield() {
        /*
         * In a struct, a bitfield field is seen after a non-bitfield.
         * Initialize bitfieldLayouts list to collect this and subsequent
         * bitfield layouts.
         */
        if (bitfieldDecls == null) {
            bitfieldDecls = new ArrayList<>();
            bitfieldSize = 0;
        }
    }

    @Override
    void processField(Cursor c) {
        boolean isBitfield = c.isBitField();
        long expectedOffset = offsetOf(parent, c);
        if (offset > expectedOffset) {
            // out-of-order field, skip
            System.err.println("WARNING: ignoring field: " + c.spelling() + " in struct " + type.spelling());
            return;
        }
        if (expectedOffset > offset) {
            addPadding(expectedOffset - offset);
            actualSize += (expectedOffset - offset);
            offset = expectedOffset;
        }

        if (isBitfield) {
            startBitfield();
        } else { // !isBitfield
            /*
             * We may be crossing from bit fields to non-bitfield field.
             *
             * struct Foo {
             *     int i:12;
             *     int j:20;
             *     int k; // <-- processing this
             *     int m;
             * }
             */
            handleBitfields();
        }

        addField(offset, parent, c);
        long size = fieldSize(c);
        offset += size;
        actualSize += size;
    }

    @Override
    Declaration.Scoped finishRecord(String layoutName, String declName) {
        // pad at the end, if any
        long expectedSize = type.size() * 8;
        if (actualSize < expectedSize) {
            addPadding(expectedSize - actualSize);
        }

        /*
         * Handle bitfields at the end, if any.
         *
         * struct Foo {
         *     int i,j, k;
         *     int f:10;
         *     int pad:12;
         * }
         */
        handleBitfields();

        GroupLayout g = MemoryLayout.structLayout(alignFields());
        checkSize(g);
        g = g.withName(layoutName);
        Declaration.Scoped declaration = Declaration.struct(TreeMaker.CursorPosition.of(cursor), declName,
                g, fieldDecls.stream().toArray(Declaration[]::new));
        return declaration;
    }

    // process bitfields if any and clear bitfield layouts
    private void handleBitfields() {
        if (bitfieldDecls != null) {
            List<Declaration> prevBitfieldDecls = bitfieldDecls;
            long prevBitfieldSize = bitfieldSize;
            bitfieldDecls = null;
            bitfieldSize = 0;
            if (!prevBitfieldDecls.isEmpty()) {
                addField(offset, bitfield(prevBitfieldDecls.toArray(new Declaration.Variable[0])));
            }
            if (prevBitfieldSize > 0) {
                if (prevBitfieldSize % 8 != 0) {
                    throw new IllegalStateException("Cannot get here: " + prevBitfieldSize);
                }
                fieldLayouts.add(MemoryLayout.paddingLayout(prevBitfieldSize / 8));
            }
        }
    }
}
