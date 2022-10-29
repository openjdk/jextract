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

import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.GroupLayout;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.MemoryLayout;
import java.lang.foreign.ValueLayout;
import org.openjdk.jextract.Declaration;
import org.openjdk.jextract.Type;

import javax.tools.JavaFileObject;
import java.lang.constant.ClassDesc;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * A helper class to generate header interface class in source form.
 * After aggregating various constituents of a .java source, build
 * method is called to get overall generated source string.
 */
class ToplevelBuilder extends JavaSourceBuilder {

    private int declCount;
    private final List<JavaSourceBuilder> builders = new ArrayList<>();
    private SplitHeader lastHeader;
    private final RootConstants rootConstants;
    private int headersCount;
    private final ClassDesc headerDesc;

    static final int DECLS_PER_HEADER_CLASS = Integer.getInteger("jextract.decls.per.header", 1000);

    ToplevelBuilder(String packageName, String headerClassName) {
        this.headerDesc = ClassDesc.of(packageName, headerClassName);
        SplitHeader first = lastHeader = new FirstHeader(headerClassName);
        rootConstants = new RootConstants();
        first.classBegin();
        builders.add(first);
    }

    public RootConstants rootConstants() {
        return rootConstants;
    }

    public List<JavaFileObject> toFiles() {
        if (constantBuilder != null) {
            constantBuilder.classEnd();
        }
        lastHeader.classEnd();
        builders.addAll(constantBuilders);
        builders.add(rootConstants);
        List<JavaFileObject> files = new ArrayList<>();
        files.addAll(builders.stream()
                .flatMap(b -> b.toFiles().stream()).toList());
        return files;
    }

    public String headerClassName() {
        return headerDesc.displayName();
    }

    @Override
    boolean isEnclosedBySameName(String name) {
        return false;
    }

    @Override
    public String packageName() {
        return headerDesc.packageName();
    }

    @Override
    public void addVar(Declaration.Variable varTree, String javaName,
        MemoryLayout layout, Optional<String> fiName) {
        nextHeader().addVar(varTree, javaName, layout, fiName);
    }

    @Override
    public void addFunction(Declaration.Function funcTree, FunctionDescriptor descriptor,
            String javaName, List<String> parameterNames) {
        nextHeader().addFunction(funcTree, descriptor, javaName, parameterNames);
    }

    @Override
    public void addConstant(Declaration.Constant constantTree, String javaName, Class<?> javaType) {
        nextHeader().addConstant(constantTree, javaName, javaType);
    }

    @Override
    public void addTypedef(Declaration.Typedef typedefTree, String javaName,
        String superClass, Type type) {
        if (type instanceof Type.Primitive primitive) {
            // primitive
            nextHeader().emitPrimitiveTypedef(typedefTree, primitive, javaName);
        } else if (((TypeImpl)type).isPointer()) {
            // pointer typedef
            nextHeader().emitPointerTypedef(typedefTree, javaName);
        } else {
            TypedefBuilder builder = new TypedefBuilder(this, typedefTree, javaName, superClass);
            builders.add(builder);
            builder.classBegin();
            builder.classEnd();
        }
    }

    @Override
    public StructBuilder addStruct(Declaration.Scoped tree, boolean isNestedAnonStruct,
        String javaName, GroupLayout layout) {
        StructBuilder structBuilder = new StructBuilder(this, tree, javaName, layout) {
            @Override
            boolean isClassFinal() {
                return false;
            }

            @Override
            void emitConstructor() {
                // None...
            }
        };
        builders.add(structBuilder);
        return structBuilder;
    }

    @Override
    public void addFunctionalInterface(Type.Function funcType, String javaName,
        FunctionDescriptor descriptor, Optional<List<String>> parameterNames) {
        FunctionalInterfaceBuilder builder = new FunctionalInterfaceBuilder(this, funcType, javaName, descriptor, parameterNames);
        builders.add(builder);
        builder.classBegin();
        builder.classEnd();
    }

    private SplitHeader nextHeader() {
        if (declCount == DECLS_PER_HEADER_CLASS) {
            boolean hasSuper = !(lastHeader instanceof FirstHeader);
            SplitHeader headerFileBuilder = new SplitHeader(headerDesc.displayName() + "_" + ++headersCount,
                    hasSuper ? lastHeader.className() : null);
            lastHeader.classEnd();
            headerFileBuilder.classBegin();
            builders.add(headerFileBuilder);
            lastHeader = headerFileBuilder;
            declCount = 1;
            return headerFileBuilder;
        } else {
            declCount++;
            return lastHeader;
        }
    }

    class SplitHeader extends HeaderFileBuilder {
        SplitHeader(String name, String superclass) {
            super(ToplevelBuilder.this, name, superclass);
        }

        @Override
        boolean isClassFinal() {
            return false;
        }

        @Override
        void emitConstructor() {
            // None...
        }
    }

    class FirstHeader extends SplitHeader {

        FirstHeader(String name) {
            super(name, "#{SUPER}");
        }

        @Override
        void classBegin() {
            super.classBegin();
            // emit basic primitive types
            emitPrimitiveTypedef(Type.primitive(Type.Primitive.Kind.Bool), "C_BOOL");
            emitPrimitiveTypedef(Type.primitive(Type.Primitive.Kind.Char), "C_CHAR");
            emitPrimitiveTypedef(Type.primitive(Type.Primitive.Kind.Short), "C_SHORT");
            emitPrimitiveTypedef(Type.primitive(Type.Primitive.Kind.Int), "C_INT");
            emitPrimitiveTypedef(Type.primitive(Type.Primitive.Kind.Long), "C_LONG");
            emitPrimitiveTypedef(Type.primitive(Type.Primitive.Kind.LongLong), "C_LONG_LONG");
            emitPrimitiveTypedef(Type.primitive(Type.Primitive.Kind.Float), "C_FLOAT");
            emitPrimitiveTypedef(Type.primitive(Type.Primitive.Kind.Double), "C_DOUBLE");
            emitPointerTypedef("C_POINTER");
        }

