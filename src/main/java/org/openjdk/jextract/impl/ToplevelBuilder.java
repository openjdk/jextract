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

import org.openjdk.jextract.Declaration;
import org.openjdk.jextract.Type;
import org.openjdk.jextract.impl.DeclarationImpl.JavaFunctionalInterfaceName;
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
    private final List<SourceFileBuilder> headerBuilders = new ArrayList<>();
    private final List<SourceFileBuilder> otherBuilders = new ArrayList<>();
    private HeaderFileBuilder lastHeader;
    private final ClassDesc headerDesc;

    ToplevelBuilder(String packageName, String headerClassName, List<String> libraries) {
        this.headerDesc = ClassDesc.of(packageName, headerClassName);
        SourceFileBuilder sfb = SourceFileBuilder.newSourceFile(packageName, headerClassName);
        headerBuilders.add(sfb);
        lastHeader = createFirstHeader(sfb, libraries);
    }

    private static HeaderFileBuilder createFirstHeader(SourceFileBuilder sfb, List<String> libraries) {
        HeaderFileBuilder first = new HeaderFileBuilder(sfb, STR."\{sfb.className()}#{SUFFIX}", null, sfb.className());
        first.appendBlankLine();
        first.classBegin();
        first.emitFirstHeaderPreamble(libraries);
        first.emitDefaultConstructor();
        // emit basic primitive types
        first.appendIndentedLines(STR."""

            public static final ValueLayout.OfBoolean C_BOOL = ValueLayout.JAVA_BOOLEAN;
            public static final ValueLayout.OfByte C_CHAR = ValueLayout.JAVA_BYTE;
            public static final ValueLayout.OfShort C_SHORT = ValueLayout.JAVA_SHORT;
            public static final ValueLayout.OfInt C_INT = ValueLayout.JAVA_INT;
            public static final ValueLayout.OfLong C_LONG_LONG = ValueLayout.JAVA_LONG;
            public static final ValueLayout.OfFloat C_FLOAT = ValueLayout.JAVA_FLOAT;
            public static final ValueLayout.OfDouble C_DOUBLE = ValueLayout.JAVA_DOUBLE;
            public static final AddressLayout C_POINTER = ValueLayout.ADDRESS
                    .withTargetLayout(MemoryLayout.sequenceLayout(java.lang.Long.MAX_VALUE, JAVA_BYTE));
            """);
        if (TypeImpl.IS_WINDOWS) {
            first.appendIndentedLines("public static final ValueLayout.OfInt C_LONG = ValueLayout.JAVA_INT;");
            first.appendIndentedLines("public static final ValueLayout.OfDouble C_LONG_DOUBLE = ValueLayout.JAVA_DOUBLE;");
        } else {
            first.appendIndentedLines("public static final ValueLayout.OfLong C_LONG = ValueLayout.JAVA_LONG;");
        }
        first.emitRuntimeHelperMethods();
        return first;
    }

    public List<JavaFileObject> toFiles() {
        lastHeader.classEnd();

        List<JavaFileObject> files = new ArrayList<>();

        if (headerBuilders.size() == 1) {
            files.add(headerBuilders.get(0).toFile(s -> s.replace("#{SUFFIX}", "")));
        } else {
            // adjust suffixes so that the last header class becomes the main header class,
            // and extends all the other header classes
            int suffix = headerBuilders.size() - 1;
            for (SourceFileBuilder header : headerBuilders) {
                String currentSuffix = suffix == 0 ?
                        "" : // main header class, drop the suffix
                        STR."_\{suffix}";
                String prevSuffix = STR."_\{suffix + 1}";
                files.add(header.toFile(currentSuffix,
                        s -> s.replace("#{SUFFIX}", currentSuffix)
                              .replace("#{PREV_SUFFIX}", prevSuffix)));
                suffix--;
            }
        }
        // add remaining builders
        files.addAll(otherBuilders.stream()
                .map(SourceFileBuilder::toFile).toList());
        return files;
    }

    public String mainHeaderClassName() {
        return headerDesc.displayName();
    }

    public String packageName() {
        return headerDesc.packageName();
    }

    @Override
    public void addVar(Declaration.Variable varTree) {
        nextHeader().addVar(varTree);
    }

    @Override
    public void addFunction(Declaration.Function funcTree) {
        nextHeader().addFunction(funcTree);
    }

    @Override
    public void addConstant(Declaration.Constant constantTree) {
        nextHeader().addConstant(constantTree);
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
            TypedefBuilder.generate(sfb, sfb.className(), superClass, mainHeaderClassName(), typedefTree);
            otherBuilders.add(sfb);
        }
    }

    @Override
    public StructBuilder addStruct(Declaration.Scoped tree) {
        SourceFileBuilder sfb = SourceFileBuilder.newSourceFile(packageName(), JavaName.getOrThrow(tree));
        otherBuilders.add(sfb);
        StructBuilder structBuilder = new StructBuilder(sfb, "public", sfb.className(), null, mainHeaderClassName(), tree);
        structBuilder.begin();
        return structBuilder;
    }

    @Override
    public void addFunctionalInterface(Declaration parentDecl, Type.Function funcType) {
        SourceFileBuilder sfb = SourceFileBuilder.newSourceFile(packageName(), JavaFunctionalInterfaceName.getOrThrow(parentDecl));
        otherBuilders.add(sfb);
        FunctionalInterfaceBuilder.generate(sfb, sfb.className(), null, mainHeaderClassName(), parentDecl, funcType);
    }

    private HeaderFileBuilder nextHeader() {
        if (declCount == DECLS_PER_HEADER_CLASS) {
            SourceFileBuilder sfb = SourceFileBuilder.newSourceFile(packageName(), mainHeaderClassName());
            String className = mainHeaderClassName() + "#{SUFFIX}";
            HeaderFileBuilder headerFileBuilder = new HeaderFileBuilder(sfb, className,
                    mainHeaderClassName() + "#{PREV_SUFFIX}", mainHeaderClassName());
            lastHeader.classEnd();
            headerFileBuilder.appendBlankLine();
            headerFileBuilder.classBegin();
            headerFileBuilder.emitDefaultConstructor();
            headerBuilders.add(sfb);
            lastHeader = headerFileBuilder;
            declCount = 1;
            return headerFileBuilder;
        } else {
            declCount++;
            return lastHeader;
        }
    }
}
