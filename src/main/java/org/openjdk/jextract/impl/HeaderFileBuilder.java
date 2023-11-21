/*
 * Copyright (c) 2020, 2023, Oracle and/or its affiliates. All rights reserved.
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

import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.GroupLayout;
import java.lang.foreign.MemoryLayout;
import java.lang.foreign.SegmentAllocator;
import java.lang.foreign.SequenceLayout;
import java.lang.foreign.ValueLayout;
import org.openjdk.jextract.Declaration;
import org.openjdk.jextract.Type;

import org.openjdk.jextract.impl.Constants.Constant;

import java.lang.invoke.MethodType;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * A helper class to generate header interface class in source form.
 * After aggregating various constituents of a .java source, build
 * method is called to get overall generated source string.
 */
class HeaderFileBuilder extends ClassSourceBuilder {

    static final String MEMBER_MODS = "public static";

    private final Constants constants;

    HeaderFileBuilder(SourceFileBuilder builder, Constants constants, String className, String superName) {
        super(builder, "public", Kind.CLASS, className, superName, List.of());
        this.constants = constants;
    }

    public void addVar(Declaration.Variable varTree, String javaName,
        MemoryLayout layout, Optional<String> fiName) {
        String nativeName = varTree.name();
        if (layout instanceof SequenceLayout || layout instanceof GroupLayout) {
            if (layout.byteSize() > 0) {
                constants.addSegment(nativeName, layout)
                        .emitGetterWithComment(this, MEMBER_MODS, javaName, nativeName, varTree);
            };
        } else if (layout instanceof ValueLayout valueLayout) {
            constants.addLayout(valueLayout)
                    .emitGetter(this, MEMBER_MODS, javaName);
            Constant vhConstant = constants.addGlobalVarHandle(valueLayout)
                    .emitGetter(this, MEMBER_MODS, javaName);
            Constant segmentConstant = constants.addSegment(nativeName, valueLayout)
                    .emitGetter(this, MEMBER_MODS, javaName, nativeName);
            emitGlobalGetter(segmentConstant, vhConstant, javaName, nativeName, valueLayout.carrier(), varTree, "Getter for variable:");
            emitGlobalSetter(segmentConstant, vhConstant, javaName, nativeName, valueLayout.carrier(), varTree, "Setter for variable:");

            if (fiName.isPresent()) {
                emitFunctionalInterfaceGetter(fiName.get(), javaName);
            }
        }
    }

    public void addFunction(Declaration.Function funcTree, FunctionDescriptor descriptor,
            String javaName, List<String> parameterNames) {
        String nativeName = funcTree.name();
        boolean isVarargs = funcTree.type().varargs();

        Constant mhConstant = constants.addDowncallMethodHandle(nativeName, descriptor, isVarargs)
                .emitGetter(this, MEMBER_MODS, javaName, nativeName);
        MethodType downcallType = descriptor.toMethodType();
        boolean needsAllocator = descriptor.returnLayout().isPresent() &&
                descriptor.returnLayout().get() instanceof GroupLayout;
        emitFunctionWrapper(mhConstant, javaName, nativeName, downcallType, needsAllocator, isVarargs, parameterNames, funcTree);
    }

    public void addConstant(Declaration.Constant constantTree, String javaName, Class<?> javaType) {
        Object value = constantTree.value();
        constants.addConstantDesc(javaType, value)
                    .emitGetterWithComment(this, MEMBER_MODS, c -> javaName, constantTree);
    }

    // private generation

    private void emitFunctionWrapper(Constant mhConstant, String javaName, String nativeName, MethodType declType,
                                     boolean needsAllocator, boolean isVarargs, List<String> parameterNames, Declaration decl) {
        incrAlign();
        emitDocComment(decl);
        indent();
        append(MEMBER_MODS + " ");
        if (needsAllocator) {
            // needs allocator parameter
            declType = declType.insertParameterTypes(0, SegmentAllocator.class);
            parameterNames = new ArrayList<>(parameterNames);
            parameterNames.add(0, "allocator");
        }
        List<String> pExprs = emitFunctionWrapperDecl(javaName, declType, isVarargs, parameterNames);
        append(" {\n");
        incrAlign();
        indent();
        append("var mh$ = ");
        append(mhConstant.getterName(javaName));
        append("();\n");
        indent();
        append("try {\n");
        incrAlign();
        indent();
        if (!declType.returnType().equals(void.class)) {
            append("return (" + declType.returnType().getName() + ")");
        }
        append("mh$.invokeExact(" + String.join(", ", pExprs) + ");\n");
        decrAlign();
        indent();
        append("} catch (Throwable ex$) {\n");
        incrAlign();
        indent();
        append("throw new AssertionError(\"should not reach here\", ex$);\n");
        decrAlign();
        indent();
        append("}\n");
        decrAlign();
        indent();
        append("}\n");
        decrAlign();
    }

