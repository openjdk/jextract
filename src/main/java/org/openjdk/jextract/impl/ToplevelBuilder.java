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
import org.openjdk.jextract.impl.DeclarationImpl.JavaName;

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
class ToplevelBuilder implements OutputFactory.Builder {
    private static final int DECLS_PER_HEADER_CLASS = Integer.getInteger("jextract.decls.per.header", 1000);

    private int declCount;
    private final List<SourceFileBuilder> builders = new ArrayList<>();
    private final HeaderFileBuilder firstHeader;
    private HeaderFileBuilder lastHeader;
    private int headersCount;
    private final ClassDesc headerDesc;

    ToplevelBuilder(String packageName, String headerClassName, List<String> libraries) {
        this.headerDesc = ClassDesc.of(packageName, headerClassName);
        SourceFileBuilder sfb = SourceFileBuilder.newSourceFile(packageName, headerClassName);
        lastHeader = firstHeader = createFirstHeader(sfb, libraries);
    }

    private static HeaderFileBuilder createFirstHeader(SourceFileBuilder sfb, List<String> libraries) {
        HeaderFileBuilder first = new HeaderFileBuilder(sfb, sfb.className(), "#{SUPER}", sfb.className());
        first.classBegin();
        first.emitFirstHeaderPreamble(libraries);
        // emit basic primitive types
        first.emitPrimitiveTypedef(Type.primitive(Type.Primitive.Kind.Bool), "C_BOOL");
        first.emitPrimitiveTypedef(Type.primitive(Type.Primitive.Kind.Char), "C_CHAR");
        first.emitPrimitiveTypedef(Type.primitive(Type.Primitive.Kind.Short), "C_SHORT");
        first.emitPrimitiveTypedef(Type.primitive(Type.Primitive.Kind.Int), "C_INT");
        first.emitPrimitiveTypedef(Type.primitive(Type.Primitive.Kind.Long), "C_LONG");
        first.emitPrimitiveTypedef(Type.primitive(Type.Primitive.Kind.LongLong), "C_LONG_LONG");
        first.emitPrimitiveTypedef(Type.primitive(Type.Primitive.Kind.Float), "C_FLOAT");
        first.emitPrimitiveTypedef(Type.primitive(Type.Primitive.Kind.Double), "C_DOUBLE");

        // we don't use 'emitPrimitiveTypedef' so we can attach the target layout
        first.appendIndentedLines("""
            public static final AddressLayout C_POINTER = ValueLayout.ADDRESS
                    .withTargetLayout(MemoryLayout.sequenceLayout(java.lang.Long.MAX_VALUE, JAVA_BYTE));
            """);
        return first;
    }

    public List<JavaFileObject> toFiles() {
        boolean hasOneHeader = lastHeader == firstHeader;
        if (hasOneHeader) {
            firstHeader.emitRuntimeHelperMethods();
        }
        lastHeader.classEnd();

        List<JavaFileObject> files = new ArrayList<>();
        files.add(firstHeader.sourceFileBuilder().toFile(s -> s.replace("extends #{SUPER}",
                hasOneHeader ? "" : "extends " + lastHeader.className())));
        files.addAll(builders.stream()
                .map(SourceFileBuilder::toFile).toList());
        return files;
    }

    public String packageName() {
        return headerDesc.packageName();
    }

    @Override
    public void addVar(Declaration.Variable varTree, MemoryLayout layout, Optional<String> fiName) {
        nextHeader().addVar(varTree, layout, fiName);
    }

    @Override
    public void addFunction(Declaration.Function funcTree, FunctionDescriptor descriptor) {
        nextHeader().addFunction(funcTree, descriptor);
    }

    @Override
    public void addConstant(Declaration.Constant constantTree, Class<?> javaType) {
        nextHeader().addConstant(constantTree, javaType);
    }

    @Override
    public void addTypedef(Declaration.Typedef typedefTree, String superClass, Type type) {
        String javaName = JavaName.getOrThrow(typedefTree);
        if (type instanceof Type.Primitive primitive) {
            // primitive
            nextHeader().emitPrimitiveTypedef(typedefTree, primitive, javaName);
        } else if (((TypeImpl)type).isPointer()) {
            // pointer typedef
            nextHeader().emitPointerTypedef(typedefTree, javaName);
        } else {
            SourceFileBuilder sfb = SourceFileBuilder.newSourceFile(packageName(), javaName);
            TypedefBuilder.generate(sfb, sfb.className(), superClass, firstHeader.className(), typedefTree);
            builders.add(sfb);
        }
    }

    @Override
    public StructBuilder addStruct(Declaration.Scoped tree, GroupLayout layout) {
        SourceFileBuilder sfb = SourceFileBuilder.newSourceFile(packageName(), JavaName.getOrThrow(tree));
        builders.add(sfb);
        StructBuilder structBuilder = new StructBuilder(sfb, "public", sfb.className(), null, firstHeader.className(), tree, layout);
        structBuilder.begin();
        return structBuilder;
    }

    @Override
    public void addFunctionalInterface(String name, Type.Function funcType, FunctionDescriptor descriptor) {
        SourceFileBuilder sfb = SourceFileBuilder.newSourceFile(packageName(), name);
        builders.add(sfb);
        FunctionalInterfaceBuilder.generate(sfb, sfb.className(), null, firstHeader.className(), funcType, descriptor,
                funcType.parameterNames().map(NameMangler::javaSafeIdentifiers));
    }

    private HeaderFileBuilder nextHeader() {
        if (declCount == DECLS_PER_HEADER_CLASS) {
            boolean wasFirstHeader = lastHeader == firstHeader;
            if (wasFirstHeader) {
                firstHeader.emitRuntimeHelperMethods();
            }
            String className = headerDesc.displayName() + "_" + ++headersCount;
            SourceFileBuilder sfb = SourceFileBuilder.newSourceFile(packageName(), className);
            HeaderFileBuilder headerFileBuilder = new HeaderFileBuilder(sfb, sfb.className(),
                    wasFirstHeader ? null : lastHeader.className(), firstHeader.className());
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
