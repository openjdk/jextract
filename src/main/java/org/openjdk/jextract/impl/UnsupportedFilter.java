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
import org.openjdk.jextract.Declaration.Scoped.Kind;
import org.openjdk.jextract.Declaration.Typedef;
import org.openjdk.jextract.Declaration.Variable;
import org.openjdk.jextract.Type;
import org.openjdk.jextract.Type.Declared;
import org.openjdk.jextract.impl.DeclarationImpl.AnonymousStruct;
import org.openjdk.jextract.impl.DeclarationImpl.ClangSizeOf;
import org.openjdk.jextract.impl.DeclarationImpl.JavaName;
import org.openjdk.jextract.impl.DeclarationImpl.Skip;

import java.io.PrintWriter;

/*
 * This visitor marks a number of unsupported construct so that they are skipped by code generation.
 * Unsupported constructs are:
 * - declarations containing an unsupported type (e.g. "long128")
 * - structs/unions.variables for which no layout exists
 * - functions/function pointer for which no descriptor exists
 * - variadic function pointers
 * - bitfields struct members
 * - anonymous struct whose first (possibly nested) member has unknown offset
 */
public class UnsupportedFilter implements Declaration.Visitor<Void, Declaration> {

    private final PrintWriter errStream;

    public UnsupportedFilter(PrintWriter errStream) {
        this.errStream = errStream;
    }

    static Type firstUnsupportedType(Type type, boolean allowVoid) {
        return type.accept(UNSUPPORTED_VISITOR, allowVoid);
    }

    public Declaration.Scoped scan(Declaration.Scoped header) {
        header.members().forEach(fieldTree -> fieldTree.accept(this, null));
        return header;
    }

    @Override
    public Void visitFunction(Function funcTree, Declaration firstNamedParent) {
        Utils.forEachNested(funcTree, s -> s.accept(this, firstNamedParent));

        //generate static wrapper for function
        Type unsupportedType = firstUnsupportedType(funcTree.type(), false);
        if (unsupportedType != null) {
            warnSkip(funcTree.name(), STR."unsupported type usage: \{unsupportedType}");
            Skip.with(funcTree);
            return null;
        }

        // check function pointers in parameters and return types
        for (Declaration.Variable param : funcTree.parameters()) {
            Utils.forEachNested(param, s -> s.accept(this, firstNamedParent));
            Type.Function f = Utils.getAsFunctionPointer(param.type());
            if (f != null && !checkFunctionTypeSupported(param, f, funcTree.name())) {
                Skip.with(funcTree);
                return null;
            }
        }

        Type.Function returnFunc = Utils.getAsFunctionPointer(funcTree.type().returnType());
        if (returnFunc != null && !checkFunctionTypeSupported(funcTree, returnFunc, funcTree.name())) {
            Skip.with(funcTree);
            return null;
        }
        return null;
    }

    private static String fieldName(Declaration firstNamedParent, Declaration decl) {
        String name = firstNamedParent != null ? firstNamedParent.name() + "." : "";
        name += decl.name();
        return name;
    }

    @Override
    public Void visitVariable(Variable varTree, Declaration firstNamedParent) {
        Utils.forEachNested(varTree, s -> s.accept(this, varTree));

        Type unsupportedType = firstUnsupportedType(varTree.type(), false);
        String name = fieldName(firstNamedParent, varTree);
        if (unsupportedType != null) {
            warnSkip(name, STR."unsupported type usage: \{unsupportedType}");
            Skip.with(varTree);
            return null;
        }

        // check
        Type.Function func = Utils.getAsFunctionPointer(varTree.type());
        if (func != null && !checkFunctionTypeSupported(varTree, func, name)) {
            Skip.with(varTree);
            return null;
        }
        return null;
    }

    @Override
    public Void visitScoped(Scoped scoped, Declaration firstNamedParent) {
        Type unsupportedType = firstUnsupportedType(Type.declared(scoped), false);
        if (unsupportedType != null) {
            warnSkip(scoped.name(), STR."unsupported type usage: \{unsupportedType}");
            Skip.with(scoped);
            return null;
        }

        if (scoped.kind() == Kind.BITFIELDS) {
            for (Declaration bitField : scoped.members()) {
                if (!bitField.name().isEmpty()) {
                    warnSkip(fieldName(firstNamedParent, bitField), "type is bitfield");
                }
            }
            Skip.with(scoped);
            return null;
        }

        // propagate
        Declaration newNamedParent = !scoped.name().isEmpty() ? scoped : firstNamedParent;
        scoped.members().forEach(fieldTree -> {
            fieldTree.accept(this, newNamedParent);
        });
        return null;
    }

