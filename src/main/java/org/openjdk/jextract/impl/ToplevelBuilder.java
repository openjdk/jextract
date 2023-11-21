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
class ToplevelBuilder implements JavaSourceBuilder {
    private static final int DECLS_PER_HEADER_CLASS = Integer.getInteger("jextract.decls.per.header", 1000);

    private int declCount;
    private final List<SourceFileBuilder> builders = new ArrayList<>();
    private final HeaderFileBuilder firstHeader;
    private HeaderFileBuilder lastHeader;
    private int headersCount;
    private final ClassDesc headerDesc;
    private final Constants constants;

    ToplevelBuilder(String packageName, String headerClassName) {
        this.headerDesc = ClassDesc.of(packageName, headerClassName);
        this.constants = new Constants(packageName);
        SourceFileBuilder sfb = newSourceFile(packageName, headerClassName);
        lastHeader = firstHeader = createFirstHeader(sfb, constants);
    }

    private static HeaderFileBuilder createFirstHeader(SourceFileBuilder sfb, Constants constants) {
        HeaderFileBuilder first = new HeaderFileBuilder(sfb, constants, sfb.className(), "#{SUPER}");
        first.classBegin();
        first.emitPrivateDefaultConstructor();
        // emit basic primitive types
        first.emitPrimitiveTypedef(Type.primitive(Type.Primitive.Kind.Bool), "C_BOOL");
        first.emitPrimitiveTypedef(Type.primitive(Type.Primitive.Kind.Char), "C_CHAR");
        first.emitPrimitiveTypedef(Type.primitive(Type.Primitive.Kind.Short), "C_SHORT");
        first.emitPrimitiveTypedef(Type.primitive(Type.Primitive.Kind.Int), "C_INT");
        first.emitPrimitiveTypedef(Type.primitive(Type.Primitive.Kind.Long), "C_LONG");
        first.emitPrimitiveTypedef(Type.primitive(Type.Primitive.Kind.LongLong), "C_LONG_LONG");
        first.emitPrimitiveTypedef(Type.primitive(Type.Primitive.Kind.Float), "C_FLOAT");
        first.emitPrimitiveTypedef(Type.primitive(Type.Primitive.Kind.Double), "C_DOUBLE");
        first.emitPointerTypedef("C_POINTER");
        return first;
    }

    private static SourceFileBuilder newSourceFile(String packageName, String className) {
        SourceFileBuilder sfb = new SourceFileBuilder(packageName, className);
        sfb.emitPackagePrefix();
        sfb.emitImportSection();
        return sfb;
    }

    public List<JavaFileObject> toFiles() {
        lastHeader.classEnd();

        List<JavaFileObject> files = new ArrayList<>();
        files.add(firstHeader.sourceFileBuilder().toFile(s -> s.replace("extends #{SUPER}",
                lastHeader != firstHeader ? "extends " + lastHeader.className() : "")));
        files.addAll(builders.stream()
                .map(SourceFileBuilder::toFile).toList());
        files.addAll(constants.toFiles());
        return files;
    }

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
            SourceFileBuilder sfb = newSourceFile(packageName(), javaName);
            TypedefBuilder builder = new TypedefBuilder(sfb, sfb.className(), superClass, typedefTree);
            builders.add(sfb);
            builder.generate();
        }
    }

    @Override
    public StructBuilder addStruct(Declaration.Scoped tree, boolean isNestedAnonStruct,
        String javaName, GroupLayout layout) {
        SourceFileBuilder sfb = newSourceFile(packageName(), javaName);
        builders.add(sfb);
        StructBuilder structBuilder = new StructBuilder(sfb, constants, "public", sfb.className(), List.of(), tree, layout);
        structBuilder.begin();
        return structBuilder;
    }

    @Override
    public void addFunctionalInterface(Type.Function funcType, String javaName,
        FunctionDescriptor descriptor, Optional<List<String>> parameterNames) {
        SourceFileBuilder sfb = newSourceFile(packageName(), javaName);
        builders.add(sfb);
        FunctionalInterfaceBuilder builder = new FunctionalInterfaceBuilder(sfb, constants, "public", sfb.className(), List.of(),
                funcType, descriptor, parameterNames);
        builder.generate();
    }

    private HeaderFileBuilder nextHeader() {
        if (declCount == DECLS_PER_HEADER_CLASS) {
            boolean hasSuper = lastHeader != firstHeader;
            String className = headerDesc.displayName() + "_" + ++headersCount;
            SourceFileBuilder sfb = newSourceFile(packageName(), className);
            HeaderFileBuilder headerFileBuilder = new HeaderFileBuilder(sfb, constants, sfb.className(),
                    hasSuper ? lastHeader.className() : null);
            lastHeader.classEnd();
            headerFileBuilder.classBegin();
            builders.add(sfb);
            lastHeader = headerFileBuilder;
            declCount = 1;
            return headerFileBuilder;
        } else {
            declCount++;
            return lastHeader;
        }
    }
}
