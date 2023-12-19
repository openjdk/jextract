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
import org.openjdk.jextract.Declaration.Constant;
import org.openjdk.jextract.impl.DeclarationImpl.EnumConstant;
import org.openjdk.jextract.impl.DeclarationImpl.Skip;

/*
 * This visitor lifts enum constants to the top level and removes enum Trees.
 */
final class EnumConstantLifter implements Declaration.Visitor<Void, Void> {

    public Declaration.Scoped scan(Declaration.Scoped header) {
        // Process all header declarations are collect potential
        // declarations that will go into transformed HeaderTree
        // into the this.decls field.
        header.members().forEach(fieldTree -> fieldTree.accept(this, null));
        return header;
    }

    @Override
    public Void visitScoped(Declaration.Scoped scoped, Void ignored) {
        if (Utils.isEnum(scoped)) {
            // add the name of the enum as an attribute.
            scoped.members().forEach(fieldTree -> {
                EnumConstant.with((Constant)fieldTree, scoped.name());
                fieldTree.accept(this, null);
            });
        }
        return null;
    }

    @Override
    public Void visitTypedef(Declaration.Typedef tree, Void ignored) {
        Utils.forEachNested(tree, d -> {
            if (Utils.isEnum(d)) {
                // no need to do anything for a typedef enum, as the IR always
                // lifts the enum tree before the typedef.
                Skip.with(tree);
            }
        });
        return null;
    }

    @Override
    public Void visitDeclaration(Declaration decl, Void ignored) {
        return null;
    }
}
