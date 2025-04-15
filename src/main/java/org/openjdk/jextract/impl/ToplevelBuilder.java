/*
 * Copyright (c) 2020, 2025, Oracle and/or its affiliates. All rights reserved.
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
import org.openjdk.jextract.JavaSourceFile;
import org.openjdk.jextract.Type;
import org.openjdk.jextract.impl.DeclarationImpl.JavaFunctionalInterfaceName;
import org.openjdk.jextract.impl.DeclarationImpl.JavaName;

import java.lang.constant.ClassDesc;
import java.util.ArrayList;
import java.util.List;

/**
 * A helper class to generate header interface class in source form.
 * After aggregating various constituents of a .java source, build
 * method is called to get overall generated source string.
 */
class ToplevelBuilder implements OutputFactory.Builder {
    private static final int DECLS_PER_HEADER_CLASS = Integer.getInteger("jextract.decls.per.header", 1000);
    public static final String PREV_SUFFIX = "#{PREV_SUFFIX}";
    private static final String SUFFIX = "#{SUFFIX}";
    private static String SHARED;

    private int declCount;
    private final List<SourceFileBuilder> headerBuilders = new ArrayList<>();
    private final List<SourceFileBuilder> otherBuilders = new ArrayList<>();
    private HeaderFileBuilder lastHeader;
    private final ClassDesc headerDesc;

    ToplevelBuilder(String packageName, String headerClassName,
                    List<Options.Library> libs, boolean useSystemLoadLibrary, String sharedSymbolsFile) {
        this.headerDesc = ClassDesc.of(packageName, headerClassName);
        SHARED = sharedSymbolsFile != null ? sharedSymbolsFile : headerDesc.displayName() + "$shared";
        createSharedClass();
        SourceFileBuilder sfb = SourceFileBuilder.newSourceFile(packageName, headerClassName);
        headerBuilders.add(sfb);
        lastHeader = createFirstHeader(sfb, libs, useSystemLoadLibrary);
    }

    private void createSharedClass() {
        SourceFileBuilder sfb = SourceFileBuilder.newSourceFile(packageName(), SHARED);
        HeaderFileBuilder sharedBuilder = new HeaderFileBuilder(sfb, SHARED, null, SHARED);
        sharedBuilder.appendBlankLine();
        sharedBuilder.classBegin();
        sharedBuilder.emitDefaultConstructor();
        sharedBuilder.emitBasicPrimitiveTypes();
        sharedBuilder.classEnd();
        otherBuilders.add(sfb);
    }

    private static HeaderFileBuilder createFirstHeader(SourceFileBuilder sfb, List<Options.Library> libs, boolean useSystemLoadLibrary) {
        HeaderFileBuilder first = new HeaderFileBuilder(sfb, String.format("%1$s#{SUFFIX}",sfb.className()), SHARED, sfb.className());
        first.appendBlankLine();
        first.classBegin();
        first.emitDefaultConstructor();
        first.emitRuntimeHelperMethods();
        first.emitFirstHeaderPreamble(libs, useSystemLoadLibrary);
        return first;
    }

    public List<JavaSourceFile> toFiles() {
        lastHeader.classEnd();

        List<JavaSourceFile> files = new ArrayList<>();

        if (headerBuilders.size() == 1) {
            files.add(headerBuilders.getFirst().toFile(s -> s.replace(SUFFIX, "")));
        } else {
            // adjust suffixes so that the last header class becomes the main header class,
            // and extends all the other header classes
            int totalHeaders = headerBuilders.size();
            for (int i = 0; i < totalHeaders; i++) {
                SourceFileBuilder header = headerBuilders.get(i);
                boolean isMainHeader = (i == totalHeaders - 1); // last header is the main header
                String currentSuffix = isMainHeader ?
                        "" : // main header class, drop the suffix
                        String.format("_%d", totalHeaders - i - 1);
                String preSuffix = String.format("_%d", totalHeaders - i);
                String className = headerBuilders.getFirst().className();
                String modifier = isMainHeader ? "public " : "";

                files.add(header.toFile(currentSuffix, s ->
                        s.replace("public class " + className, modifier + "class " + className)
                                .replace(SUFFIX, currentSuffix)
                                .replace(PREV_SUFFIX, preSuffix)
                ));
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
        FunctionalInterfaceBuilder.generate(sfb, sfb.className(), null, mainHeaderClassName(), parentDecl, funcType, false);
    }

    private HeaderFileBuilder nextHeader() {
        if (declCount == DECLS_PER_HEADER_CLASS) {
            SourceFileBuilder sfb = SourceFileBuilder.newSourceFile(packageName(), mainHeaderClassName());
            String className = mainHeaderClassName() + SUFFIX;
            HeaderFileBuilder headerFileBuilder = new HeaderFileBuilder(sfb, className,
                    mainHeaderClassName() + PREV_SUFFIX, mainHeaderClassName());
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
