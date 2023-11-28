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

import org.openjdk.jextract.Declaration;

import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.GroupLayout;
import java.lang.foreign.MemoryLayout;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.SequenceLayout;
import java.lang.foreign.StructLayout;
import java.lang.foreign.ValueLayout;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.VarHandle;
import java.util.List;
import java.util.Objects;
import java.util.StringJoiner;
import java.util.function.Function;

public final class Constants {

    static final String MEMBER_MODS = "public static final";

    static sealed abstract class Constant permits NamedConstant, ImmediateConstant {

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

        Constant emitGetterWithComment(ClassSourceBuilder builder, String mods, String javaName, String symbolName,
                                       Declaration decl) {
            return emitGetterWithComment(builder, mods, symbolName, c -> c.getterName(javaName), decl);
        }

        Constant emitGetter(ClassSourceBuilder builder, String mods, Function<Constant, String> getterNameFunc) {
            builder.emitConstantGetter(mods, getterNameFunc.apply(this), false, null, this, null);
            return this;
        }

        Constant emitGetterWithComment(ClassSourceBuilder builder, String mods, Function<Constant, String> getterNameFunc,
                                       Declaration decl) {
            builder.emitConstantGetter(mods, getterNameFunc.apply(this), false, null, this, decl);
            return this;
        }

        Constant emitGetter(ClassSourceBuilder builder, String mods, String symbolName, Function<Constant, String> getterNameFunc) {
            builder.emitConstantGetter(mods, getterNameFunc.apply(this), true, symbolName, this, null);
            return this;
        }

