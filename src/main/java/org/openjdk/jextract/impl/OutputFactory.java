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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/*
 * Scan a header file and generate Java source items for entities defined in that header
 * file. Tree visitor visit methods return true/false depending on whether a
 * particular Tree is processed or skipped.
 */
public class OutputFactory implements Declaration.Visitor<Void, Declaration> {
    protected final ToplevelBuilder toplevelBuilder;
    protected JavaSourceBuilder currentBuilder;
    private final String pkgName;
    private final Map<Declaration.Scoped, String> structClassNames = new HashMap<>();
    private final Set<Declaration.Typedef> unresolvedStructTypedefs = new HashSet<>();
    private final Map<Type.Delegated, String> functionTypeDefNames = new HashMap<>();
    private final NameMangler nameMangler;

    private void addStructDefinition(Declaration.Scoped decl, String name) {
        structClassNames.put(decl, name);
    }

    private boolean structDefinitionSeen(Declaration.Scoped decl) {
        return structClassNames.containsKey(decl);
    }

    private String structDefinitionName(Declaration.Scoped decl) {
        return structClassNames.get(decl);
    }

    private void addFunctionTypedef(Type.Delegated typedef, String name) {
        functionTypeDefNames.put(typedef, name);
    }

    private boolean functionTypedefSeen(Type.Delegated typedef) {
        return functionTypeDefNames.containsKey(typedef);
    }

    private String functionTypedefName(Type.Delegated decl) {
        return functionTypeDefNames.get(decl);
    }

    static JavaFileObject[] generateWrapped(Declaration.Scoped decl,
                String pkgName, List<String> libraryNames, NameMangler nameMangler) {
        String clsName = nameMangler.getJavaName(null, decl);
        ToplevelBuilder toplevelBuilder = new ToplevelBuilder(pkgName, clsName);
        return new OutputFactory(pkgName, toplevelBuilder, nameMangler).
            generate(decl, libraryNames.toArray(new String[0]));
    }

    private OutputFactory(String pkgName, ToplevelBuilder toplevelBuilder, NameMangler nameMangler) {
        this.pkgName = pkgName;
        this.toplevelBuilder = toplevelBuilder;
        this.currentBuilder = toplevelBuilder;
        this.nameMangler = nameMangler;
    }

    JavaFileObject[] generate(Declaration.Scoped decl, String[] libs) {
        //generate all decls
        decl.members().forEach(this::generateDecl);
        // check if unresolved typedefs can be resolved now!
        for (Declaration.Typedef td : unresolvedStructTypedefs) {
            Declaration.Scoped structDef = ((Type.Declared) td.type()).tree();
            toplevelBuilder.addTypedef(td, nameMangler.getJavaName(null, td), structDefinitionName(structDef));
        }
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
        /*
         * This method is called from visitVariable when it recursively visits type
         * When type is enum, enum constants are visited again! Checking parent to be
         * null to avoid duplicate generation of enum constant getter methods.
         */
        if (parent != null) {
            return null;
        }

        Class<?> clazz = getJavaType(constant.type());
        if (clazz == null) {
            warn("skipping " + constant.name() + " because of unsupported type usage");
            return null;
        }
        toplevelBuilder.addConstant(constant, nameMangler.getJavaName(parent, constant), clazz);
        return null;
    }

    @Override
    public Void visitScoped(Declaration.Scoped d, Declaration parent) {
        if (d.layout().isEmpty() || structDefinitionSeen(d)) {
            //skip decl
            return null;
        }

        boolean isStructKind = Utils.isStructOrUnion(d);
        StructBuilder structBuilder = null;
        if (isStructKind) {
            GroupLayout layout = (GroupLayout) layoutFor(d);
            boolean isNestedAnonStruct = d.name().isEmpty() &&
                (parent instanceof Declaration.Scoped);
            currentBuilder = structBuilder = currentBuilder.addStruct(
                d,
                isNestedAnonStruct,
                isNestedAnonStruct? null : nameMangler.getJavaName(parent, d),
                layout);
            structBuilder.classBegin();
            if (!d.name().isEmpty()) {
                addStructDefinition(d, structBuilder.fullName());
            }
            if (parent instanceof Declaration.Typedef) {
                addStructDefinition(d, structBuilder.fullName());
            }
        }
        try {
            d.members().forEach(fieldTree -> fieldTree.accept(this, d));
        } finally {
            if (isStructKind) {
                currentBuilder = structBuilder.classEnd();
            }
        }
        return null;
    }

    private boolean generateFunctionalInterface(Type.Function func, String javaName) {
        FunctionDescriptor descriptor = Type.descriptorFor(func).orElse(null);
        if (descriptor == null) {
            return false;
        }

        String unsupportedType = UnsupportedLayouts.firstUnsupportedType(func);
        if (unsupportedType != null) {
            warn("skipping " + javaName + " because of unsupported type usage: " +
                    unsupportedType);
            return false;
        }

        //generate functional interface
        if (func.varargs() && !func.argumentTypes().isEmpty()) {
            warn("varargs in callbacks is not supported: " + func);
            return false;
        }

        currentBuilder.addFunctionalInterface(func, javaName, descriptor,
            nameMangler.getParameterNames(func));
        return true;
    }

