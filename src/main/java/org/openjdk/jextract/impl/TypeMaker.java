/*
 *  Copyright (c) 2020, 2023, Oracle and/or its affiliates. All rights reserved.
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

import java.util.ArrayList;
import java.util.List;

import org.openjdk.jextract.Declaration;
import org.openjdk.jextract.Declaration.Scoped;
import org.openjdk.jextract.Declaration.Typedef;
import org.openjdk.jextract.Position;
import org.openjdk.jextract.Type;
import org.openjdk.jextract.Type.Delegated;
import org.openjdk.jextract.Type.Primitive;
import org.openjdk.jextract.clang.TypeKind;
import org.openjdk.jextract.impl.TreeMaker.CursorPosition;

/**
 * This class turns a clang type into a jextract type. Unlike declarations, jextract types are not
 * de-duplicated, so relying on the identity of a jextract type is wrong. Since pointer types can
 * point back to declarations, we create special pointer types backed by a supplier which fetches
 * the correct declaration from the tree maker cache. This makes sure that situations with
 * mutually referring pointers are dealt with correctly (i.e. by breaking cycles).
 */
class TypeMaker {

    static Type makeType(org.openjdk.jextract.clang.Type t, TreeMaker treeMaker) {
        switch(t.kind()) {
            case Auto:
                return makeType(t.canonicalType(), treeMaker);
            case Void:
                return Type.void_();
            case Char_S:
            case Char_U:
                return Type.primitive(Primitive.Kind.Char);
            case Short:
                return Type.primitive(Primitive.Kind.Short);
            case Int:
                return Type.primitive(Primitive.Kind.Int);
            case Long:
                return Type.primitive(Primitive.Kind.Long);
            case LongLong:
                return Type.primitive(Primitive.Kind.LongLong);
            case SChar: {
                Type chType = Type.primitive(Primitive.Kind.Char);
                return Type.qualified(Delegated.Kind.SIGNED, chType);
            }
            case UShort: {
                Type chType = Type.primitive(Primitive.Kind.Short);
                return Type.qualified(Delegated.Kind.UNSIGNED, chType);
            }
            case UInt: {
                Type chType = Type.primitive(Primitive.Kind.Int);
                return Type.qualified(Delegated.Kind.UNSIGNED, chType);
            }
            case ULong: {
                Type chType = Type.primitive(Primitive.Kind.Long);
                return Type.qualified(Delegated.Kind.UNSIGNED, chType);
            }
            case ULongLong: {
                Type chType = Type.primitive(Primitive.Kind.LongLong);
                return Type.qualified(Delegated.Kind.UNSIGNED, chType);
            }
            case UChar: {
                Type chType = Type.primitive(Primitive.Kind.Char);
                return Type.qualified(Delegated.Kind.UNSIGNED, chType);
            }

            case Bool:
                return Type.primitive(Primitive.Kind.Bool);
            case Double:
                return Type.primitive(Primitive.Kind.Double);
            case Float:
                return Type.primitive(Primitive.Kind.Float);
            case Unexposed:
            case Elaborated:
                org.openjdk.jextract.clang.Type canonical = t.canonicalType();
                if (canonical.equalType(t)) {
                    return Type.error(t.spelling());
                }
                return makeType(canonical, treeMaker);
            case ConstantArray: {
                Type elem = makeType(t.getElementType(), treeMaker);
                return Type.array(t.getNumberOfElements(), elem);
            }
            case IncompleteArray: {
                Type elem = makeType(t.getElementType(), treeMaker);
                return Type.array(elem);
            }
            case FunctionProto:
            case FunctionNoProto: {
                List<Type> args = new ArrayList<>();
                for (int i = 0; i < t.numberOfArgs(); i++) {
                    // argument could be function pointer declared locally
                    args.add(lowerFunctionType(t.argType(i), treeMaker));
                }
                return Type.function(t.isVariadic(), lowerFunctionType(t.resultType(), treeMaker), args.toArray(new Type[0]));
            }
            case Enum:
            case Record: {
                Declaration d = treeMaker.createTree(t.getDeclarationCursor());
                return d != null ?
                        Type.declared((Declaration.Scoped)d) : Type.error(t.spelling());
            }
            case BlockPointer:
            case Pointer: {
                org.openjdk.jextract.clang.Type pointee = t.getPointeeType();
                if (pointee.kind() == TypeKind.FunctionProto ||
                        pointee.getDeclarationCursor().isInvalid()) {
                    return Type.pointer(makeType(t.getPointeeType(), treeMaker));
                } else {
                    // struct/union pointer - defer processing of pointee type
                    Position pos = CursorPosition.of(pointee.getDeclarationCursor());
                    return Type.pointer(() -> {
                        Declaration decl = treeMaker.lookup(pos).orElseThrow(IllegalStateException::new);
                        return switch (decl) {
                            case Scoped scoped -> Type.declared(scoped);
                            case Typedef typedef -> Type.typedef(typedef.name(), typedef.type());
                            default -> throw new UnsupportedOperationException();
                        };
                    });
                }
            }
            case Typedef: {
                Type __type = makeType(t.canonicalType(), treeMaker);
                return Type.typedef(t.spelling(), __type);
            }
            case Complex: {
                Type __type = makeType(t.getElementType(), treeMaker);
                return Type.qualified(Delegated.Kind.COMPLEX, __type);
            }
            case Vector: {
                Type __type = makeType(t.getElementType(), treeMaker);
                return Type.vector(t.getNumberOfElements(), __type);
            }
            case WChar: //unsupported
                return Type.primitive(Primitive.Kind.WChar);
            case Char16: //unsupported
                return Type.primitive(Primitive.Kind.Char16);
            case Half: //unsupported
                return Type.primitive(Primitive.Kind.HalfFloat);
            case Int128: //unsupported
                return Type.primitive(Primitive.Kind.Int128);
            case LongDouble: //unsupported
                return Type.primitive(Primitive.Kind.LongDouble);
            case UInt128: { //unsupported
                Type iType = Type.primitive(Primitive.Kind.Int128);
                return Type.qualified(Delegated.Kind.UNSIGNED, iType);
            }
            case Atomic: {
                Type aType = makeType(t.getValueType(), treeMaker);
                return Type.qualified(Delegated.Kind.ATOMIC, aType);
            }
            default:
                return Type.error(t.spelling());
        }
    }

    private static Type lowerFunctionType(org.openjdk.jextract.clang.Type t, TreeMaker treeMaker) {
        Type t2 = makeType(t, treeMaker);
        return t2.accept(lowerFunctionType, null);
    }

    private static final Type.Visitor<Type, Void> lowerFunctionType = new Type.Visitor<>() {
        @Override
        public Type visitArray(Type.Array t, Void aVoid) {
            return Type.pointer(t.elementType());
        }

        @Override
        public Type visitDelegated(Type.Delegated t, Void aVoid) {
            if (t.kind() == Delegated.Kind.TYPEDEF && t.type() instanceof Type.Array array) {
                return visitArray(array, aVoid);
            }
            return visitType(t, aVoid);
        }

        @Override
        public Type visitType(Type t, Void aVoid) {
            return t;
        }
    };
}
