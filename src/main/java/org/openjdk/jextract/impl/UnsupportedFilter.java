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
import org.openjdk.jextract.Declaration.Constant;
import org.openjdk.jextract.Declaration.Function;
import org.openjdk.jextract.Declaration.Scoped;
import org.openjdk.jextract.Declaration.Typedef;
import org.openjdk.jextract.Declaration.Variable;
import org.openjdk.jextract.Declaration.Visitor;
import org.openjdk.jextract.Type;
import org.openjdk.jextract.Type.Declared;
import org.openjdk.jextract.impl.DeclarationImpl.JavaName;
import org.openjdk.jextract.impl.DeclarationImpl.Skip;

import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.MemoryLayout;
import java.lang.foreign.ValueLayout;

public class UnsupportedFilter implements Declaration.Visitor<Void, Declaration> {
    static String firstUnsupportedType(Type type) {
        return type.accept(unsupportedVisitor, null);
    }

    public Declaration.Scoped scan(Declaration.Scoped header) {
        // Process all header declarations are collect potential
        // declarations that will go into transformed HeaderTree
        // into the this.decls field.
        header.members().forEach(fieldTree -> fieldTree.accept(this, null));
        return header;
    }

    @Override
    public Void visitFunction(Function funcTree, Declaration parent) {
        //generate static wrapper for function
        String unsupportedType = firstUnsupportedType(funcTree.type());
        if (unsupportedType != null) {
            warn("skipping " + funcTree.name() + " because of unsupported type usage: " +
                    unsupportedType);
            Skip.with(funcTree);
        } else {
            FunctionDescriptor descriptor = Type.descriptorFor(funcTree.type()).orElse(null);
            if (descriptor == null) {
                Skip.with(funcTree);
            }
        }

        // check function pointers in parameters and return types
        for (Declaration.Variable param : funcTree.parameters()) {
            Type.Function f = Utils.getAsFunctionPointer(param.type());
            if (f != null) {
                checkFunctionType(param, f);
            }
        }

        Type.Function returnFunc = Utils.getAsFunctionPointer(funcTree.type().returnType());
        if (returnFunc != null) {
            checkFunctionType(funcTree, returnFunc);
        }
        return null;
    }

    @Override
    public Void visitVariable(Variable varTree, Declaration parent) {
        String unsupportedType = firstUnsupportedType(varTree.type());
        if (unsupportedType != null) {
            String name = parent != null ? parent.name() + "." : "";
            name += varTree.name();
            warn("skipping " + name + " because of unsupported type usage: " +
                    unsupportedType);
            Skip.with(varTree);
        } else {
            MemoryLayout layout = Type.layoutFor(varTree.type()).orElse(null);
            if (layout == null) {
                //no layout - skip
                Skip.with(varTree);
            }

            if (varTree.kind() == Declaration.Variable.Kind.BITFIELD ||
                    (layout instanceof ValueLayout && layout.byteSize() > 8)) {
                //skip
                Skip.with(varTree);
            }
        }

        // check
        Type.Function func = Utils.getAsFunctionPointer(varTree.type());
        if (func != null) {
            checkFunctionType(varTree, func);
        }
        return null;
    }

    @Override
    public Void visitScoped(Scoped scoped, Declaration declaration) {
        if (scoped.layout().isEmpty()) {
            // skip
            Skip.with(scoped);
        }
        // propagate
        scoped.members().forEach(fieldTree -> {
            fieldTree.accept(this, scoped);
        });
        return null;
    }

    @Override
    public Void visitTypedef(Typedef typedefTree, Declaration declaration) {
        Type.Function func = Utils.getAsFunctionPointer(typedefTree.type());
        if (func != null) {
            checkFunctionType(typedefTree, func);
        }
        // propagate
        if (typedefTree.type() instanceof Declared declared) {
            visitScoped(declared.tree(), null);
        }
        return null;
    }

    @Override
    public Void visitConstant(Constant d, Declaration declaration) {
        return null;
    }

    @Override
    public Void visitDeclaration(Declaration d, Declaration declaration) {
        return null;
    }

    private void checkFunctionType(Declaration decl, Type.Function func) {
        String unsupportedType = firstUnsupportedType(func);
        if (unsupportedType != null) {
            warn("skipping " + JavaName.getOrThrow(decl) + " because of unsupported type usage: " +
                    unsupportedType);
            Skip.with(func);
        } else {
            FunctionDescriptor descriptor = Type.descriptorFor(func).orElse(null);
            if (descriptor == null) {
                Skip.with(func);
            }

            //generate functional interface
            if (func.varargs() && !func.argumentTypes().isEmpty()) {
                warn("varargs in callbacks is not supported: " + CDeclarationPrinter.declaration(func, JavaName.getOrThrow(decl)));
                Skip.with(func);
            }
        }
    }

    static Type.Visitor<String, Void> unsupportedVisitor = new Type.Visitor<>() {
        @Override
        public String visitPrimitive(Type.Primitive t, Void unused) {
            if (Skip.isPresent(t)) {
                return t.kind().layout().get().name().get();
            } else {
                return null;
            }
        }

        @Override
        public String visitFunction(Type.Function t, Void unused) {
            for (Type arg : t.argumentTypes()) {
                String unsupported = firstUnsupportedType(arg);
                if (unsupported != null) {
                    return unsupported;
                }
            }
            String unsupported = firstUnsupportedType(t.returnType());
            if (unsupported != null) {
                return unsupported;
            }
            return null;
        }

        @Override
        public String visitDeclared(Type.Declared t, Void unused) {
            for (Declaration d : t.tree().members()) {
                if (d instanceof Declaration.Variable variable) {
                    String unsupported = firstUnsupportedType(variable.type());
                    if (unsupported != null) {
                        return unsupported;
                    }
                }
            }
            return null;
        }

        @Override
        public String visitDelegated(Type.Delegated t, Void unused) {
            return t.kind() != Type.Delegated.Kind.POINTER ?
                    firstUnsupportedType(t.type()) :
                    null;
            //in principle we should always do this:
            // return firstUnsupportedType(t.type());
            // but if we do that, we might end up with infinite recursion (because of pointer types).
            // Unsupported pointer types (e.g. *long double) are not detected, but they are not problematic layout-wise
            // (e.g. they are always 32- or 64-bits, depending on the platform).
        }

        @Override
        public String visitArray(Type.Array t, Void unused) {
            return firstUnsupportedType(t.elementType());
        }

        @Override
        public String visitType(Type t, Void unused) {
            return null;
        }
    };

    static void warn(String msg) {
        System.err.println("WARNING: " + msg);
    }
}
