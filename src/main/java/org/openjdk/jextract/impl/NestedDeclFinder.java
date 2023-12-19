/*
 * Copyright (c) 2023 Oracle and/or its affiliates. All rights reserved.
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
import org.openjdk.jextract.Type;
import org.openjdk.jextract.Type.Array;
import org.openjdk.jextract.Type.Declared;
import org.openjdk.jextract.Type.Delegated;
import org.openjdk.jextract.Type.Function;
import org.openjdk.jextract.Type.Visitor;
import org.openjdk.jextract.impl.DeclarationImpl.NestedDecl;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;

/*
 * This visitor searches for scoped declarations that appear indirectly in the toplevel tree, as part of some
 * declared type, and marks them with the NestedDecl attribute.
 */
final class NestedDeclFinder implements Declaration.Visitor<Void, Void> {

    private final Set<Declaration> seen = Collections.newSetFromMap(new IdentityHashMap<>());
    private final Set<Declaration> pending = Collections.newSetFromMap(new IdentityHashMap<>());

    public Declaration.Scoped scan(Declaration.Scoped header) {
        // Initial scan. After this step:
        // (a) "seen" contains declarations directly nested in any of the visited members
        // (b) "pending" contains declarations that are indirectly (e.g. via a type) nested in any of the visited members
        header.members().forEach(fieldTree -> fieldTree.accept(this, null));
        while (!pending.isEmpty()) {
            // remove directly nested declaration from "pending" (as these are not true nested declarations)
            pending.removeAll(seen);
            // for each remaining nested declaration in the to do list:
            // (a) mark it with NestedDecl attribute
            // (b) propagate the visit (this results in further updates to "seen" and "pending", as described above)
            for (Declaration d : Set.copyOf(pending)) {
                NestedDecl.with(d);
                d.accept(this, null);
            }
        }

        return header;
    }

    @Override
    public Void visitFunction(Declaration.Function funcTree, Void ignored) {
        funcTree.type().accept(nestedDeclarationTypeVisitor, null);
        return null;
    }

    @Override
    public Void visitTypedef(Declaration.Typedef tree, Void ignored) {
        tree.type().accept(nestedDeclarationTypeVisitor, null);
        return null;
    }

    @Override
    public Void visitVariable(Declaration.Variable tree, Void ignored) {
        tree.type().accept(nestedDeclarationTypeVisitor, null);
        return null;
    }

    @Override
    public Void visitScoped(Scoped d, Void unused) {
        seen.add(d);
        // propagate
        d.members().forEach(m -> m.accept(this, null));
        return null;
    }

    @Override
    public Void visitDeclaration(Declaration decl, Void ignored) {
        return null;
    }

    Type.Visitor<Void, Void> nestedDeclarationTypeVisitor = new Visitor<Void, Void>() {
        @Override
        public Void visitArray(Array t, Void unused) {
            t.elementType().accept(this, null);
            return null;
        }

        @Override
        public Void visitDelegated(Delegated t, Void unused) {
            t.type().accept(this, null);
            return null;
        }

        @Override
        public Void visitDeclared(Declared t, Void unused) {
            pending.add(t.tree());
            return null;
        }

        @Override
        public Void visitFunction(Function t, Void unused) {
            t.returnType().accept(this, null);
            t.argumentTypes().forEach(a -> a.accept(this, null));
            return null;
        }

        @Override
        public Void visitType(Type t, Void unused) {
            return null;
        }
    };
}