    @Override
    public Void visitFunction(Declaration.Function funcTree, Declaration parent) {
        FunctionDescriptor descriptor = Type.descriptorFor(funcTree.type()).orElse(null);
        if (descriptor == null) {
            return null;
        }

        //generate static wrapper for function
        String unsupportedType = UnsupportedLayouts.firstUnsupportedType(funcTree.type());
        if (unsupportedType != null) {
            warn("skipping " + funcTree.name() + " because of unsupported type usage: " +
                    unsupportedType);
            return null;
        }

        // check for function pointer type arguments
        int i = 0;
        for (Declaration.Variable param : funcTree.parameters()) {
            Type.Function f = Utils.getAsFunctionPointer(param.type());
            if (f != null) {
                String fiName = nameMangler.getFiName(funcTree, i, param);
                if (! generateFunctionalInterface(f, fiName)) {
                    return null;
                }
                i++;
            }
        }

        // return type could be a function pointer type
        Type.Function returnFunc = Utils.getAsFunctionPointer(funcTree.type().returnType());
        if (returnFunc != null) {
             if (! generateFunctionalInterface(returnFunc, nameMangler.getReturnFiName(funcTree))) {
                 return null;
             }
        }

        toplevelBuilder.addFunction(funcTree, descriptor, nameMangler.getJavaName(parent, funcTree),
            funcTree.parameters().
                stream().
                map(param -> nameMangler.getJavaName(null, param)).
                toList());

        return null;
    }

    Optional<String> getAsFunctionPointerTypedef(Type type) {
        if (type instanceof Type.Delegated delegated &&
                delegated.kind() == Type.Delegated.Kind.TYPEDEF &&
                functionTypedefSeen(delegated)) {
            return Optional.of(functionTypedefName(delegated));
        } else {
            return Optional.empty();
        }
    }

    @Override
    public Void visitTypedef(Declaration.Typedef tree, Declaration parent) {
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
                            if (structDefinitionSeen(s)) {
                                String javaName = nameMangler.getJavaName(parent, tree);
                                toplevelBuilder.addTypedef(tree, javaName, structDefinitionName(s));
                            } else {
                                /*
                                 * Definition of typedef'ed struct/union not seen yet. May be the definition comes later.
                                 * Save it to visit at the end of all declarations.
                                 */
                                unresolvedStructTypedefs.add(tree);
                            }
                        }
                    }
                    default -> visitScoped(s, tree);
                }
            }
        } else if (type instanceof Type.Primitive) {
            toplevelBuilder.addTypedef(tree, nameMangler.getJavaName(parent, tree), null);
        } else {
            Type.Function func = Utils.getAsFunctionPointer(type);
            if (func != null) {
                String fiName = nameMangler.getFiName(parent, tree);
                boolean funcIntfGen = generateFunctionalInterface(func, fiName);
                if (funcIntfGen) {
                    addFunctionTypedef(Type.typedef(tree.name(), tree.type()), fiName);
                }
            } else if (((TypeImpl)type).isPointer()) {
                toplevelBuilder.addTypedef(tree, nameMangler.getJavaName(parent, tree), null);
            } else {
                Type.Primitive primitive = Utils.getAsSignedOrUnsigned(type);
                if (primitive != null) {
                    toplevelBuilder.addTypedef(tree, nameMangler.getJavaName(parent, tree), null, primitive);
                }
            }
        }
        return null;
    }

    @Override
    public Void visitVariable(Declaration.Variable tree, Declaration parent) {
        Type type = tree.type();

        if (type instanceof Type.Declared declared) {
            // declared type - visit declaration recursively
            declared.tree().accept(this, tree);
        }

        MemoryLayout layout = tree.layout().orElse(Type.layoutFor(type).orElse(null));
        if (layout == null) {
            //no layout - abort
            return null;
        }

        if (tree.kind() == Declaration.Variable.Kind.BITFIELD ||
                (layout instanceof ValueLayout && layout.byteSize() > 8)) {
            //skip
            return null;
        }

        final String fieldName = tree.name();
        assert !fieldName.isEmpty();

        String unsupportedType = UnsupportedLayouts.firstUnsupportedType(type);
        if (unsupportedType != null) {
            String name = parent != null? parent.name() + "." : "";
            name += fieldName;
            warn("skipping " + name + " because of unsupported type usage: " +
                    unsupportedType);
            return null;
        }

        Class<?> clazz = getJavaType(type);
        if (clazz == null) {
            String name = parent != null? parent.name() + "." : "";
            name += fieldName;
            warn("skipping " + name + " because of unsupported type usage");
            return null;
        }


        Type.Function func = Utils.getAsFunctionPointer(type);
        String fiName = null;
        if (func != null) {
            fiName = nameMangler.getFiName(parent, tree);
            if (! generateFunctionalInterface(func, fiName)) {
                fiName = null;
            }
        } else {
            Optional<String> funcTypedef = getAsFunctionPointerTypedef(type);
            if (funcTypedef.isPresent()) {
                fiName = funcTypedef.get();
            }
        }

        currentBuilder.addVar(tree, nameMangler.getJavaName(parent, tree), layout, Optional.ofNullable(fiName));

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

    static void warn(String msg) {
        System.err.println("WARNING: " + msg);
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
}