        @Override
        String build() {
            HeaderFileBuilder last = lastHeader;
            return super.build().replace("extends #{SUPER}",
                    last != this ? "extends " + last.className() : "");
        }
    }

    // constant support

    class RootConstants extends ConstantBuilder {

        private final Map<ValueLayout, Constant> primitiveLayouts = new HashMap<>();

        public RootConstants() {
            super(ToplevelBuilder.this, "Constants$root");
            classBegin();
            addPrimitiveLayout("C_BOOL", Type.Primitive.Kind.Bool);
            addPrimitiveLayout("C_CHAR", Type.Primitive.Kind.Char);
            addPrimitiveLayout("C_SHORT", Type.Primitive.Kind.Short);
            addPrimitiveLayout("C_INT", Type.Primitive.Kind.Int);
            addPrimitiveLayout("C_LONG", Type.Primitive.Kind.Long);
            addPrimitiveLayout("C_LONG_LONG", Type.Primitive.Kind.LongLong);
            addPrimitiveLayout("C_FLOAT", Type.Primitive.Kind.Float);
            addPrimitiveLayout("C_DOUBLE", Type.Primitive.Kind.Double);
            addPrimitiveLayout("C_POINTER", TypeImpl.PointerImpl.POINTER_LAYOUT);
            classEnd();
        }

        @Override
        String mods() {
            return "final "; // Constants$root package-private!
        }

        @Override
        protected String primitiveLayoutString(ValueLayout vl) {
            if (vl.carrier() == boolean.class) {
                return "JAVA_BOOLEAN";
            } else if (vl.carrier() == char.class) {
                return "JAVA_CHAR" + withBitAlignmentIfNeeded(ValueLayout.JAVA_CHAR, vl);
            } else if (vl.carrier() == byte.class) {
                return "JAVA_BYTE";
            } else if (vl.carrier() == short.class) {
                return "JAVA_SHORT" + withBitAlignmentIfNeeded(ValueLayout.JAVA_SHORT, vl);
            } else if (vl.carrier() == int.class) {
                return "JAVA_INT" + withBitAlignmentIfNeeded(ValueLayout.JAVA_INT, vl);
            } else if (vl.carrier() == float.class) {
                return "JAVA_FLOAT" + withBitAlignmentIfNeeded(ValueLayout.JAVA_FLOAT, vl);
            } else if (vl.carrier() == long.class) {
                return "JAVA_LONG" + withBitAlignmentIfNeeded(ValueLayout.JAVA_LONG, vl);
            } else if (vl.carrier() == double.class) {
                return "JAVA_DOUBLE" + withBitAlignmentIfNeeded(ValueLayout.JAVA_DOUBLE, vl);
            } else if (vl.carrier() == MemorySegment.class) {
                return "ADDRESS.withBitAlignment(" + vl.bitAlignment() + ").asUnbounded()";
            } else {
                return "MemoryLayout.paddingLayout(" + vl.bitSize() +  ")";
            }
        }

        String withBitAlignmentIfNeeded(ValueLayout original, ValueLayout actual) {
            if (original.bitAlignment() == actual.bitAlignment()) {
                return "";
            }
            return ".withBitAlignment(" + actual.bitAlignment() + ")";
        }

        private Constant addPrimitiveLayout(String javaName, ValueLayout layout) {
            ValueLayout layoutNoName = layoutNoName(layout);
            Constant layoutConstant = super.addLayout(javaName, layoutNoName);
            primitiveLayouts.put(layoutNoName, layoutConstant);
            return layoutConstant;
        }

        private Constant addPrimitiveLayout(String javaName, Type.Primitive.Kind kind) {
            return addPrimitiveLayout(javaName, (ValueLayout)kind.layout().orElseThrow());
        }

        private ValueLayout layoutNoName(ValueLayout layout) {
            // drop name if present
            return MemoryLayout.valueLayout(layout.carrier(), layout.order())
                    .withBitAlignment(layout.bitAlignment());
        }

        public Constant resolvePrimitiveLayout(ValueLayout layout) {
            return primitiveLayouts.get(layoutNoName(layout));
        }
    }

    // other constants

    int constant_counter = 0;
    int constant_class_index = 0;
    List<ConstantBuilder> constantBuilders = new ArrayList<>();

    static final int CONSTANTS_PER_CLASS = Integer.getInteger("jextract.constants.per.class", 5);
    ConstantBuilder constantBuilder;

    protected void emitWithConstantClass(Consumer<ConstantBuilder> constantConsumer) {
        if (constant_counter > CONSTANTS_PER_CLASS || constantBuilder == null) {
            if (constantBuilder != null) {
                constantBuilder.classEnd();
            }
            constant_counter = 0;
            constantBuilder = new ConstantsSequelBuilder(this, "constants$" + constant_class_index++);
            constantBuilders.add(constantBuilder);
            constantBuilder.classBegin();
        }
        constantConsumer.accept(constantBuilder);
        constant_counter++;
    }

    static final class ConstantsSequelBuilder extends ConstantBuilder {

        ConstantsSequelBuilder(JavaSourceBuilder enclosing, String className) {
            super(enclosing, className);
        }

        @Override
        String mods() {
            return "final "; // constants package-private!
        }
    }

}
