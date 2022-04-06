/*
 * Copyright (c) 2020, Oracle and/or its affiliates. All rights reserved.
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

import jdk.incubator.foreign.*;
import org.openjdk.jextract.Declaration;
import org.openjdk.jextract.Type;

import org.openjdk.jextract.impl.JavaSourceBuilder.VarInfo;
import org.openjdk.jextract.impl.JavaSourceBuilder.FunctionInfo;

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
    // internal symbol used by clang for "va_list"
    private static final String VA_LIST_TAG = "__va_list_tag";
    private final Set<String> constants = new HashSet<>();
    // To detect duplicate Variable and Function declarations.
    private final Set<String> variables = new HashSet<>();
    private final Set<Declaration.Function> functions = new HashSet<>();

    protected final ToplevelBuilder toplevelBuilder;
    protected JavaSourceBuilder currentBuilder;
    private final String pkgName;
    private final Map<Declaration, String> structClassNames = new HashMap<>();
    private final Set<Declaration.Typedef> unresolvedStructTypedefs = new HashSet<>();
    private final Map<Type, String> functionTypeDefNames = new HashMap<>();
    private final IncludeHelper includeHelper;

    private void addStructDefinition(Declaration decl, String name) {
        structClassNames.put(decl, name);
    }

    private boolean structDefinitionSeen(Declaration decl) {
        return structClassNames.containsKey(decl);
    }

    private String structDefinitionName(Declaration decl) {
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

    // have we seen this Variable earlier?
    protected boolean variableSeen(Declaration.Variable tree) {
        return !variables.add(tree.name());
    }

    // have we seen this Function earlier?
    protected boolean functionSeen(Declaration.Function tree) {
        return !functions.add(tree);
    }

    public static JavaFileObject[] generateWrapped(Declaration.Scoped decl, String headerName,
                String pkgName, IncludeHelper includeHelper, List<String> libraryNames) {
        String clsName = Utils.javaSafeIdentifier(headerName.replace(".h", "_h"), true);
        ToplevelBuilder toplevelBuilder = new ToplevelBuilder(pkgName, clsName);
        return new OutputFactory(pkgName, toplevelBuilder, includeHelper).generate(decl, libraryNames.toArray(new String[0]));
    }

    private OutputFactory(String pkgName, ToplevelBuilder toplevelBuilder, IncludeHelper includeHelper) {
        this.pkgName = pkgName;
        this.toplevelBuilder = toplevelBuilder;
        this.currentBuilder = toplevelBuilder;
        this.includeHelper = includeHelper;
    }

    JavaFileObject[] generate(Declaration.Scoped decl, String[] libs) {
        //generate all decls
        decl.members().forEach(this::generateDecl);
        // check if unresolved typedefs can be resolved now!
        for (Declaration.Typedef td : unresolvedStructTypedefs) {
            Declaration.Scoped structDef = ((Type.Declared) td.type()).tree();
            toplevelBuilder.addTypedef(td.name(),
                    structDefinitionSeen(structDef) ? structDefinitionName(structDef) : null, td.type());
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
        if (!constants.add(constant.name()) || !includeHelper.isIncluded(constant)) {
            //skip
            return null;
        }

        Class<?> clazz = getJavaType(constant.type());
        if (clazz == null) {
            warn("skipping " + constant.name() + " because of unsupported type usage");
            return null;
        }
        toplevelBuilder.addConstant(Utils.javaSafeIdentifier(constant.name()),
                constant.value() instanceof String ? MemorySegment.class :
                clazz, constant.value());
        return null;
    }

    @Override
    public Void visitScoped(Declaration.Scoped d, Declaration parent) {
        if (d.layout().isEmpty() || structDefinitionSeen(d)) {
            //skip decl
            return null;
        }
        boolean isStructKind = switch (d.kind()) {
            case STRUCT, UNION -> true;
            default -> false;
        };
        StructBuilder structBuilder = null;
        if (isStructKind) {
            String className = d.name();
            if (!className.isEmpty() && !includeHelper.isIncluded(d)) {
                return null;
            }
            GroupLayout layout = (GroupLayout) layoutFor(d);
            currentBuilder = structBuilder = currentBuilder.addStruct(className, parent, layout, Type.declared(d));
            structBuilder.classBegin();
            if (!className.isEmpty()) {
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

    private String generateFunctionalInterface(Type.Function func, String name) {
        FunctionDescriptor descriptor = Type.descriptorFor(func).orElse(null);
        if (descriptor == null) {
            return null;
        }

        String unsupportedType = UnsupportedLayouts.firstUnsupportedType(func);
        if (unsupportedType != null) {
            warn("skipping " + name + " because of unsupported type usage: " +
                    unsupportedType);
            return null;
        }

        //generate functional interface
        if (func.varargs() && !func.argumentTypes().isEmpty()) {
            warn("varargs in callbacks is not supported: " + func);
            return null;
        }

        FunctionInfo fInfo = FunctionInfo.ofFunctionPointer(
                CLinker.upcallType(descriptor),
                CLinker.downcallType(descriptor),
                descriptor,
                func.parameterNames()
        );
        return currentBuilder.addFunctionalInterface(Utils.javaSafeIdentifier(name), fInfo);
    }

    @Override
    public Void visitFunction(Declaration.Function funcTree, Declaration parent) {
        if (functionSeen(funcTree) ||
                !includeHelper.isIncluded(funcTree)) {
            return null;
        }

        String mhName = Utils.javaSafeIdentifier(funcTree.name());
        //generate static wrapper for function
        List<String> paramNames = funcTree.parameters()
                                          .stream()
                                          .map(Declaration.Variable::name)
                                          .map(p -> !p.isEmpty() ? Utils.javaSafeIdentifier(p) : p)
                                          .collect(Collectors.toList());

        FunctionDescriptor descriptor = Type.descriptorFor(funcTree.type()).orElse(null);
        if (descriptor == null) {
            return null;
        }

        String unsupportedType = UnsupportedLayouts.firstUnsupportedType(funcTree.type());
        if (unsupportedType != null) {
            warn("skipping " + funcTree.name() + " because of unsupported type usage: " +
                    unsupportedType);
            return null;
        }

        FunctionInfo fInfo = FunctionInfo.ofFunction(
                CLinker.downcallType(descriptor),
                descriptor,
                funcTree.type().varargs(),
                paramNames
        );

        int i = 0;
        for (Declaration.Variable param : funcTree.parameters()) {
            Type.Function f = getAsFunctionPointer(param.type());
            if (f != null) {
                String name = funcTree.name() + "$" + (param.name().isEmpty() ? "x" + i : param.name());
                if (generateFunctionalInterface(f, name) == null) {
                    return null;
                }
                i++;
            }
        }

        toplevelBuilder.addFunction(mhName, funcTree.name(), fInfo);
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

    Type.Function getAsFunctionPointer(Type type) {
        if (type instanceof Type.Function function) {
            /*
             * // pointer to function declared as function like this
             *
             * typedef void CB(int);
             * void func(CB cb);
             */
            return function;
        } else if (Utils.isPointerType(type)) {
            return getAsFunctionPointer(((Type.Delegated)type).type());
        } else {
            return null;
        }
    }

    @Override
    public Void visitTypedef(Declaration.Typedef tree, Declaration parent) {
        if (!includeHelper.isIncluded(tree)) {
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
                            if (structDefinitionSeen(s)) {
                                toplevelBuilder.addTypedef(tree.name(), structDefinitionName(s), tree.type());
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
             toplevelBuilder.addTypedef(tree.name(), null, type);
        } else {
            Type.Function func = getAsFunctionPointer(type);
            if (func != null) {
                String funcIntfName = generateFunctionalInterface(func, tree.name());
                if (funcIntfName != null) {
                    addFunctionTypedef(Type.typedef(tree.name(), tree.type()), funcIntfName);
                }
            } else if (((TypeImpl)type).isPointer()) {
                toplevelBuilder.addTypedef(tree.name(), null, type);
            }
        }
        return null;
    }

    @Override
    public Void visitVariable(Declaration.Variable tree, Declaration parent) {
        if (parent == null && (variableSeen(tree) || !includeHelper.isIncluded(tree))) {
            return null;
        }

        String fieldName = tree.name();
        String symbol = tree.name();
        assert !symbol.isEmpty();
        assert !fieldName.isEmpty();
        fieldName = Utils.javaSafeIdentifier(fieldName);

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


        VarInfo varInfo = VarInfo.ofVar(clazz, layout);
        Type.Function func = getAsFunctionPointer(type);
        String fiName;
        if (func != null) {
            fiName = generateFunctionalInterface(func, fieldName);
            if (fiName != null) {
                varInfo = VarInfo.ofFunctionalPointerVar(clazz, layout, fiName);
            }
        } else {
            Optional<String> funcTypedef = getAsFunctionPointerTypedef(type);
            if (funcTypedef.isPresent()) {
                varInfo = VarInfo.ofFunctionalPointerVar(clazz, layout, Utils.javaSafeIdentifier(funcTypedef.get()));
            }
        }

        if (tree.kind() == Declaration.Variable.Kind.BITFIELD ||
                (layout instanceof ValueLayout && layout.byteSize() > 8)) {
            //skip
            return null;
        }

        boolean sizeAvailable;
        try {
            layout.byteSize();
            sizeAvailable = true;
        } catch (Exception ignored) {
            sizeAvailable = false;
        }
        if (sizeAvailable) {
            currentBuilder.addVar(fieldName, tree.name(), varInfo);
        } else {
            warn("Layout size not available for " + fieldName);
        }

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
