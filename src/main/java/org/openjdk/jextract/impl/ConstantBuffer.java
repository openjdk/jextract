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
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.StringJoiner;
import java.util.function.Consumer;
import java.util.function.Function;

public class ConstantBuffer {

    private final ClassSourceBuilder builder;
    private List<Constant> constants = new ArrayList<>();

    public ConstantBuffer(ClassSourceBuilder builder) {
        this.builder = builder;
    }

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

        abstract void emit();

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

        @Override
        void emit() {
            // do nothing
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

    final class NamedConstant extends Constant {
        final String constantName;
        final Consumer<String> emitterFunc;

        NamedConstant(Class<?> type, Consumer<String> emitterFunc) {
            this(type, newConstantName(), emitterFunc);
        }

        NamedConstant(Class<?> type, String name, Consumer<String> emitterFunc) {
            super(type);
            this.emitterFunc = emitterFunc;
            this.constantName = name;
        }

        @Override
        String accessExpression() {
            return builder.className() + "." + constantName;
        }

        @Override
        void emit() {
            emitterFunc.accept(constantName);
        }
    }

    private Constant emitDowncallMethodHandleField(String nativeName, FunctionDescriptor descriptor, boolean isVarargs, boolean virtual) {
        Constant functionDesc = addFunctionDesc(descriptor);
        return new NamedConstant(MethodHandle.class, constantName -> {
            builder.incrAlign();
            builder.indent();
            builder.append(MEMBER_MODS + " MethodHandle ");
            builder.append(constantName + " = RuntimeHelper.");
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
        });
    }

    private Constant emitUpcallMethodHandleField(String className, String methodName, FunctionDescriptor descriptor) {
        Constant functionDesc = addFunctionDesc(descriptor);
        return new NamedConstant(MethodHandle.class, constantName ->
            builder.appendIndentedLines(STR."""
                static final MethodHandle \{constantName} = RuntimeHelper.upcallHandle(\{className}.class, "\{methodName}", \{functionDesc});
                """)
        );
    }

    private Constant emitVarHandle(ValueLayout valueLayout) {
        Constant layoutConstant = addLayout(valueLayout);
        return new NamedConstant(VarHandle.class, constantName -> {
            builder.appendIndentedLines(STR."""
                public static final VarHandle \{constantName} = \{layoutConstant}.varHandle();
                """);
        });
    }

    private static String pathElementStr(String nativeName, List<String> prefixElementNames) {
        StringJoiner joiner = new StringJoiner(", ");
        for (String prefixElementName : prefixElementNames) {
            joiner.add(STR."MemoryLayout.PathElement.groupElement(\"\{prefixElementName}\")");
        }
        joiner.add(STR."MemoryLayout.PathElement.groupElement(\"\{nativeName}\")");
        return joiner.toString();
    }

    private Constant emitFieldVarHandle(String nativeName, GroupLayout parentLayout, List<String> prefixElementNames) {
        Constant layoutConstant = addLayout(parentLayout);
        return new NamedConstant(VarHandle.class, constantName -> {
            builder.appendIndentedLines(STR."""
                public static final VarHandle \{constantName} = \{layoutConstant}.varHandle(\{pathElementStr(nativeName, prefixElementNames)});
                """);
        });
    }

    private Constant emitLayoutField(MemoryLayout layout) {
        return emitLayoutField(layout, newConstantName());
    }

    private Constant emitLayoutField(MemoryLayout layout, String name) {
        return new NamedConstant(MemoryLayout.class, name, constantName -> {
            builder.incrAlign();
            builder.indent();
            String layoutClassName = Utils.layoutDeclarationType(layout).getSimpleName();
            builder.append(STR."public static final \{layoutClassName} \{constantName} = ");
            emitLayoutString(layout);
            builder.append(";\n");
            builder.decrAlign();
        });
    }

    private void emitLayoutString(MemoryLayout l) {
        if (l instanceof ValueLayout val) {
            builder.append(ImmediateConstant.ofPrimitiveLayout(val).accessExpression());
            if (l.byteAlignment() != l.byteSize()) {
                builder.append(STR.".withByteAlignment(\{l.byteAlignment()})");
            }
        } else if (l instanceof SequenceLayout seq) {
            builder.append(STR."MemoryLayout.sequenceLayout(\{seq.elementCount()}, ");
            emitLayoutString(seq.elementLayout());
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
                emitLayoutString(e);
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

    private Constant emitFunctionDescField(FunctionDescriptor desc) {
        final boolean noArgs = desc.argumentLayouts().isEmpty();
        return new NamedConstant(FunctionDescriptor.class, constantName -> {
            builder.incrAlign();
            builder.indent();
            builder.append(STR."public static final FunctionDescriptor \{constantName} =");
            if (desc.returnLayout().isPresent()) {
                builder.append("FunctionDescriptor.of(");
                emitLayoutString(desc.returnLayout().get());
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
                    emitLayoutString(e);
                    delim = ",\n";
                }
                builder.append("\n");
                builder.decrAlign();
                builder.indent();
            }
            builder.append(");\n");
            builder.decrAlign();
        });
    }

    private Constant emitConstantString(Object value) {
        return new NamedConstant(MemorySegment.class, constantName ->
            builder.appendIndentedLines(STR."""
                public static final MemorySegment \{constantName} =
                        RuntimeHelper.CONSTANT_ALLOCATOR.allocateFrom("\{Utils.quote(Objects.toString(value))}");
                """)
        );
    }

    private Constant emitConstantAddress(Object value) {
        return new NamedConstant(MemorySegment.class, constantName ->
            builder.appendIndentedLines(STR."""
                public static final MemorySegment \{constantName} =
                        MemorySegment.ofAddress(\{((Number)value).longValue()}L);
                """)
        );
    }

    private Constant emitSegmentField(String nativeName, MemoryLayout layout) {
        Constant layoutConstant = addLayout(layout);
        return new NamedConstant(MemorySegment.class, constantName -> {
            builder.appendIndentedLines(STR."""
                public static final MemorySegment \{constantName} =
                        RuntimeHelper.lookupGlobalVariable("\{nativeName}", \{layoutConstant});
                """);
        });
    }

    String newConstantName() {
        return STR."const$\{constants.size()}";
    }

    // public API

    public Constant addLayout(MemoryLayout layout) {
        Constant layoutConstant = emitLayoutField(layout);
        constants.add(layoutConstant);
        return layoutConstant;
    }

    public Constant addLayoutWithName(MemoryLayout layout, String name) {
        Constant layoutConstant = emitLayoutField(layout, name);
        constants.add(layoutConstant);
        return layoutConstant;
    }

    public Constant addFieldVarHandle(String nativeName, GroupLayout parentLayout, List<String> prefixElementNames) {
        Constant varHandleConstant = emitFieldVarHandle(nativeName, parentLayout, prefixElementNames);
        constants.add(varHandleConstant);
        return varHandleConstant;
    }

    public Constant addGlobalVarHandle(ValueLayout valueLayout) {
        Constant varHandleConstant = emitVarHandle(valueLayout);
        constants.add(varHandleConstant);
        return varHandleConstant;
    }

    public Constant addDowncallMethodHandle(String nativeName, FunctionDescriptor descriptor, boolean isVarargs) {
        Constant downcallHandleConstant = emitDowncallMethodHandleField(nativeName, descriptor, isVarargs, false);
        constants.add(downcallHandleConstant);
        return downcallHandleConstant;
    }

    public Constant addVirtualDowncallMethodHandle(FunctionDescriptor descriptor) {
        Constant downcallHandleConstant = emitDowncallMethodHandleField(null, descriptor, false, true);
        constants.add(downcallHandleConstant);
        return downcallHandleConstant;
    }

    public Constant addUpcallMethodHandle(String className, String name, FunctionDescriptor descriptor) {
        Constant upcallHandleConstant = emitUpcallMethodHandleField(className, name, descriptor);
        constants.add(upcallHandleConstant);
        return upcallHandleConstant;
    }

    public Constant addSegment(String nativeName, MemoryLayout layout) {
        Constant segmentConstant = emitSegmentField(nativeName, layout);
        constants.add(segmentConstant);
        return segmentConstant;
    }

    public Constant addFunctionDesc(FunctionDescriptor desc) {
        Constant functionDescConstant = emitFunctionDescField(desc);
        constants.add(functionDescConstant);
        return functionDescConstant;
    }

    public Constant addConstantDesc(Class<?> type, Object value) {
        Constant constant;
        if (value instanceof String) {
            constant = emitConstantString(value);
        } else if (type == MemorySegment.class) {
            constant = emitConstantAddress(value);
        } else {
            constant = ImmediateConstant.ofLiteral(type, value);
        }
        constants.add(constant);
        return constant;
    }

    void writeConstants() {
        for (Constant c : constants) {
            c.emit();
        }
    }
}
