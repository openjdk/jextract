/*
 * Copyright (c) 2021, 2023, Oracle and/or its affiliates. All rights reserved.
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

import javax.tools.JavaFileObject;
import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.GroupLayout;
import java.lang.foreign.MemoryLayout;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.SequenceLayout;
import java.lang.foreign.StructLayout;
import java.lang.foreign.ValueLayout;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.VarHandle;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

public class Constants {

    private final Map<Object, Constant> cache = new HashMap<>();

    List<Builder> constantBuilders = new ArrayList<>();
    Builder currentBuilder;

    public Constants(JavaSourceBuilder enclosing) {
        currentBuilder = new Builder(enclosing, 0);
        constantBuilders.add(currentBuilder);
        currentBuilder.classBegin();
    }

    static final int CONSTANTS_PER_CLASS = Integer.getInteger("jextract.constants.per.class", 5);

    private Builder builder() {
        if (currentBuilder.constantIndex > CONSTANTS_PER_CLASS || currentBuilder == null) {
            if (currentBuilder != null) {
                currentBuilder.classEnd();
            }
            currentBuilder = new Builder(currentBuilder.enclosing, constantBuilders.size());
            constantBuilders.add(currentBuilder);
            currentBuilder.classBegin();
        }
        return currentBuilder;
    }

    record Constant(Builder builder, Class<?> type, String constantName) {

        String getterName(String javaName) {
            return javaName + nameSuffix();
        }

        String accessExpression() {
            return builder.className() + "." + constantName;
        }

        Constant emitGetter(ClassSourceBuilder builder, String mods, String javaName) {
            return emitGetter(builder, mods, c -> c.getterName(javaName));
        }

        Constant emitGetter(ClassSourceBuilder builder, String mods, String javaName, String symbolName) {
            return emitGetter(builder, mods, symbolName, c -> c.getterName(javaName));
        }

        Constant emitGetter(ClassSourceBuilder builder, String mods, Function<Constant, String> getterNameFunc) {
            builder.emitConstantGetter(mods, getterNameFunc.apply(this), false, null, this);
            return this;
        }

        Constant emitGetter(ClassSourceBuilder builder, String mods, String symbolName, Function<Constant, String> getterNameFunc) {
            builder.emitConstantGetter(mods, getterNameFunc.apply(this), true, symbolName, this);
            return this;
        }

        String nameSuffix() {
            if (type.equals(MemorySegment.class)) {
                return "$SEGMENT";
            } else if (type.equals(MemoryLayout.class)) {
                return "$LAYOUT";
            } else if (type.equals(MethodHandle.class)) {
                return "$MH";
            } else if (type.equals(VarHandle.class)) {
                return "$VH";
            } else if (type.equals(FunctionDescriptor.class)) {
                return "$DESC";
            } else if (type.isPrimitive()) {
                return "$" + type.getSimpleName().toUpperCase();
            } else {
                throw new AssertionError("Cannot get here: " + type.getSimpleName());
            }
        }
    }

    public List<JavaFileObject> toFiles() {
        currentBuilder.classEnd();
        List<JavaFileObject> files = new ArrayList<>();
        files.addAll(constantBuilders.stream()
                .flatMap(b -> b.toFiles().stream()).toList());
        return files;
    }

    class Builder extends ClassSourceBuilder {

        Builder(JavaSourceBuilder encl, int id) {
            super(encl, Kind.CLASS, "constants$" + id);
        }

        String memberMods() {
            return kind == ClassSourceBuilder.Kind.CLASS ?
                    "static final " : "";
        }

        @Override
        String mods() {
            return "final "; // constants package-private!
        }

        int constantIndex = 0;

        private Constant emitDowncallMethodHandleField(String nativeName, FunctionDescriptor descriptor, boolean isVarargs, boolean virtual) {
            Constant functionDesc = addFunctionDesc(descriptor);
            incrAlign();
            String constName = newConstantName();
            indent();
            append(memberMods() + "MethodHandle ");
            append(constName + " = RuntimeHelper.");
            if (isVarargs) {
                append("downcallHandleVariadic");
            } else {
                append("downcallHandle");
            }
            append("(\n");
            incrAlign();
            indent();
            if (!virtual) {
                append("\"" + nativeName + "\"");
                append(",\n");
                indent();
            }
            append(functionDesc.accessExpression());
            append("\n");
            decrAlign();
            indent();
            append(");\n");
            decrAlign();
            return new Constant(this, MethodHandle.class, constName);
        }

        private Constant emitUpcallMethodHandleField(String className, String methodName, FunctionDescriptor descriptor) {
            Constant functionDesc = addFunctionDesc(descriptor);
            incrAlign();
            String constName = newConstantName();
            indent();
            append(memberMods() + "MethodHandle ");
            append(constName + " = RuntimeHelper.upcallHandle(");
            append(className + ".class, ");
            append("\"" + methodName + "\", ");
            append(functionDesc.accessExpression());
            append(");\n");
            decrAlign();
            return new Constant(this, MethodHandle.class, constName);
        }

        private Constant emitVarHandleField(String nativeName, ValueLayout valueLayout,
                                            Constant rootLayout, List<String> prefixElementNames) {
            String layoutAccess = rootLayout != null ?
                    rootLayout.accessExpression() :
                    addLayout(valueLayout).accessExpression();
            incrAlign();
            indent();
            String constName = newConstantName();
            append(memberMods() + "VarHandle " + constName + " = ");
            append(layoutAccess);
            append(".varHandle(");
            String prefix = "";
            if (rootLayout != null) {
                for (String prefixElementName : prefixElementNames) {
                    append(prefix + "MemoryLayout.PathElement.groupElement(\"" + prefixElementName + "\")");
                    prefix = ", ";
                }
                append(prefix + "MemoryLayout.PathElement.groupElement(\"" + nativeName + "\")");
            }
            append(")");
            append(";\n");
            decrAlign();
            return new Constant(this, VarHandle.class, constName);
        }

        private Constant emitLayoutField(MemoryLayout layout) {
            String constName = newConstantName();
            incrAlign();
            indent();
            String layoutClassName = Utils.layoutDeclarationType(layout).getSimpleName();
            append(memberMods() + layoutClassName + " " + constName + " = ");
            emitLayoutString(layout);
            append(";\n");
            decrAlign();
            return new Constant(this, MemoryLayout.class, constName);
        }

        protected String primitiveLayoutString(ValueLayout vl) {
            if (vl.carrier() == boolean.class) {
                return "JAVA_BOOLEAN";
            } else if (vl.carrier() == char.class) {
                return "JAVA_CHAR";
            } else if (vl.carrier() == byte.class) {
                return "JAVA_BYTE";
            } else if (vl.carrier() == short.class) {
                return "JAVA_SHORT";
            } else if (vl.carrier() == int.class) {
                return "JAVA_INT";
            } else if (vl.carrier() == float.class) {
                return "JAVA_FLOAT";
            } else if (vl.carrier() == long.class) {
                return "JAVA_LONG";
            } else if (vl.carrier() == double.class) {
                return "JAVA_DOUBLE";
            } else if (vl.carrier() == MemorySegment.class) {
                return "RuntimeHelper.POINTER";
            } else {
                return "MemoryLayout.paddingLayout(" + vl.bitSize() +  ")";
            }
        }

        private void emitLayoutString(MemoryLayout l) {
            if (l instanceof ValueLayout val) {
                append(primitiveLayoutString(val));
                if (l.bitAlignment() != l.bitSize()) {
                    append(".withBitAlignment(");
                    append(l.bitAlignment());
                    append(")");
                }
            } else if (l instanceof SequenceLayout seq) {
                append("MemoryLayout.sequenceLayout(");
                append(seq.elementCount() + ", ");
                emitLayoutString(seq.elementLayout());
                append(")");
            } else if (l instanceof GroupLayout group) {
                if (group instanceof StructLayout) {
                    append("MemoryLayout.structLayout(\n");
                } else {
                    append("MemoryLayout.unionLayout(\n");
                }
                incrAlign();
                String delim = "";
                for (MemoryLayout e : group.memberLayouts()) {
                    append(delim);
                    indent();
                    emitLayoutString(e);
                    delim = ",\n";
                }
                append("\n");
                decrAlign();
                indent();
                append(")");
            } else {
                // padding (or unsupported)
                append("MemoryLayout.paddingLayout(" + l.bitSize() + ")");
            }
            if (l.name().isPresent()) {
                append(".withName(\"" +  l.name().get() + "\")");
            }
        }

        private Constant emitFunctionDescField(FunctionDescriptor desc) {
            incrAlign();
            indent();
            final boolean noArgs = desc.argumentLayouts().isEmpty();
            append(memberMods());
            append("FunctionDescriptor ");
            String constName = newConstantName();
            append(constName);
            append(" = ");
            if (desc.returnLayout().isPresent()) {
                append("FunctionDescriptor.of(");
                emitLayoutString(desc.returnLayout().get());
                if (!noArgs) {
                    append(",");
                }
            } else {
                append("FunctionDescriptor.ofVoid(");
            }
            if (!noArgs) {
                append("\n");
                incrAlign();
                String delim = "";
                for (MemoryLayout e : desc.argumentLayouts()) {
                    append(delim);
                    indent();
                    emitLayoutString(e);
                    delim = ",\n";
                }
                append("\n");
                decrAlign();
                indent();
            }
            append(");\n");
            decrAlign();
            return new Constant(this, FunctionDescriptor.class, constName);
        }

        private Constant emitConstantSegment(Object value) {
            incrAlign();
            indent();
            append(memberMods());
            append("MemorySegment ");
            String constName = newConstantName();
            append(constName);
            append(" = RuntimeHelper.CONSTANT_ALLOCATOR.allocateUtf8String(\"");
            append(Utils.quote(Objects.toString(value)));
            append("\");\n");
            decrAlign();
            return new Constant(this, MemorySegment.class, constName);
        }

        private Constant emitConstantAddress(Object value) {
            incrAlign();
            indent();
            append(memberMods());
            append("MemorySegment ");
            String constName = newConstantName();
            append(constName);
            append(" = MemorySegment.ofAddress(");
            append(((Number)value).longValue());
            append("L);\n");
            decrAlign();
            return new Constant(this, MemorySegment.class, constName);
        }

        private Constant emitLiteral(Class<?> type, Object value) {
            incrAlign();
            indent();
            append(memberMods());
            append(type.getSimpleName() + " ");
            String constName = newConstantName();
            append(constName + " = ");
            if (type == float.class) {
                float f = ((Number)value).floatValue();
                if (Float.isFinite(f)) {
                    append(value);
                    append("f");
                } else {
                    append("Float.valueOf(\"");
                    append(value);
                    append("\")");
                }
            } else if (type == long.class) {
                append(value.toString());
                append("L");
            } else if (type == double.class) {
                double d = ((Number)value).doubleValue();
                if (Double.isFinite(d)) {
                    append(value);
                    append("d");
                } else {
                    append("Double.valueOf(\"");
                    append(value);
                    append("\")");
                }
            } else if (type == boolean.class) {
                boolean booleanValue = ((Number)value).byteValue() != 0;
                append(booleanValue);
            } else {
                append("(" + type.getName() + ")");
                append(value + "L");
            }
            append(";\n");
            decrAlign();
            return new Constant(this, type, constName);
        }

        private Constant emitSegmentField(String nativeName, MemoryLayout layout) {
            Constant layoutConstant = addLayout(layout);
            incrAlign();
            indent();
            append(memberMods());
            append("MemorySegment ");
            String constName = newConstantName();
            append(constName);
            append(" = ");
            append("RuntimeHelper.lookupGlobalVariable(");
            append("\"" + nativeName + "\", ");
            append(layoutConstant.accessExpression());
            append(");\n");
            decrAlign();
            return new Constant(this, MemorySegment.class, constName);
        }

        String newConstantName() {
            return "const$" + constantIndex++;
        }
    }

    // public API

    public Constant addLayout(MemoryLayout layout) {
        Constant constant = cache.get(layout);
        if (constant == null) {
            constant = builder().emitLayoutField(layout);
            cache.put(layout, constant);
        }
        return constant;
    }

    public Constant addFieldVarHandle(String nativeName, ValueLayout valueLayout,
                                      Constant rootLayout, List<String> prefixElementNames) {
        return addVarHandle(nativeName, valueLayout, rootLayout, prefixElementNames);
    }

    public Constant addGlobalVarHandle(String nativeName, ValueLayout valueLayout) {
        return addVarHandle(nativeName, valueLayout, null, List.of());
    }

    private Constant addVarHandle(String nativeName, ValueLayout valueLayout,
                                  Constant rootLayout, List<String> prefixElementNames) {
        return builder().emitVarHandleField(nativeName, valueLayout, rootLayout, prefixElementNames);
    }

    public Constant addDowncallMethodHandle(String nativeName, FunctionDescriptor descriptor, boolean isVarargs, boolean virtual) {
        return builder().emitDowncallMethodHandleField(nativeName, descriptor, isVarargs, virtual);
    }

    public Constant addUpcallMethodHandle(String className, String name, FunctionDescriptor descriptor) {
        return builder().emitUpcallMethodHandleField(className, name, descriptor);
    }

    public Constant addSegment(String nativeName, MemoryLayout layout) {
        return builder().emitSegmentField(nativeName, layout);
    }

    public Constant addFunctionDesc(FunctionDescriptor desc) {
        Constant constant = cache.get(desc);
        if (constant == null) {
            constant = builder().emitFunctionDescField(desc);
            cache.put(desc, constant);
        }
        return constant;
    }

    public Constant addConstantDesc(Class<?> type, Object value) {
        record ConstantKey(Class<?> type, Object value) { }
        var key = new ConstantKey(type, value);
        Constant constant = cache.get(key);
        if (constant == null) {
            if (value instanceof String) {
                constant = builder().emitConstantSegment(value);
            } else if (type == MemorySegment.class) {
                constant = builder().emitConstantAddress(value);
            } else {
                constant = builder().emitLiteral(type, value);
            }
            cache.put(key, constant);
        }
        return constant;
    }
}
