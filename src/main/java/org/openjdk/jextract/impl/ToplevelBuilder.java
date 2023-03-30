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
import org.openjdk.jextract.Declaration;
import org.openjdk.jextract.Type;

import javax.tools.JavaFileObject;
import java.lang.constant.ClassDesc;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * A helper class to generate header interface class in source form.
 * After aggregating various constituents of a .java source, build
 * method is called to get overall generated source string.
 */
class ToplevelBuilder extends JavaSourceBuilder {

    private int declCount;
    private final List<JavaSourceBuilder> builders = new ArrayList<>();
    private SplitHeader lastHeader;
    private int headersCount;
    private final ClassDesc headerDesc;

    Constants constants;

    static final int DECLS_PER_HEADER_CLASS = Integer.getInteger("jextract.decls.per.header", 1000);

    ToplevelBuilder(String packageName, String headerClassName) {
        this.headerDesc = ClassDesc.of(packageName, headerClassName);
        SplitHeader first = lastHeader = new FirstHeader(headerClassName);
        builders.add(first);
        constants = new Constants(this);
        first.classBegin();
    }

    public List<JavaFileObject> toFiles() {
        lastHeader.classEnd();
        List<JavaFileObject> files = new ArrayList<>();
        files.addAll(builders.stream()
                .flatMap(b -> b.toFiles().stream()).toList());
        files.addAll(constants.toFiles());
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
    protected Constants constants() {
        return constants;
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
}
