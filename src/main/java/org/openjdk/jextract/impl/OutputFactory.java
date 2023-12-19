/*
 * Copyright (c) 2020, 2022 Oracle and/or its affiliates. All rights reserved.
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
import org.openjdk.jextract.Declaration.Variable.Kind;
import org.openjdk.jextract.Type;
import org.openjdk.jextract.impl.DeclarationImpl.JavaFunctionalInterfaceName;
import org.openjdk.jextract.impl.DeclarationImpl.JavaName;
import org.openjdk.jextract.impl.DeclarationImpl.Skip;

import javax.tools.JavaFileObject;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/*
 * Scan a header file and generate Java source items for entities defined in that header
 * file. Tree visitor visit methods return true/false depending on whether a
 * particular Tree is processed or skipped.
 */
public class OutputFactory implements Declaration.Visitor<Void, Declaration> {
    protected final ToplevelBuilder toplevelBuilder;
    protected Builder currentBuilder;

    static JavaFileObject[] generateWrapped(Declaration.Scoped decl,
                String pkgName, List<String> libraryNames) {
        String clsName = JavaName.getOrThrow(decl);
        ToplevelBuilder toplevelBuilder = new ToplevelBuilder(pkgName, clsName, libraryNames);
        return new OutputFactory(toplevelBuilder).generate(decl);
    }

    private OutputFactory(ToplevelBuilder toplevelBuilder) {
        this.toplevelBuilder = toplevelBuilder;
        this.currentBuilder = toplevelBuilder;
    }

    JavaFileObject[] generate(Declaration.Scoped decl) {
        //generate all decls
        decl.members().forEach(this::generateDecl);
        List<JavaFileObject> files = new ArrayList<>(toplevelBuilder.toFiles());
        return files.toArray(new JavaFileObject[0]);
    }

    private void generateDecl(Declaration tree) {
        try {
            tree.accept(this, null);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    @Override
    public Void visitConstant(Declaration.Constant constant, Declaration parent) {
        if (Skip.isPresent(constant)) {
            return null;
        }

        toplevelBuilder.addConstant(constant);
        return null;
    }

    @Override
    public Void visitScoped(Declaration.Scoped d, Declaration parent) {
        if (Skip.isPresent(d)) {
            return null;
        }

        Skip.with(d); // do not generate twice
        boolean isStructKind = Utils.isStructOrUnion(d);
        Builder prevBuilder = null;
        StructBuilder structBuilder = null;
        if (isStructKind) {
            prevBuilder = currentBuilder;
            currentBuilder = structBuilder = currentBuilder.addStruct(d);
        }
        try {
            d.members().forEach(fieldTree -> fieldTree.accept(this, d));
        } finally {
            if (isStructKind) {
                structBuilder.end();
                currentBuilder = prevBuilder;
            }
        }
        return null;
    }

    private void generateFunctionalInterface(String name, Type.Function func) {
        currentBuilder.addFunctionalInterface(name, func);
    }

    @Override
    public Void visitFunction(Declaration.Function funcTree, Declaration parent) {
        if (Skip.isPresent(funcTree)) {
            return null;
        }

        // check for function pointer type arguments
        for (Declaration.Variable param : funcTree.parameters()) {
            Type.Function f = Utils.getAsFunctionPointer(param.type());
            if (f != null) {
                generateFunctionalInterface(JavaFunctionalInterfaceName.getOrThrow(param), f);
            }
        }

        // return type could be a function pointer type
        Type.Function returnFunc = Utils.getAsFunctionPointer(funcTree.type().returnType());
        if (returnFunc != null) {
             generateFunctionalInterface(JavaFunctionalInterfaceName.getOrThrow(funcTree), returnFunc);
        }

        toplevelBuilder.addFunction(funcTree);
        return null;
    }

    @Override
    public Void visitTypedef(Declaration.Typedef tree, Declaration parent) {
        if (Skip.isPresent(tree)) {
            return null;
        }
        Type type = tree.type();
        Utils.forEachNested(tree, s -> s.accept(this, null));

        Declaration.Scoped structOrUnionDecl = Utils.structOrUnionDecl(type);
        if (structOrUnionDecl != null) {
            toplevelBuilder.addTypedef(tree, JavaName.getFullNameOrThrow(structOrUnionDecl));
        } else if (type instanceof Type.Primitive) {
            toplevelBuilder.addTypedef(tree, null);
        } else {
            Type.Function func = Utils.getAsFunctionPointer(type);
            if (func != null) {
                generateFunctionalInterface(JavaFunctionalInterfaceName.getOrThrow(tree), func);
            } else if (((TypeImpl)type).isPointer()) {
                toplevelBuilder.addTypedef(tree, null);
            } else {
                Type.Primitive primitive = Utils.getAsSignedOrUnsigned(type);
                if (primitive != null) {
                    toplevelBuilder.addTypedef(tree, null, primitive);
                }
            }
        }
        return null;
    }

    @Override
    public Void visitVariable(Declaration.Variable tree, Declaration parent) {
        if (Skip.isPresent(tree)) {
            return null;
        }
        Type type = tree.type();

        Utils.forEachNested(tree, s -> s.accept(this, tree));

        final String fieldName = tree.name();
        assert !fieldName.isEmpty();

        Optional<String> fiName = JavaFunctionalInterfaceName.get(tree);
        Type.Function func = Utils.getAsFunctionPointer(type);
        if (func != null) {
            generateFunctionalInterface(fiName.get(), func);
        }

        if (tree.kind() == Kind.GLOBAL || tree.kind() == Kind.FIELD) {
            currentBuilder.addVar(tree, fiName);
        }
        return null;
    }

    interface Builder {

        default void addVar(Declaration.Variable varTree, Optional<String> fiName) {
            throw new UnsupportedOperationException("Not implemented");
        }

        default void addFunction(Declaration.Function funcTree) {
            throw new UnsupportedOperationException("Not implemented");
        }

        default void addConstant(Declaration.Constant constantTree) {
            throw new UnsupportedOperationException("Not implemented");
        }

        default void addTypedef(Declaration.Typedef typedefTree, String superClass) {
            addTypedef(typedefTree, superClass, typedefTree.type());
        }

        default void addTypedef(Declaration.Typedef typedefTree, String superClass, Type type) {
            throw new UnsupportedOperationException("Not implemented");
        }

        default StructBuilder addStruct(Declaration.Scoped structTree) {
            throw new UnsupportedOperationException("Not implemented");
        }

        default void addFunctionalInterface(String name, Type.Function funcType) {
            throw new UnsupportedOperationException("Not implemented");
        }
    }
}