    private List<String> emitFunctionWrapperDecl(String javaName, MethodType methodType, boolean isVarargs, List<String> paramNames) {
        append(methodType.returnType().getSimpleName() + " " + javaName + "(");
        String delim = "";
        List<String> pExprs = new ArrayList<>();
        final int numParams = paramNames.size();
        for (int i = 0 ; i < numParams; i++) {
            String pName = paramNames.get(i);
            if (pName.isEmpty()) {
                pName = "x" + i;
            }
            pExprs.add(pName);
            Class<?> pType = methodType.parameterType(i);
            append(delim + pType.getSimpleName() + " " + pName);
            delim = ", ";
        }
        if (isVarargs) {
            String lastArg = "x" + numParams;
            append(delim + "Object... " + lastArg);
            pExprs.add(lastArg);
        }
        append(")");
        return pExprs;
    }

    private void emitFunctionalInterfaceGetter(String fiName, String javaName) {
        incrAlign();
        indent();
        append(MEMBER_MODS + " ");
        append(fiName + " " + javaName + " () {\n");
        incrAlign();
        indent();
        append("return " + fiName + ".ofAddress(" + javaName + "$get(), Arena.global());\n");
        decrAlign();
        indent();
        append("}\n");
        decrAlign();
    }

    void emitPrimitiveTypedef(Type.Primitive primType, String name) {
        emitPrimitiveTypedef(null, primType, name);
    }

    void emitPrimitiveTypedef(Declaration.Typedef typedefTree, Type.Primitive primType, String name) {
        Type.Primitive.Kind kind = primType.kind();
        if (primitiveKindSupported(kind) && kind.layout().isPresent()) {
            incrAlign();
            if (typedefTree != null) {
                emitDocComment(typedefTree);
            }
            indent();
            append(MEMBER_MODS);
            append(" final");
            append(" " + Utils.layoutDeclarationType(primType.kind().layout().orElseThrow()).getSimpleName());
            append(" " + name);
            append(" = ");
            append(constants.addLayout(kind.layout().get()).accessExpression());
            append(";\n");
            decrAlign();
        }
    }

    void emitPointerTypedef(String name) {
        emitPointerTypedef(null, name);
    }

    void emitPointerTypedef(Declaration.Typedef typedefTree, String name) {
        incrAlign();
        if (typedefTree != null) {
            emitDocComment(typedefTree);
        }
        indent();
        append(MEMBER_MODS);
        append(" final");
        append(" AddressLayout ");
        append(name);
        append(" = ");
        append(constants.addLayout(TypeImpl.PointerImpl.POINTER_LAYOUT).accessExpression());
        append(";\n");
        decrAlign();
    }

    private boolean primitiveKindSupported(Type.Primitive.Kind kind) {
        return switch(kind) {
            case Short, Int, Long, LongLong, Float, Double, Char -> true;
            default -> false;
        };
    }

    private void emitGlobalGetter(Constant segmentConstant, Constant vhConstant, String javaName, String nativeName,
                                  Class<?> type, Declaration.Variable decl, String docHeader) {
        incrAlign();
        emitDocComment(decl, docHeader);
        indent();
        append(MEMBER_MODS + " " + type.getSimpleName() + " " + javaName + "$get() {\n");
        incrAlign();
        indent();
        append("return (" + type.getName() + ") ");
        append(vhConstant.accessExpression());
        append(".get(RuntimeHelper.requireNonNull(");
        append(segmentConstant.accessExpression());
        append(", \"");
        append(nativeName);
        append("\"), 0L);\n");
        decrAlign();
        indent();
        append("}\n");
        decrAlign();
    }

    private void emitGlobalSetter(Constant segmentConstant, Constant vhConstant, String javaName, String nativeName,
                                  Class<?> type, Declaration.Variable decl, String docHeader) {
        incrAlign();
        emitDocComment(decl, docHeader);
        indent();
        append(MEMBER_MODS + " void " + javaName + "$set(" + type.getSimpleName() + " x) {\n");
        incrAlign();
        indent();
        append(vhConstant.accessExpression());
        append(".set(RuntimeHelper.requireNonNull(");
        append(segmentConstant.accessExpression());
        append(", \"");
        append(nativeName);
        append("\"), 0L, x);\n");
        decrAlign();
        indent();
        append("}\n");
        decrAlign();
    }
}
