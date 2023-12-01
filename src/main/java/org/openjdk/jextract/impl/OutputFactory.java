/*
 * Copyright (c) 2020, 2022 Oracle and/or its affiliates. All rights reserved.
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

import java.lang.foreign.*;
import org.openjdk.jextract.Declaration;
import org.openjdk.jextract.Type;
import org.openjdk.jextract.Type.Function;
import org.openjdk.jextract.impl.DeclarationImpl.JavaFunctionalInterfaceName;
import org.openjdk.jextract.impl.DeclarationImpl.JavaName;
import org.openjdk.jextract.impl.DeclarationImpl.Skip;

import javax.tools.JavaFileObject;
import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.net.URL;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/*
 * Scan a header file and generate Java source items for entities defined in that header
 * file. Tree visitor visit methods return true/false depending on whether a
 * particular Tree is processed or skipped.
 */
public class OutputFactory implements Declaration.Visitor<Void, Declaration> {
    protected final ToplevelBuilder toplevelBuilder;
    protected Builder currentBuilder;
    private final String pkgName;

    static JavaFileObject[] generateWrapped(Declaration.Scoped decl,
                String pkgName, List<String> libraryNames) {
        String clsName = JavaName.getOrThrow(decl);
        ToplevelBuilder toplevelBuilder = new ToplevelBuilder(pkgName, clsName);
        return new OutputFactory(pkgName, toplevelBuilder).
            generate(decl, libraryNames.toArray(new String[0]));
    }

    private OutputFactory(String pkgName, ToplevelBuilder toplevelBuilder) {
        this.pkgName = pkgName;
        this.toplevelBuilder = toplevelBuilder;
        this.currentBuilder = toplevelBuilder;
    }

    JavaFileObject[] generate(Declaration.Scoped decl, String[] libs) {
        //generate all decls
        decl.members().forEach(this::generateDecl);
        try {
            List<JavaFileObject> files = new ArrayList<>(toplevelBuilder.toFiles());
            files.add(jfoFromString(pkgName,"RuntimeHelper", getRuntimeHelperSource(libs)));
            return files.toArray(new JavaFileObject[0]);
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        } catch (URISyntaxException ex2) {
            throw new RuntimeException(ex2);
        }
    }

    private String getRuntimeHelperSource(String[] libraries) throws URISyntaxException, IOException {
        URL runtimeHelper = OutputFactory.class.getResource("resources/RuntimeHelper.java.template");
        String template = (pkgName.isEmpty()? "" : "package " + pkgName + ";\n") +
                        String.join("\n", Files.readAllLines(Paths.get(runtimeHelper.toURI())));
        List<String> loadLibrariesStr = new ArrayList<>();
        for (String lib : libraries) {
            String quotedLibName = quoteLibraryName(lib);
            if (quotedLibName.indexOf(File.separatorChar) != -1) {
                loadLibrariesStr.add("System.load(\"" + quotedLibName + "\");");
            } else {
                loadLibrariesStr.add("System.loadLibrary(\"" + quotedLibName + "\");");
            }
        }
        return template.replace("#LOAD_LIBRARIES#", loadLibrariesStr.stream().collect(Collectors.joining(" ")));
    }

    private String quoteLibraryName(String lib) {
        return lib.replace("\\", "\\\\"); // double up slashes
    }

