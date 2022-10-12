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
import org.openjdk.jextract.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/*
 * This visitor lifts enum constants to the top level and removes enum Trees.
 */
final class EnumConstantLifter implements TreeTransformer, Declaration.Visitor<Void, Void> {
    private static final String ENUM_NAME = "enum-name";

    private final List<Declaration> decls = new ArrayList<>();
    EnumConstantLifter() {
    }

    static Optional<String> enumName(Declaration.Constant constant) {
        return constant.getAttribute(ENUM_NAME).map(attrs -> attrs.get(0).toString());
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
    public Void visitScoped(Declaration.Scoped scoped, Void ignored) {
        if (liftEnumConstants(scoped)) {
            return null;
        }
        decls.add(scoped);
        return null;
    }

    @Override
    public Void visitTypedef(Declaration.Typedef tree, Void ignored) {
        Type type = tree.type();
        if (type instanceof Type.Declared declared) {
            if (liftEnumConstants(declared.tree())) {
                return null;
            }
        }
        decls.add(tree);
        return null;
    }

    @Override
    public Void visitDeclaration(Declaration decl, Void ignored) {
        decls.add(decl);
        return null;
    }

    private boolean liftEnumConstants(Declaration.Scoped scoped) {
        boolean isEnum = scoped.kind() == Declaration.Scoped.Kind.ENUM;
        if (isEnum) {
            // add the name of the enum as an attribute.
            scoped.members().forEach(fieldTree -> fieldTree
                .withAttribute(ENUM_NAME, scoped.name())
                .accept(this, null));
        }
        return isEnum;
    }
}
