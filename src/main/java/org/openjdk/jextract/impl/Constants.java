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

import org.openjdk.jextract.Type;

import javax.tools.JavaFileObject;
import java.lang.foreign.AddressLayout;
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
        // prime the cache with basic primitive/pointer (immediate) layouts
        for (Type.Primitive.Kind kind : Type.Primitive.Kind.values()) {
            kind.layout().ifPresent(layout -> {
                if (layout instanceof ValueLayout valueLayout) {
                    cache.put(valueLayout, ImmediateConstant.ofPrimitiveLayout(valueLayout));
                }
            });
        }
        AddressLayout pointerLayout = ValueLayout.ADDRESS.withTargetLayout(
                MemoryLayout.sequenceLayout(ValueLayout.JAVA_BYTE));
        cache.put(pointerLayout, ImmediateConstant.ofPrimitiveLayout(pointerLayout));
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

    static sealed abstract class Constant permits Builder.NamedConstant, ImmediateConstant {

        final Class<?> type;

        public Constant(Class<?> type) {
            this.type = type;
        }

        Class<?> type() {
            return type;
        }

        String getterName(String javaName) {
            return javaName + nameSuffix();
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
            } else {
                return "";
            }
        }

        abstract String accessExpression();
    }

    final static class ImmediateConstant extends Constant {
        final String value;

        ImmediateConstant(Class<?> type, String value) {
            super(type);
            this.value = value;
        }

        @Override
        String accessExpression() {
            return value;
        }

        static ImmediateConstant ofPrimitiveLayout(ValueLayout vl) {
            final String layoutStr;
            if (vl.carrier() == boolean.class) {
                layoutStr = "JAVA_BOOLEAN";
            } else if (vl.carrier() == char.class) {
                layoutStr = "JAVA_CHAR";
            } else if (vl.carrier() == byte.class) {
                layoutStr = "JAVA_BYTE";
            } else if (vl.carrier() == short.class) {
                layoutStr = "JAVA_SHORT";
            } else if (vl.carrier() == int.class) {
                layoutStr = "JAVA_INT";
            } else if (vl.carrier() == float.class) {
                layoutStr = "JAVA_FLOAT";
            } else if (vl.carrier() == long.class) {
                layoutStr = "JAVA_LONG";
            } else if (vl.carrier() == double.class) {
                layoutStr = "JAVA_DOUBLE";
            } else if (vl.carrier() == MemorySegment.class) {
                layoutStr = "RuntimeHelper.POINTER";
            } else {
                throw new UnsupportedOperationException("Unsupported layout: " + vl);
            }
            return new ImmediateConstant(MemoryLayout.class, layoutStr);
        }

        static Constant ofLiteral(Class<?> type, Object value) {
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
            return new ImmediateConstant(type, buf.toString());
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

        final class NamedConstant extends Constant {
            final String constantName;

            NamedConstant(Class<?> type) {
                super(type);
                this.constantName = newConstantName();
            }

            String constantName() {
                return constantName;
            }

            @Override
            String accessExpression() {
                return className() + "." + constantName;
            }
        }

        private Constant emitDowncallMethodHandleField(String nativeName, FunctionDescriptor descriptor, boolean isVarargs, boolean virtual) {
            Constant functionDesc = addFunctionDesc(descriptor);
            incrAlign();
            NamedConstant mhConst = new NamedConstant(MethodHandle.class);
            indent();
            append(memberMods() + "MethodHandle ");
            append(mhConst.constantName + " = RuntimeHelper.");
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
            return mhConst;
        }

        private Constant emitUpcallMethodHandleField(String className, String methodName, FunctionDescriptor descriptor) {
            Constant functionDesc = addFunctionDesc(descriptor);
            incrAlign();
            NamedConstant mhConst = new NamedConstant(MethodHandle.class);
            indent();
            append(memberMods() + "MethodHandle ");
            append(mhConst.constantName + " = RuntimeHelper.upcallHandle(");
            append(className + ".class, ");
            append("\"" + methodName + "\", ");
            append(functionDesc.accessExpression());
            append(");\n");
            decrAlign();
            return mhConst;
        }

        private Constant emitVarHandle(ValueLayout valueLayout) {
            Constant layoutConstant = addLayout(valueLayout);
            incrAlign();
            indent();
            NamedConstant vhConst = new NamedConstant(VarHandle.class);
            append(memberMods() + "VarHandle " + vhConst.constantName + " = ");
            append(layoutConstant.accessExpression());
            append(".varHandle();\n");
            decrAlign();
            return vhConst;
        }

        private Constant emitFieldVarHandle(String nativeName, GroupLayout parentLayout, List<String> prefixElementNames) {
            Constant layoutConstant = addLayout(parentLayout);
            incrAlign();
            indent();
            NamedConstant vhConst = new NamedConstant(VarHandle.class);
            append(memberMods() + "VarHandle " + vhConst.constantName + " = ");
            append(layoutConstant.accessExpression());
            append(".varHandle(");
            String prefix = "";
            for (String prefixElementName : prefixElementNames) {
                append(prefix + "MemoryLayout.PathElement.groupElement(\"" + prefixElementName + "\")");
                prefix = ", ";
            }
            append(prefix + "MemoryLayout.PathElement.groupElement(\"" + nativeName + "\")");
            append(")");
            append(";\n");
            decrAlign();
            return vhConst;
        }

        private Constant emitLayoutField(MemoryLayout layout) {
            NamedConstant layoutConst = new NamedConstant(MemoryLayout.class);
            incrAlign();
            indent();
            String layoutClassName = Utils.layoutDeclarationType(layout).getSimpleName();
            append(memberMods() + layoutClassName + " " + layoutConst.constantName + " = ");
            emitLayoutString(layout);
            append(";\n");
            decrAlign();
            return layoutConst;
        }

        private void emitLayoutString(MemoryLayout l) {
            if (l instanceof ValueLayout val) {
                append(ImmediateConstant.ofPrimitiveLayout(val).accessExpression());
                if (l.byteAlignment() != l.byteSize()) {
                    append(".withByteAlignment(");
                    append(l.byteAlignment());
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
                append("MemoryLayout.paddingLayout(" + l.byteSize() + ")");
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
            NamedConstant descConstant = new NamedConstant(FunctionDescriptor.class);
            append(descConstant.constantName);
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
            return descConstant;
        }

        private Constant emitConstantString(Object value) {
            incrAlign();
            indent();
            append(memberMods());
            append("MemorySegment ");
            NamedConstant segConstant = new NamedConstant(MemorySegment.class);
            append(segConstant.constantName);
            append(" = RuntimeHelper.CONSTANT_ALLOCATOR.allocateUtf8String(\"");
            append(Utils.quote(Objects.toString(value)));
            append("\");\n");
            decrAlign();
            return segConstant;
        }

        private Constant emitConstantAddress(Object value) {
            incrAlign();
            indent();
            append(memberMods());
            append("MemorySegment ");
            NamedConstant segConstant = new NamedConstant(MemorySegment.class);
            append(segConstant.constantName);
            append(" = MemorySegment.ofAddress(");
            append(((Number)value).longValue());
            append("L);\n");
            decrAlign();
            return segConstant;
        }

        private Constant emitSegmentField(String nativeName, MemoryLayout layout) {
            Constant layoutConstant = addLayout(layout);
            incrAlign();
            indent();
            append(memberMods());
            append("MemorySegment ");
            NamedConstant segConstant = new NamedConstant(MemorySegment.class);
            append(segConstant.constantName);
            append(" = ");
            append("RuntimeHelper.lookupGlobalVariable(");
            append("\"" + nativeName + "\", ");
            append(layoutConstant.accessExpression());
            append(");\n");
            decrAlign();
            return segConstant;
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

    public Constant addFieldVarHandle(String nativeName, GroupLayout parentLayout, List<String> prefixElementNames) {
        return builder().emitFieldVarHandle(nativeName, parentLayout, prefixElementNames);
    }

    public Constant addGlobalVarHandle(ValueLayout valueLayout) {
        record VarHandleKey(ValueLayout valueLayout) { }
        VarHandleKey key = new VarHandleKey(valueLayout.withoutName());
        Constant constant = cache.get(key);
        if (constant == null) {
            constant = builder().emitVarHandle(valueLayout);
            cache.put(key, constant);
        }
        return constant;
    }

    public Constant addDowncallMethodHandle(String nativeName, FunctionDescriptor descriptor, boolean isVarargs) {
        return builder().emitDowncallMethodHandleField(nativeName, descriptor, isVarargs, false);
    }

    public Constant addVirtualDowncallMethodHandle(FunctionDescriptor descriptor) {
        record DowncallKey(FunctionDescriptor desc) { }
        DowncallKey downcallKey = new DowncallKey(descriptor);
        Constant constant = cache.get(downcallKey);
        if (constant == null) {
            constant = builder().emitDowncallMethodHandleField(null, descriptor, false, true);
            cache.put(downcallKey, constant);
        }
        return constant;
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
                constant = builder().emitConstantString(value);
            } else if (type == MemorySegment.class) {
                constant = builder().emitConstantAddress(value);
            } else {
                constant = ImmediateConstant.ofLiteral(type, value);
            }
            cache.put(key, constant);
        }
        return constant;
    }
}