    private void generateDecl(Declaration tree) {
        try {
            tree.accept(this, null);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private JavaFileObject jfoFromString(String pkgName, String clsName, String contents) {
        String pkgPrefix = pkgName.isEmpty() ? "" : pkgName.replaceAll("\\.", "/") + "/";
        return InMemoryJavaCompiler.jfoFromString(URI.create(pkgPrefix + clsName + ".java"), contents);
    }

    @Override
    public Void visitConstant(Declaration.Constant constant, Declaration parent) {
        if (Skip.isPresent(constant)) {
            return null;
        }

        Class<?> clazz = getJavaType(constant.type());
        toplevelBuilder.addConstant(constant, clazz);
        return null;
    }

    @Override
    public Void visitScoped(Declaration.Scoped d, Declaration parent) {
        if (Skip.isPresent(d)) {
            return null;
        }

        Skip.with(d); // do not generate twice
        boolean isStructKind = Utils.isStructOrUnion(d);
        Builder prevBuilder = null;
        StructBuilder structBuilder = null;
        if (isStructKind) {
            GroupLayout layout = (GroupLayout) layoutFor(d);
            prevBuilder = currentBuilder;
            currentBuilder = structBuilder = currentBuilder.addStruct(
                d,
                layout);
        }
        try {
            d.members().forEach(fieldTree -> fieldTree.accept(this, d));
        } finally {
            if (isStructKind) {
                structBuilder.end();
                currentBuilder = prevBuilder;
            }
        }
        return null;
    }

    private boolean generateFunctionalInterface(String name, Type.Function func) {
        if (Skip.isPresent(func)) return false;
        FunctionDescriptor descriptor = Type.descriptorFor(func).get();
        currentBuilder.addFunctionalInterface(name, func, descriptor);
        return true;
    }

    @Override
    public Void visitFunction(Declaration.Function funcTree, Declaration parent) {
        if (Skip.isPresent(funcTree)) {
            return null;
        }

        // check for function pointer type arguments
        for (Declaration.Variable param : funcTree.parameters()) {
            Type.Function f = Utils.getAsFunctionPointer(param.type());
            if (f != null) {
                if (! generateFunctionalInterface(JavaFunctionalInterfaceName.getOrThrow(param), f)) {
                    return null;
                }
            }
        }

        // return type could be a function pointer type
        Type.Function returnFunc = Utils.getAsFunctionPointer(funcTree.type().returnType());
        if (returnFunc != null) {
             if (! generateFunctionalInterface(JavaFunctionalInterfaceName.getOrThrow(funcTree), returnFunc)) {
                 return null;
             }
        }

        FunctionDescriptor descriptor = Type.descriptorFor(funcTree.type()).get();
        toplevelBuilder.addFunction(funcTree, descriptor);
        return null;
    }

    Function getAsFunctionPointerTypedef(Type type) {
        if (type instanceof Type.Delegated delegated &&
                delegated.kind() == Type.Delegated.Kind.TYPEDEF) {
            return Utils.getAsFunctionPointer(delegated.type());
        } else {
            return null;
        }
    }

    @Override
    public Void visitTypedef(Declaration.Typedef tree, Declaration parent) {
        if (Skip.isPresent(tree)) {
            return null;
        }
        Type type = tree.type();
        if (type instanceof Type.Declared declared) {
            Declaration.Scoped s = declared.tree();
            if (!s.name().equals(tree.name())) {
                switch (s.kind()) {
                    case STRUCT, UNION -> {
                        if (s.name().isEmpty()) {
                            visitScoped(s, tree);
                        } else {
                            /*
                             * If typedef is seen after the struct/union definition, we can generate subclass
                             * right away. If not, we've to save it and revisit after all the declarations are
                             * seen. This is to support forward declaration of typedefs.
                             *
                             * typedef struct Foo Bar;
                             *
                             * struct Foo {
                             *     int x, y;
                             * };
                             */
                            toplevelBuilder.addTypedef(tree, s.layout().isEmpty() ? null : JavaName.getFullNameOrThrow(s));
                        }
                    }
                    default -> visitScoped(s, tree);
                }
            }
        } else if (type instanceof Type.Primitive) {
            toplevelBuilder.addTypedef(tree, null);
        } else {
            Type.Function func = Utils.getAsFunctionPointer(type);
            if (func != null) {
                generateFunctionalInterface(JavaFunctionalInterfaceName.getOrThrow(tree), func);
            } else if (((TypeImpl)type).isPointer()) {
                toplevelBuilder.addTypedef(tree, null);
            } else {
                Type.Primitive primitive = Utils.getAsSignedOrUnsigned(type);
                if (primitive != null) {
                    toplevelBuilder.addTypedef(tree, null, primitive);
                }
            }
        }
        return null;
    }

    @Override
    public Void visitVariable(Declaration.Variable tree, Declaration parent) {
        if (Skip.isPresent(tree)) {
            return null;
        }
        Type type = tree.type();

        if (type instanceof Type.Declared declared) {
            // declared type - visit declaration recursively
            declared.tree().accept(this, tree);
        }

        final String fieldName = tree.name();
        assert !fieldName.isEmpty();

        Optional<String> fiName = JavaFunctionalInterfaceName.get(tree);
        Type.Function func = Utils.getAsFunctionPointer(type);
        if (func != null) {
            generateFunctionalInterface(fiName.get(), func);
        }

        MemoryLayout layout = Type.layoutFor(type).get();
        currentBuilder.addVar(tree, layout, fiName);
        return null;
    }

    protected static MemoryLayout layoutFor(Declaration decl) {
        if (decl instanceof Declaration.Typedef alias) {
            return Type.layoutFor(alias.type()).orElseThrow();
        } else if (decl instanceof Declaration.Scoped scoped) {
            return scoped.layout().orElseThrow();
        } else {
            throw new IllegalArgumentException("Unexpected parent declaration");
        }
        // case like `typedef struct { ... } Foo`
    }

    private Class<?> getJavaType(Type type) {
        Optional<MemoryLayout> layout = Type.layoutFor(type);
        if (!layout.isPresent()) return null;
        if (layout.get() instanceof SequenceLayout || layout.get() instanceof GroupLayout) {
            return MemorySegment.class;
        } else if (layout.get() instanceof ValueLayout valueLayout) {
            return valueLayout.carrier();
        } else {
            return null;
        }
    }

    interface Builder {

        default void addVar(Declaration.Variable varTree, MemoryLayout layout, Optional<String> fiName) {
            throw new UnsupportedOperationException("Not implemented");
        }

        default void addFunction(Declaration.Function funcTree, FunctionDescriptor descriptor) {
            throw new UnsupportedOperationException("Not implemented");
        }

        default void addConstant(Declaration.Constant constantTree, Class<?> javaType) {
            throw new UnsupportedOperationException("Not implemented");
        }

        default void addTypedef(Declaration.Typedef typedefTree, String superClass) {
            addTypedef(typedefTree, superClass, typedefTree.type());
        }

        default void addTypedef(Declaration.Typedef typedefTree, String superClass, Type type) {
            throw new UnsupportedOperationException("Not implemented");
        }

        default StructBuilder addStruct(Declaration.Scoped structTree, GroupLayout layout) {
            throw new UnsupportedOperationException("Not implemented");
        }

        default void addFunctionalInterface(String name, Type.Function funcType, FunctionDescriptor descriptor) {
            throw new UnsupportedOperationException("Not implemented");
        }
    }
}
