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
import org.openjdk.jextract.clang.TypeKind;

import java.util.List;

/**
 * MemoryLayout computer for C unions.
 */
final class UnionLayoutComputer extends RecordLayoutComputer {
    private final long offset;
    private long actualSize = 0L;

    UnionLayoutComputer(TypeMaker typeMaker, long offsetInParent, Type parent, Type type) {
        super(typeMaker, parent, type);
        this.offset = offsetInParent;
    }

    @Override
    void processField(Cursor c) {
        long expectedOffset = offsetOf(parent, c);
        if (expectedOffset > offset) {
            throw new IllegalStateException("No padding in union elements!");
        }

        addField(offset, parent, c);
        actualSize = Math.max(actualSize, fieldSize(c));
    }

    @Override
    void startBitfield() {
        // do nothing
    }

    @Override
    Declaration field(long offset, Cursor c) {
        if (c.isBitField()) {
            Declaration.Variable var = (Declaration.Variable)super.field(offset, c);
            return bitfield(var);
        } else {
            return super.field(offset, c);
        }
    }

    @Override
    long fieldSize(Cursor c) {
        if (c.type().kind() == TypeKind.IncompleteArray) {
            return 0;
        } else if (c.isBitField()) {
            return c.getBitFieldWidth();
        } else {
            return c.type().size() * 8;
        }
    }

    @Override
    Declaration.Scoped finishRecord(String layoutName, String declName) {
        // size mismatch indicates use of bitfields in union
        long expectedSize = type.size() * 8;
        if (actualSize < expectedSize) {
            // emit an extra padding of expected size to make sure union layout size is computed correctly
            addPadding(expectedSize);
        }

        GroupLayout g = MemoryLayout.unionLayout(alignFields());
        checkSize(g);
        g = g.withName(layoutName);
        return Declaration.union(TreeMaker.CursorPosition.of(cursor), declName, g, fieldDecls.stream().toArray(Declaration[]::new));
    }
}
