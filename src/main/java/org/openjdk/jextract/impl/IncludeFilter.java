/*
 * Copyright (c) 2022, 2024 Oracle and/or its affiliates. All rights reserved.
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
import org.openjdk.jextract.Type;
import org.openjdk.jextract.Type.Delegated;
import org.openjdk.jextract.impl.DeclarationImpl.Skip;

/*
 * This visitor marks declarations to be skipped, based on --include options specified.
 */
public final class IncludeFilter implements Declaration.Visitor<Void, Declaration> {
    private final IncludeHelper includeHelper;
    private final Logger logger;

    public IncludeFilter(IncludeHelper includeHelper, Logger logger) {
        this.includeHelper = includeHelper;
        this.logger = logger;
    }

    public Declaration.Scoped scan(Declaration.Scoped header) {
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
            Skip.with(constant);
        }
        return null;
    }

    @Override
    public Void visitFunction(Declaration.Function funcTree, Declaration parent) {
        if (!includeHelper.isIncluded(funcTree)) {
            //skip
            Skip.with(funcTree);
        } else {
            warnMissingDep(funcTree, funcTree.type().returnType());
            funcTree.type().argumentTypes().forEach(p -> warnMissingDep(funcTree, p));
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
                Skip.with(d);
            }
        }

        d.members().forEach(fieldTree -> fieldTree.accept(this, d));
        return null;
    }

    @Override
    public Void visitTypedef(Declaration.Typedef tree, Declaration parent) {
        if (!includeHelper.isIncluded(tree)) {
            //skip
            Skip.with(tree);
        } else {
            warnMissingDep(tree, tree.type());
        }
        return null;
    }

    @Override
    public Void visitVariable(Declaration.Variable tree, Declaration parent) {
        if (parent == null && !includeHelper.isIncluded(tree)) {
            //skip
            Skip.with(tree);
        } else if (parent != null)  {
            if (!Skip.isPresent(parent)) {
                warnMissingDep(parent, tree.type());
            }
        } else {
            warnMissingDep(tree, tree.type());
        }
        return null;
    }

    @Override
    public Void visitDeclaration(Declaration decl, Declaration parent) {
        return null;
    }

    void warnMissingDep(Declaration decl, Type type) {
        if (type instanceof Type.Declared declared) {
            // we only have to check for missing structs because (a) pointers to missing structs can still lead
            // to valid code and (b) missing typedefs to existing structs are resolved correctly, as typedefs are never
            // referred to by name in the generated code (because of libclang limitations).
            if (Skip.isPresent(declared.tree())) {
                logger.err("jextract.bad.include", decl.name(), declared.tree().name());
            }
        } else if (type instanceof Type.Delegated delegated &&
                        delegated.kind() == Delegated.Kind.TYPEDEF) {
            warnMissingDep(decl, delegated.type());
        }
    }
}
