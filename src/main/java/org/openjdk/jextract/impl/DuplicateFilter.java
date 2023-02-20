/*
 * Copyright (c) 2022 Oracle and/or its affiliates. All rights reserved.
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
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/*
 * This visitor filters duplicate top-level variables, constants and functions.
 */
final class DuplicateFilter implements TreeTransformer, Declaration.Visitor<Void, Void> {
    // To detect duplicate Variable and Function declarations.
    private final Set<String> constants = new HashSet<>();
    private final Set<String> variables = new HashSet<>();
    private final Set<Declaration.Typedef> typedefs = new HashSet<>();
    private final Set<Declaration.Function> functions = new HashSet<>();
    private final List<Declaration> decls = new ArrayList<>();

    // have we seen this Constant earlier?
    private boolean constantSeen(Declaration.Constant tree) {
        return !constants.add(tree.name());
    }

    // have we seen this Variable earlier?
    private boolean variableSeen(Declaration.Variable tree) {
        return !variables.add(tree.name());
    }

    // have we seen this Function earlier?
    private boolean functionSeen(Declaration.Function tree) {
        return !functions.add(tree);
    }

    // have we seen this Function earlier?
    private boolean typedefSeen(Declaration.Typedef tree) {
        return !typedefs.add(tree);
    }

    DuplicateFilter() {
    }

    @Override
    public Declaration.Scoped transform(Declaration.Scoped header) {
        // Process all header declarations are collect potential
        // declarations that will go into transformed HeaderTree
        // into the this.decls field.
        header.members().forEach(fieldTree -> fieldTree.accept(this, null));
        return createHeader(header, decls);
    }

    @Override
    public Void visitConstant(Declaration.Constant constant, Void ignored) {
        if (constantSeen(constant)) {
            //skip
            return null;
        }

        decls.add(constant);
        return null;
    }

    @Override
    public Void visitFunction(Declaration.Function funcTree, Void ignored) {
        if (functionSeen(funcTree)) {
            //skip
            return null;
        }

        decls.add(funcTree);
        return null;
    }

    @Override
    public Void visitTypedef(Declaration.Typedef tree, Void ignored) {
        if (typedefSeen(tree)) {
            //skip
            return null;
        }

        decls.add(tree);
        return null;
    }

    @Override
    public Void visitVariable(Declaration.Variable tree, Void ignored) {
        if (variableSeen(tree)) {
            //skip
            return null;
        }

        decls.add(tree);
        return null;
    }

    @Override
    public Void visitDeclaration(Declaration decl, Void ignored) {
        decls.add(decl);
        return null;
    }
}
