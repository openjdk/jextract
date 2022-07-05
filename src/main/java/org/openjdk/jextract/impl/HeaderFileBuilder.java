/*
 * Copyright (c) 2020, 2022, Oracle and/or its affiliates. All rights reserved.
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

import java.lang.foreign.Linker;
import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.GroupLayout;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.MemoryLayout;
import java.lang.foreign.SegmentAllocator;
import java.lang.foreign.SequenceLayout;
import java.lang.foreign.ValueLayout;
import org.openjdk.jextract.Type;

import org.openjdk.jextract.impl.ConstantBuilder.Constant;

import java.lang.invoke.MethodType;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * A helper class to generate header interface class in source form.
 * After aggregating various constituents of a .java source, build
 * method is called to get overall generated source string.
 */
abstract class HeaderFileBuilder extends ClassSourceBuilder {

    static final String MEMBER_MODS = "public static";

    private final String superclass;

    HeaderFileBuilder(ToplevelBuilder enclosing, String name, String superclass) {
        super(enclosing, Kind.CLASS, name);
        this.superclass = superclass;
    }

    @Override
    String superClass() {
        return superclass;
    }

    @Override
    public void addVar(String javaName, String nativeName, MemoryLayout layout, Optional<String> fiName) {
        if (layout instanceof SequenceLayout || layout instanceof GroupLayout) {
            emitWithConstantClass(constantBuilder -> {
                if (layout.byteSize() > 0) {
                    constantBuilder.addSegment(javaName, nativeName, layout)
                        .emitGetter(this, MEMBER_MODS, Constant.QUALIFIED_NAME, nativeName);
                }
            });
        } else if (layout instanceof ValueLayout valueLayout) {
            emitWithConstantClass(constantBuilder -> {
                constantBuilder.addLayout(javaName, valueLayout)
                        .emitGetter(this, MEMBER_MODS, Constant.QUALIFIED_NAME);
                Constant vhConstant = constantBuilder.addGlobalVarHandle(javaName, nativeName, valueLayout)
                        .emitGetter(this, MEMBER_MODS, Constant.QUALIFIED_NAME);
                Constant segmentConstant = constantBuilder.addSegment(javaName, nativeName, valueLayout)
                        .emitGetter(this, MEMBER_MODS, Constant.QUALIFIED_NAME, nativeName);
                emitGlobalGetter(segmentConstant, vhConstant, javaName, nativeName, valueLayout.carrier());
                emitGlobalSetter(segmentConstant, vhConstant, javaName, nativeName, valueLayout.carrier());

                if (fiName.isPresent()) {
                    emitFunctionalInterfaceGetter(fiName.get(), javaName);
                }
            });
        }
    }

    @Override
    public void addFunction(String javaName, String nativeName, FunctionDescriptor descriptor, boolean isVarargs, List<String> parameterNames) {
        emitWithConstantClass(constantBuilder -> {
            Constant mhConstant = constantBuilder.addMethodHandle(javaName, nativeName, descriptor, isVarargs, false)
                    .emitGetter(this, MEMBER_MODS, Constant.QUALIFIED_NAME, nativeName);
            MethodType downcallType = Linker.methodType(descriptor);
            boolean needsAllocator = descriptor.returnLayout().isPresent() &&
                    descriptor.returnLayout().get() instanceof GroupLayout;
            emitFunctionWrapper(mhConstant, javaName, nativeName, downcallType, needsAllocator, isVarargs, parameterNames);
        });
    }

    @Override
    public void addConstant(String javaName, Class<?> type, Object value) {
        if (type.equals(MemorySegment.class)) {
            emitWithConstantClass(constantBuilder -> {
                constantBuilder.addConstantDesc(javaName, type, value)
                        .emitGetter(this, MEMBER_MODS, Constant.JAVA_NAME);
            });
        } else {
            emitGetter(MEMBER_MODS, type, javaName, getConstantString(type, value));
        }
    }

    // private generation

    private void emitFunctionWrapper(Constant mhConstant, String javaName, String nativeName, MethodType declType,
                                     boolean needsAllocator, boolean isVarargs, List<String> parameterNames) {
        incrAlign();
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
        append(mhConstant.kind().fieldName(javaName));
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
        append(methodType.returnType().getSimpleName() + " " + javaName + " (");
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
            append(delim + " " + pType.getSimpleName() + " " + pName);
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
        Type.Primitive.Kind kind = primType.kind();
        if (primitiveKindSupported(kind) && !kind.layout().isEmpty()) {
            incrAlign();
            indent();
            append(MEMBER_MODS);
            append(" " + primType.kind().layout().orElseThrow().getClass().getSimpleName());
            append(" " + uniqueNestedClassName(name));
            append(" = ");
            append(toplevel().rootConstants().resolvePrimitiveLayout((ValueLayout)kind.layout().get()).accessExpression());
            append(";\n");
            decrAlign();
        }
    }

    void emitPointerTypedef(String name) {
        incrAlign();
        indent();
        append(MEMBER_MODS);
        append(" OfAddress ");
        append(uniqueNestedClassName(name));
        append(" = ");
        append(toplevel().rootConstants().resolvePrimitiveLayout(TypeImpl.PointerImpl.POINTER_LAYOUT).accessExpression());
        append(";\n");
        decrAlign();
    }

    private boolean primitiveKindSupported(Type.Primitive.Kind kind) {
        return switch(kind) {
            case Short, Int, Long, LongLong, Float, Double, Char -> true;
            default -> false;
        };
    }

    private String getConstantString(Class<?> type, Object value) {
        StringBuilder buf = new StringBuilder();
        if (type == float.class) {
            float f = ((Number)value).floatValue();
            if (Float.isFinite(f)) {
                buf.append(value);
                buf.append("f");
            } else {
                buf.append("Float.valueOf(\"");
                buf.append(value);
                buf.append("\")");
            }
        } else if (type == long.class) {
            buf.append(value);
            buf.append("L");
        } else if (type == double.class) {
            double d = ((Number)value).doubleValue();
            if (Double.isFinite(d)) {
                buf.append(value);
                buf.append("d");
            } else {
                buf.append("Double.valueOf(\"");
                buf.append(value);
                buf.append("\")");
            }
        } else if (type == boolean.class) {
            boolean booleanValue = ((Number)value).byteValue() != 0;
            buf.append(booleanValue);
        } else {
            buf.append("(" + type.getName() + ")");
            buf.append(value + "L");
        }
        return buf.toString();
    }

    private void emitGlobalGetter(Constant segmentConstant, Constant vhConstant, String javaName, String nativeName, Class<?> type) {
        incrAlign();
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
        append("\"));\n");
        decrAlign();
        indent();
        append("}\n");
        decrAlign();
    }

    private void emitGlobalSetter(Constant segmentConstant, Constant vhConstant, String javaName, String nativeName, Class<?> type) {
        incrAlign();
        indent();
        append(MEMBER_MODS + " void " + javaName + "$set(" + " " + type.getSimpleName() + " x) {\n");
        incrAlign();
        indent();
        append(vhConstant.accessExpression());
        append(".set(RuntimeHelper.requireNonNull(");
        append(segmentConstant.accessExpression());
        append(", \"");
        append(nativeName);
        append("\"), x);\n");
        decrAlign();
        indent();
        append("}\n");
        decrAlign();
    }
}