    @Override
    public Void visitTypedef(Typedef typedefTree, Declaration firstNamedParent) {
        // propagate
        if (typedefTree.type() instanceof Declared declared) {
            visitScoped(declared.tree(), null);
        }

        Type unsupportedType = firstUnsupportedType(typedefTree.type(),false);
        if (unsupportedType != null) {
            warnSkip(typedefTree.name(), STR."unsupported type usage: \{unsupportedType}");
            Skip.with(typedefTree);
            return null;
        }

        Type.Function func = Utils.getAsFunctionPointer(typedefTree.type());
        if (func != null && !checkFunctionTypeSupported(typedefTree, func, typedefTree.name())) {
            Skip.with(typedefTree);
        }
        return null;
    }

    @Override
    public Void visitConstant(Constant d, Declaration firstNamedParent) {
        return null;
    }

    @Override
    public Void visitDeclaration(Declaration d, Declaration firstNamedParent) {
        return null;
    }

    private boolean checkFunctionTypeSupported(Declaration decl, Type.Function func, String nameOfSkipped) {
        Type unsupportedType = firstUnsupportedType(func, false);
        if (unsupportedType != null) {
            warnSkip(nameOfSkipped, STR."unsupported type usage: \{unsupportedType}");
            return false;
        }
        //generate functional interface
        if (func.varargs() && !func.argumentTypes().isEmpty()) {
            warnSkip(nameOfSkipped, "varargs in callbacks is not supported: " + decl.name());
            return false;
        }
        return true;
    }

    private static final Type.Visitor<Type, Boolean> UNSUPPORTED_VISITOR = new Type.Visitor<>() {
        @Override
        public Type visitPrimitive(Type.Primitive t, Boolean allowVoid) {
            return switch (t.kind()) {
                case Char16, Float128, HalfFloat, Int128, WChar -> t;
                case LongDouble -> TypeImpl.IS_WINDOWS ? null : t;
                case Void -> allowVoid ? null : t;
                default -> null;
            };
        }

        @Override
        public Type visitFunction(Type.Function t, Boolean allowVoid) {
            for (Type arg : t.argumentTypes()) {
                Type unsupported = firstUnsupportedType(arg, false);
                if (unsupported != null) {
                    return unsupported;
                }
            }
            Type unsupported = firstUnsupportedType(t.returnType(), true);
            if (unsupported != null) {
                return unsupported;
            }
            return null;
        }

        @Override
        public Type visitDeclared(Type.Declared t, Boolean allowVoid) {
            if (t.tree().kind() == Kind.STRUCT || t.tree().kind() == Kind.UNION) {
                if (!isValidStructOrUnion(t.tree())) {
                    return t;
                }
            }
            return null;
        }

        private boolean isValidStructOrUnion(Scoped scoped) {
            if (ClangSizeOf.get(scoped).isEmpty()) {
                return false;
            }
            if (AnonymousStruct.isPresent(scoped) &&
                    AnonymousStruct.getOrThrow(scoped).offset().isEmpty()) {
                return false;
            }
            return true;
        }

        @Override
        public Type visitDelegated(Type.Delegated t, Boolean allowVoid) {
            // Note: unsupported pointer types (e.g. *long double) are not detected, but they are not problematic
            // layout-wise (e.g. they are always 32- or 64-bits, depending on the platform). This policy also allows
            // more flexibility when it comes to opaque struct types.
            return t.kind() != Type.Delegated.Kind.POINTER ?
                    firstUnsupportedType(t.type(), allowVoid) :
                    null;
        }

        @Override
        public Type visitArray(Type.Array t, Boolean allowVoid) {
            return firstUnsupportedType(t.elementType(), false);
        }

        @Override
        public Type visitType(Type t, Boolean allowVoid) {
            return t.isErroneous() ?
                    t : null;
        }
    };

    private void warnSkip(String treeName, String message) {
        warn(STR."skipping \{treeName}: \{message}");
    }

    private void warn(String msg) {
        errStream.println("WARNING: " + msg);
    }
}
