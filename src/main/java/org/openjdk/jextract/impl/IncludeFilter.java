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
import org.openjdk.jextract.impl.DeclarationImpl.Skip;

/*
 * This visitor marks declarations to be skipped, based on --include options specified.
 */
final class IncludeFilter implements Declaration.Visitor<Void, Declaration> {
    private final IncludeHelper includeHelper;

    IncludeFilter(IncludeHelper includeHelper) {
        this.includeHelper = includeHelper;
    }

    public Declaration.Scoped process(Declaration.Scoped header) {
        // Process all header declarations are collect potential
        // declarations that will go into transformed HeaderTree
        // into the this.decls field.
        header.members().forEach(fieldTree -> fieldTree.accept(this, null));
        return header;
    }

    @Override
    public Void visitConstant(Declaration.Constant constant, Declaration parent) {
        if (!includeHelper.isIncluded(constant)) {
            //skip
            constant.addAttribute(new Skip());
        }
        return null;
    }

    @Override
    public Void visitFunction(Declaration.Function funcTree, Declaration parent) {
        if (!includeHelper.isIncluded(funcTree)) {
            //skip
            funcTree.addAttribute(new Skip());
        }
        return null;
    }

    @Override
    public Void visitScoped(Declaration.Scoped d, Declaration parent) {
        boolean isStructKind = Utils.isStructOrUnion(d);
        if (isStructKind) {
            String name = d.name();
            if (!name.isEmpty() && !includeHelper.isIncluded(d)) {
                //skip
                d.addAttribute(new Skip());
            }
        }

        d.members().forEach(fieldTree -> fieldTree.accept(this, d));
        return null;
    }

    @Override
    public Void visitTypedef(Declaration.Typedef tree, Declaration parent) {
        if (!includeHelper.isIncluded(tree)) {
            //skip
            tree.addAttribute(new Skip());
        }
        return null;
    }

    @Override
    public Void visitVariable(Declaration.Variable tree, Declaration parent) {
        if (parent == null && !includeHelper.isIncluded(tree)) {
            //skip
            tree.addAttribute(new Skip());
        }
        return null;
    }

    @Override
    public Void visitDeclaration(Declaration decl, Declaration parent) {
        return null;
    }
}
