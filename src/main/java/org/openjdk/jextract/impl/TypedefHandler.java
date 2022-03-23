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

import org.openjdk.jextract.Declaration;
import org.openjdk.jextract.clang.Cursor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * This visitor handles certain typedef declarations.
 *
 * 1. Remove redundant typedefs.
 * 2. Rename typedef'ed anonymous type definitions like
 *        typedef struct { int x; int y; } Point;
 */
final class TypedefHandler implements Declaration.Visitor<Void, Void> {

    TreeMaker maker;

    public TypedefHandler(TreeMaker maker) {
        this.maker = maker;
    }

    // Potential Tree instances that will go into transformed HeaderTree
    // are collected in this list.
    private List<Declaration> decls = new ArrayList<>();

    // Tree instances that are to be replaced from "decls" list are
    // saved in the following Map. One or more Trees can replace a Tree.
    private final Map<Cursor, List<Declaration>> replacements = new HashMap<>();

    public Declaration.Scoped transform(Declaration.Scoped ht) {
        // Process all header declarations are collect potential
        // declarations that will go into transformed HeaderTree
        // into the this.decls field.
        ht.accept(this, null);

//        // Replace trees from this.decls with Trees found in this.replacements.
//        // We need this two step process so that named StructTree instances
//        // will replace with original unnamed StructTree instances.
//        List<Declaration> newDecls = decls.stream().flatMap(tx -> {
//            if (replacements.containsKey(tx.cursor())) {
//                return replacements.get(tx.cursor()).stream();
//            } else {
//                return Stream.of(tx);
//            }
//        }).collect(Collectors.toList());
//
//        return treeMaker.createHeader(ht.cursor(), ht.path(), newDecls);
        return ht;
    }

    @Override
    public Void visitDeclaration(Declaration d, Void aVoid) {
        decls.add(d);
        return null;
    }

    @Override
    public Void visitScoped(Declaration.Scoped ht, Void v) {
        ht.members().forEach(decl -> decl.accept(this, null));
        return null;
    }
}
