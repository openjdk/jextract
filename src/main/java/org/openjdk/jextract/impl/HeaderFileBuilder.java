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
import java.lang.foreign.MemorySegment;
import java.lang.foreign.SegmentAllocator;
import java.lang.foreign.SequenceLayout;
import java.lang.foreign.ValueLayout;
import java.lang.invoke.MethodHandle;

import org.openjdk.jextract.Declaration;
import org.openjdk.jextract.Type;

import java.lang.invoke.MethodType;
import java.lang.invoke.VarHandle;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.StringJoiner;

/**
 * A helper class to generate header interface class in source form.
 * After aggregating various constituents of a .java source, build
 * method is called to get overall generated source string.
 */
class HeaderFileBuilder extends ClassSourceBuilder {

    static final String MEMBER_MODS = "public static";

    HeaderFileBuilder(SourceFileBuilder builder, String className, String superName) {
        super(builder, "public", Kind.CLASS, className, superName, null);
    }

    public void addVar(Declaration.Variable varTree, String javaName,
        MemoryLayout layout, Optional<String> fiName) {
        String nativeName = varTree.name();
        String layoutConstant = emitLayoutConstantWithMangledName(javaName, layout, null);
        if (layout instanceof SequenceLayout || layout instanceof GroupLayout) {
            if (layout.byteSize() > 0) {
                String segmentField = emitConstantWithMangledName(MemorySegment.class, javaName, segmentField(nativeName, layoutConstant), null);
                emitGetterWithMangledName(MEMBER_MODS, MemorySegment.class, javaName, segmentField, varTree);
            };
        } else if (layout instanceof ValueLayout valueLayout) {
            emitGetterWithMangledName(MEMBER_MODS, MemoryLayout.class, javaName, layoutConstant, null);
            String vhConstant = emitConstantWithMangledName(VarHandle.class, javaName, STR."\{layoutConstant}.varHandle()", null);
            emitGetterWithMangledName(MEMBER_MODS, VarHandle.class, javaName, vhConstant, null);
            String segmentConstant = emitConstantWithMangledName(MemorySegment.class, javaName, segmentField(nativeName, layoutConstant), null);
            emitGetterWithMangledName(MEMBER_MODS, MemorySegment.class, javaName, segmentConstant, null);
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
        String mhConstant = emitDowncallGetter(javaName, nativeName, descriptor, isVarargs);
        MethodType downcallType = descriptor.toMethodType();
        boolean needsAllocator = descriptor.returnLayout().isPresent() &&
                descriptor.returnLayout().get() instanceof GroupLayout;
        emitFunctionWrapper(mhConstant, javaName, downcallType, needsAllocator, isVarargs, parameterNames, funcTree);
    }

    String emitDowncallGetter(String javaName, String nativeName, FunctionDescriptor descriptor, boolean isVarargs) {
        incrAlign();
        indent();
        String getterName = mangleName(javaName, MethodHandle.class);
        append(STR."\{MEMBER_MODS} MethodHandle \{getterName}() {\n");
        incrAlign();
        String constant = addLocalDowncallHandle(nativeName, descriptor, isVarargs);
        indent();
        append(STR."return \{checkNonNull(constant, javaName)};\n");
        decrAlign();
        indent();
        append("}\n");
        decrAlign();
        return className() + "." + getterName + "()";
    }

    String addLocalDowncallHandle(String nativeName, FunctionDescriptor descriptor, boolean isVarargs) {
        class LocalConstantClassHolder extends ClassSourceBuilder {
            public LocalConstantClassHolder() {
                super(HeaderFileBuilder.this.sourceFileBuilder(), "", Kind.CLASS, "Holder", null, HeaderFileBuilder.this);
            }
        }
        ClassSourceBuilder localConstantClass = new LocalConstantClassHolder();
        localConstantClass.classBegin();
        String desc = localConstantClass.emitDescriptorConstant("DESC", descriptor, null);
        String mh = localConstantClass.emitConstant(MethodHandle.class, "MH",
                downcallHandleString(nativeName, desc, isVarargs, false), null);
        localConstantClass.classEnd();
        return mh;
    }

    public void addConstant(Declaration.Constant constantTree, String javaName, Class<?> javaType) {
        Object value = constantTree.value();
        String constantName = emitConstant(javaType, javaName, constantValue(javaType, value), null);
        emitGetter(MEMBER_MODS, javaType, javaName, constantName, constantTree);
    }

    String constantValue(Class<?> type, Object value) {
        if (value instanceof String) {
            return STR."RuntimeHelper.CONSTANT_ALLOCATOR.allocateFrom(\"\{Utils.quote(Objects.toString(value))}\");";
        } else if (type == MemorySegment.class) {
            return STR."MemorySegment.ofAddress(\{((Number)value).longValue()}L);";
        } else {
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
                buf.append(value.toString());
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
    }

    // private generation

    private static List<String> finalizeParameterNames(List<String> parameterNames, boolean needsAllocator, boolean isVarArg) {
        List<String> result = new ArrayList<>();

        if (needsAllocator) {
            result.add("allocator");
        }

        int i = 0;
        for (; i < parameterNames.size(); i++) {
            String name = parameterNames.get(i);
            if (name.isEmpty()) {
                name = "x" + i;
            }
            result.add(name);
        }

        if (isVarArg) {
            result.add("x" + i);
        }

        return result;
    }

    private static String paramExprs(MethodType type, List<String> parameterNames, boolean isVarArg) {
        assert parameterNames.size() >= type.parameterCount();
        StringJoiner sb = new StringJoiner(", ");
        int i = 0;
        for (; i < type.parameterCount(); i++) {
            String pName = parameterNames.get(i);
            sb.add(type.parameterType(i).getSimpleName() + " " + pName);
        }

        if (isVarArg) {
            sb.add("Object... " + parameterNames.get(i));
        }

        return sb.toString();
    }

    private void emitFunctionWrapper(String mhGetterExpr, String javaName, MethodType declType, boolean needsAllocator,
                                     boolean isVarArg, List<String> parameterNames, Declaration decl) {
        List<String> finalParamNames = finalizeParameterNames(parameterNames, needsAllocator, isVarArg);
        if (needsAllocator) {
            declType = declType.insertParameterTypes(0, SegmentAllocator.class);
        }

        String retType = declType.returnType().getSimpleName();
        String returnExpr = "";
        if (!declType.returnType().equals(void.class)) {
            returnExpr = STR."return (\{retType}) ";
        }
        incrAlign();
        emitDocComment(decl);
        appendLines(STR."""
            public static \{retType} \{javaName}(\{paramExprs(declType, finalParamNames, isVarArg)}) {
                var mh$ = \{mhGetterExpr};
                try {
                    \{returnExpr}mh$.invokeExact(\{String.join(", ", finalParamNames)});
                } catch (Throwable ex$) {
                   throw new AssertionError("should not reach here", ex$);
                }
            }
            """);
        decrAlign();
    }

    private void emitFunctionalInterfaceGetter(String fiName, String javaName) {
        appendIndentedLines(STR."""
            public static \{fiName} \{javaName}() {
                return \{fiName}.ofAddress(\{javaName}$get(), Arena.global());
            }
            """);
    }

    void emitPrimitiveTypedef(Type.Primitive primType, String name) {
        emitPrimitiveTypedef(null, primType, name);
    }

    void emitPrimitiveTypedef(Declaration.Typedef typedefTree, Type.Primitive primType, String name) {
        Type.Primitive.Kind kind = primType.kind();
        if (primitiveKindSupported(kind) && kind.layout().isPresent()) {
            emitLayoutConstant(name, kind.layout().get(), typedefTree);
        }
    }

    void emitPointerTypedef(String name) {
        emitPointerTypedef(null, name);
    }

    void emitPointerTypedef(Declaration.Typedef typedefTree, String name) {
        emitLayoutConstant(name, TypeImpl.PointerImpl.POINTER_LAYOUT, typedefTree);
    }

    private boolean primitiveKindSupported(Type.Primitive.Kind kind) {
        return switch(kind) {
            case Short, Int, Long, LongLong, Float, Double, Char -> true;
            default -> false;
        };
    }

    private void emitGlobalGetter(String segmentConstant, String vhConstant, String javaName, String nativeName,
                                  Class<?> type, Declaration.Variable decl, String docHeader) {
        incrAlign();
        emitDocComment(decl, docHeader);
        appendLines(STR."""
            public static \{type.getSimpleName()} \{javaName}$get() {
                return (\{type.getSimpleName()}) \{vhConstant}.get(RuntimeHelper.requireNonNull(\{segmentConstant}, "\{nativeName}"), 0L);
            }
            """);
        decrAlign();
    }

    private void emitGlobalSetter(String segmentConstant, String vhConstant, String javaName, String nativeName,
                                  Class<?> type, Declaration.Variable decl, String docHeader) {
        incrAlign();
        emitDocComment(decl, docHeader);
        appendLines(STR."""
            public static void \{javaName}$set(\{type.getSimpleName()} x) {
                \{vhConstant}.set(RuntimeHelper.requireNonNull(\{segmentConstant}, "\{nativeName}"), 0L, x);
            }
            """);
        decrAlign();
    }
}