        Constant emitGetterWithComment(ClassSourceBuilder builder, String mods, String symbolName,
                                       Function<Constant, String> getterNameFunc, Declaration decl) {
            builder.emitConstantGetter(mods, getterNameFunc.apply(this), true, symbolName, this, decl);
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

        @Override
        public String toString() {
            return accessExpression();
        }
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

    final static class NamedConstant extends Constant {

        final String owner;
        final String constantName;

        NamedConstant(Class<?> type, String owner, String name) {
            this(type, owner, name, null);
        }

        NamedConstant(Class<?> type, String owner, String name, Function<String, String> nameFunc) {
            super(type);
            this.owner = owner;
            this.constantName = nameFunc != null ?
                    nameFunc.apply(name) :
                    getterName(name);
        }

        String name() {
            return constantName;
        }

        @Override
        String accessExpression() {
            return owner + "." + constantName;
        }
    }

    public static Constant emitDowncallMethodHandle(ClassSourceBuilder builder, String constantName, String nativeName,
                                                   Constant functionDesc, boolean isVarargs, boolean virtual) {
        NamedConstant constant = new NamedConstant(MethodHandle.class, builder.className(), constantName);
        builder.incrAlign();
        builder.indent();
        builder.append(MEMBER_MODS + " MethodHandle ");
        builder.append(constant.name() + " = RuntimeHelper.");
        if (isVarargs) {
            builder.append("downcallHandleVariadic");
        } else {
            builder.append("downcallHandle");
        }
        builder.append("(\n");
        builder.incrAlign();
        builder.indent();
        if (!virtual) {
            builder.append("\"" + nativeName + "\"");
            builder.append(",\n");
            builder.indent();
        }
        builder.append(functionDesc.accessExpression());
        builder.append("\n");
        builder.decrAlign();
        builder.indent();
        builder.append(");\n");
        builder.decrAlign();
        return constant;
    }

    public static Constant emitVirtualDowncallMethodHandle(ClassSourceBuilder builder, String constantName, Constant functionDesc) {
        return emitDowncallMethodHandle(builder, constantName, null, functionDesc, false, true);
    }

    public static Constant emitUpcallMethodHandle(ClassSourceBuilder builder, String constantName, String className, String methodName, Constant functionDesc) {
        NamedConstant constant = new NamedConstant(MethodHandle.class, builder.className(), constantName);
        builder.appendIndentedLines(STR."""
            static final MethodHandle \{constant.name()} = RuntimeHelper.upcallHandle(\{className}.class, "\{methodName}", \{functionDesc});
            """);
        return constant;
    }

    public static Constant emitVarHandle(ClassSourceBuilder builder, String constantName, Constant layoutConstant) {
        NamedConstant constant = new NamedConstant(VarHandle.class, builder.className(), constantName);
        builder.appendIndentedLines(STR."""
            public static final VarHandle \{constant.name()} = \{layoutConstant}.varHandle();
            """);
        return constant;
    }

    public static String pathElementStr(String nativeName, List<String> prefixElementNames) {
        StringJoiner joiner = new StringJoiner(", ");
        for (String prefixElementName : prefixElementNames) {
            joiner.add(STR."MemoryLayout.PathElement.groupElement(\"\{prefixElementName}\")");
        }
        joiner.add(STR."MemoryLayout.PathElement.groupElement(\"\{nativeName}\")");
        return joiner.toString();
    }

    public static Constant emitFieldVarHandle(ClassSourceBuilder builder, String constantName, String nativeName,
                                               Constant layoutConstant, List<String> prefixElementNames) {
        NamedConstant constant = new NamedConstant(VarHandle.class, builder.className(), constantName);
        builder.appendIndentedLines(STR."""
            public static final VarHandle \{constant.name()} = \{layoutConstant}.varHandle(\{pathElementStr(nativeName, prefixElementNames)});
            """);
        return constant;
    }

    public static Constant emitLayout(ClassSourceBuilder builder, String constantName, MemoryLayout layout) {
        return emitLayout(builder, constantName, null, layout);
    }

    public static Constant emitLayout(ClassSourceBuilder builder, String constantName, Function<String, String> nameFunc, MemoryLayout layout) {
        NamedConstant constant = new NamedConstant(MemoryLayout.class, builder.className(), constantName, nameFunc);
        builder.incrAlign();
        builder.indent();
        String layoutClassName = Utils.layoutDeclarationType(layout).getSimpleName();
        builder.append(STR."public static final \{layoutClassName} \{constant.name()} = ");
        emitLayoutString(builder, layout);
        builder.append(";\n");
        builder.decrAlign();
        return constant;
    }

    private static void emitLayoutString(ClassSourceBuilder builder, MemoryLayout l) {
        if (l instanceof ValueLayout val) {
            builder.append(ImmediateConstant.ofPrimitiveLayout(val).accessExpression());
            if (l.byteAlignment() != l.byteSize()) {
                builder.append(STR.".withByteAlignment(\{l.byteAlignment()})");
            }
        } else if (l instanceof SequenceLayout seq) {
            builder.append(STR."MemoryLayout.sequenceLayout(\{seq.elementCount()}, ");
            emitLayoutString(builder, seq.elementLayout());
            builder.append(")");
        } else if (l instanceof GroupLayout group) {
            if (group instanceof StructLayout) {
                builder.append("MemoryLayout.structLayout(\n");
            } else {
                builder.append("MemoryLayout.unionLayout(\n");
            }
            builder.incrAlign();
            String delim = "";
            for (MemoryLayout e : group.memberLayouts()) {
                builder.append(delim);
                builder.indent();
                emitLayoutString(builder, e);
                delim = ",\n";
            }
            builder.append("\n");
            builder.decrAlign();
            builder.indent();
            builder.append(")");
        } else {
            // padding (or unsupported)
            builder.append(STR."MemoryLayout.paddingLayout(\{l.byteSize()})");
        }
        if (l.name().isPresent()) {
            builder.append(STR.".withName(\"\{l.name().get()}\")");
        }
    }

    public static Constant emitFunctionDesc(ClassSourceBuilder builder, String constantName, FunctionDescriptor desc) {
        NamedConstant constant = new NamedConstant(FunctionDescriptor.class, builder.className(), constantName);
        final boolean noArgs = desc.argumentLayouts().isEmpty();
        builder.incrAlign();
        builder.indent();
        builder.append(STR."public static final FunctionDescriptor \{constant.name()} =");
        if (desc.returnLayout().isPresent()) {
            builder.append("FunctionDescriptor.of(");
            emitLayoutString(builder, desc.returnLayout().get());
            if (!noArgs) {
                builder.append(",");
            }
        } else {
            builder.append("FunctionDescriptor.ofVoid(");
        }
        if (!noArgs) {
            builder.append("\n");
            builder.incrAlign();
            String delim = "";
            for (MemoryLayout e : desc.argumentLayouts()) {
                builder.append(delim);
                builder.indent();
                emitLayoutString(builder, e);
                delim = ",\n";
            }
            builder.append("\n");
            builder.decrAlign();
            builder.indent();
        }
        builder.append(");\n");
        builder.decrAlign();
        return constant;
    }

    public static Constant emitConstant(ClassSourceBuilder builder, String constantName, Class<?> type, Object value) {
        Constant constant;
        if (value instanceof String) {
            constant = emitConstantString(builder, constantName, value);
        } else if (type == MemorySegment.class) {
            constant = emitConstantAddress(builder, constantName, value);
        } else {
            constant = ImmediateConstant.ofLiteral(type, value);
        }
        return constant;
    }

    private static Constant emitConstantString(ClassSourceBuilder builder, String constantName, Object value) {
        NamedConstant constant = new NamedConstant(MemorySegment.class, builder.className(), constantName);
        builder.appendIndentedLines(STR."""
            public static final MemorySegment \{constant.name()} =
                    RuntimeHelper.CONSTANT_ALLOCATOR.allocateFrom("\{Utils.quote(Objects.toString(value))}");
            """);
        return constant;
    }

    private static Constant emitConstantAddress(ClassSourceBuilder builder, String constantName, Object value) {
        NamedConstant constant = new NamedConstant(MemorySegment.class, builder.className(), constantName);
        builder.appendIndentedLines(STR."""
            public static final MemorySegment \{constant.name()} =
                    MemorySegment.ofAddress(\{((Number)value).longValue()}L);
            """);
        return constant;
    }

    public static Constant emitSegmentField(ClassSourceBuilder builder, String constantName, String nativeName, Constant layoutConstant) {
        NamedConstant constant = new NamedConstant(MemorySegment.class, builder.className(), constantName);
        builder.appendIndentedLines(STR."""
            public static final MemorySegment \{constant.name()} =
                    RuntimeHelper.lookupGlobalVariable("\{nativeName}", \{layoutConstant});
            """);
        return constant;
    }
}
