/*
 * Copyright (c) 2022 Oracle and/or its affiliates. All rights reserved.
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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import javax.lang.model.SourceVersion;

/*
 * This visitor handles java safe names for identifiers, type names. This visitor
 * stores java safe names in maps. Subsequent code generation steps can check for
 * java safe names via lookup methods.
 *
 * NOTE: Unlike other transforming tree visitors, this visitor collects name
 * mappings as it visits tree nodes. Subsequent code generation steps can check
 * the collected names using getters.
 */
final class NameMangler implements Declaration.Visitor<Void, Declaration> {
    private final String headerName;

    private static class Scope {
         private Scope parent;
         private String className;
         private Set<String> nestedClassNames = new HashSet<>();
         private int nestedClassNameCount = 0;
         private boolean isStruct;

         // is the name enclosed enclosed by a class of the same name?
         private boolean isEnclosedBySameName(String name) {
            return className().equals(name) ||
                (isNested() && parent.isEnclosedBySameName(name));
         }

         private boolean isNested() {
             return parent != null && parent.isStruct;
         }

         private Scope(Scope parent, String name, boolean isStruct) {
             this.parent = parent;
             this.className = parent != null ?
                  parent.uniqueNestedClassName(name) :
                  javaSafeIdentifier(name);
             this.isStruct = isStruct;
         }

         static Scope newStruct(Scope parent, String name) {
             return new Scope(parent, name, true);
         }

         static Scope newHeader(String name) {
             return new Scope(null, name, false);
         }

         String uniqueNestedClassName(String name) {
             name = javaSafeIdentifier(name);
             var notSeen = nestedClassNames.add(name.toLowerCase());
             var notEnclosed = !isEnclosedBySameName(name);
             return notSeen && notEnclosed? name : (name + "$" + nestedClassNameCount++);
         }

         String className() {
             return className;
         }
    }

    private Scope curScope;

    private static record NameAndDecl(String name, Declaration decl) {}
    // key is either Declaration or NameAndDecl
    private final Map<Object, String> declJavaNames = new HashMap<>();

    private static record DeclPair(Declaration parent, Declaration decl) {}
    // key is either Declaration or NameAndDecl or DeclPair
    private final Map<Object, String> declFiNames = new HashMap<>();
    private final Map<Type.Function, List<String>> parameterNames = new HashMap<>();

    NameMangler(String headerName) {
        this.headerName = headerName;
    }

    // package private name lookup API
    String getJavaName(Declaration parent, Declaration decl) {
        Objects.requireNonNull(decl);
        if (declJavaNames.containsKey(decl)) {
            return Objects.requireNonNull(declJavaNames.get(decl));
        } else {
            var name = decl.name().isEmpty()? parent.name() : decl.name();
            var nameAndDecl = new NameAndDecl(name, decl);
            return Objects.requireNonNull(declJavaNames.get(nameAndDecl));
        }
    }

    Optional<List<String>> getParameterNames(Type.Function func) {
        return Optional.ofNullable(parameterNames.get(func));
    }

    String getFiName(Declaration.Function func, int paramNum, Declaration.Variable param) {
        Objects.requireNonNull(func);
        Objects.requireNonNull(param);
        var nameAndDecl = new NameAndDecl(funcParamID(func, paramNum), param);
        return Objects.requireNonNull(declFiNames.get(nameAndDecl));
    }

    String getReturnFiName(Declaration.Function func) {
        Objects.requireNonNull(func);
        return funcReturnID(func);
    }

    String getFiName(Declaration parent, Declaration decl) {
        Objects.requireNonNull(decl);
        if (declFiNames.containsKey(decl)) {
            return Objects.requireNonNull(declFiNames.get(decl));
        } else {
            Objects.requireNonNull(parent);
            var declPair = new DeclPair(parent, decl);
            return Objects.requireNonNull(declFiNames.get(declPair));
        }
    }

    // Internals below this point

    private static String funcReturnID(Declaration.Function func) {
        return func.name() + "$return";
    }

    private static String funcParamID(Declaration.Function func, int paramNum) {
        return func.name() + "$" + paramNum;
    }

    private void putJavaName(Declaration decl, String javaName) {
        assert decl != null;
        assert javaName != null;
        declJavaNames.put(decl, javaName);
    }

    private void putJavaName(String name, Declaration decl, String javaName) {
        assert name != null;
        assert decl != null;
        assert javaName != null;
        declJavaNames.put(new NameAndDecl(name, decl), javaName);
    }

    private void putFiName(Declaration decl, String javaName) {
        assert decl != null;
        assert javaName != null;
        declFiNames.put(decl, javaName);
    }

    private void putFiName(Declaration parent, Declaration.Variable variable, String javaName) {
        assert parent != null;
        assert variable != null;
        assert javaName != null;
        declFiNames.put(new DeclPair(parent, variable), javaName);
    }

    private void putFiName(String name, Declaration decl, String javaName) {
        assert name != null;
        assert decl != null;
        assert javaName != null;
        declFiNames.put(new NameAndDecl(name, decl), javaName);
    }

    // entry point for this visitor
    Declaration.Scoped scan(Declaration.Scoped header) {
        String javaName = javaSafeIdentifier(headerName.replace(".h", "_h"), true);
        curScope = Scope.newHeader(javaName);
        putJavaName(header, javaName);
        // Process all header declarations are collect java name mappings
        header.members().forEach(fieldTree -> fieldTree.accept(this, null));
        return header;
    }

    @Override
    public Void visitConstant(Declaration.Constant constant, Declaration parent) {
        putJavaName(constant, makeJavaName(constant));
        return null;
    }

    @Override
    public Void visitFunction(Declaration.Function func, Declaration parent) {
        putJavaName(func, makeJavaName(func));
        int i = 0;
        for (Declaration.Variable param : func.parameters()) {
            Type.Function f = Utils.getAsFunctionPointer(param.type());
            if (f != null) {
                String declFiName = func.name() + "$" + (param.name().isEmpty() ? "x" + i : param.name());
                putFiName(funcParamID(func, i), param, declFiName);
                i++;
            }
            putJavaName(param, makeJavaName(param));
        }

        return null;
    }

    @Override
    public Void visitScoped(Declaration.Scoped scoped, Declaration parent) {
        String name = scoped.name().isEmpty()? parent.name() : scoped.name();
        if (declJavaNames.containsKey(new NameAndDecl(name, scoped))) {
            //skip struct that's seen already
            return null;
        }

        boolean isStruct = Utils.isStructOrUnion(scoped);
        if (!isStruct) {
            return null;
        }

        Scope oldScope = curScope;
        boolean isNestedAnonStruct = scoped.name().isEmpty() &&
            (parent instanceof Declaration.Scoped);
        if (!isNestedAnonStruct) {
            this.curScope = Scope.newStruct(oldScope, name);
            putJavaName(name, scoped, curScope.className());
        }
        try {
            scoped.members().forEach(fieldTree -> fieldTree.accept(this, scoped));
        } finally {
            this.curScope = oldScope;
        }

        return null;
    }

    @Override
    public Void visitTypedef(Declaration.Typedef typedef, Declaration parent) {
        if (declJavaNames.containsKey(typedef)) {
            //skip typedef that's seen already
            return null;
        }

        // handle if this typedef is of a struct/union/enum etc.
        if (typedef.type() instanceof Type.Declared declared) {
            declared.tree().accept(this, typedef);
        }

        // We may potentially generate a class for a typedef. Make sure
        // class name is unique in the current nesting context.
        String javaName = curScope.uniqueNestedClassName(typedef.name());
        putJavaName(typedef, javaName);
        Type.Function func = Utils.getAsFunctionPointer(typedef.type());
        if (func != null) {
           var paramNamesOpt = func.parameterNames();
           if (paramNamesOpt.isPresent()) {
               parameterNames.put(func,
                   paramNamesOpt.
                      get().
                      stream().
                      map(NameMangler::javaSafeIdentifier).
                      toList()
               );
           }
           putFiName(typedef, javaName);
        }
        return null;
    }

    @Override
    public Void visitVariable(Declaration.Variable variable, Declaration parent) {
        putJavaName(variable, makeJavaName(variable));
        var type = variable.type();
        if (type instanceof Type.Declared declared) {
            // declared type - visit declaration recursively
            declared.tree().accept(this, variable);
        }
        Type.Function func = Utils.getAsFunctionPointer(type);
        if (func != null) {
            String fiName = curScope.uniqueNestedClassName(variable.name());
            if (parent != null) {
                putFiName(parent, variable, fiName);
            } else {
                putFiName(variable, fiName);
            }
        }
        return null;
    }

    @Override
    public Void visitDeclaration(Declaration decl, Declaration parent) {
        return null;
    }

    private List<String> javaSafeNameList(List<String> names) {
        return names.stream().
            map(n -> n.isEmpty()? n : javaSafeIdentifier(n)).
            toList();
    }

    private String makeJavaName(Declaration decl) {
        return decl.name().isEmpty()? decl.name() : javaSafeIdentifier(decl.name());
    }

    // Java identifier handling helpers
    private static String javaSafeIdentifier(String name) {
        return javaSafeIdentifier(name, false);
    }

    private static String javaSafeIdentifier(String name, boolean checkAllChars) {
        if (checkAllChars) {
            StringBuilder buf = new StringBuilder();
            char[] chars = name.toCharArray();
            if (Character.isJavaIdentifierStart(chars[0])) {
                buf.append(chars[0]);
            } else {
                buf.append('_');
            }
            if (chars.length > 1) {
                for (int i = 1; i < chars.length; i++) {
                    char ch = chars[i];
                    if (Character.isJavaIdentifierPart(ch)) {
                        buf.append(ch);
                    } else {
                        buf.append('_');
                    }
                }
            }
            return buf.toString();
        } else {
            // We never get the problem of Java non-identifiers (like 123, ab-xy) as
            // C identifiers. But we may have a java keyword used as a C identifier.
            assert SourceVersion.isIdentifier(name);

            return SourceVersion.isKeyword(name) || isRestrictedTypeName(name) || isJavaTypeName(name)? (name + "_") : name;
        }
    }

    private static boolean isRestrictedTypeName(String name) {
        return switch (name) {
            case "var", "yield", "record",
                "sealed", "permits" -> true;
            default -> false;
        };
    }

    private static boolean isJavaTypeName(String name) {
        // Java types that are used unqualified in the generated code
        return switch (name) {
            case "String", "Struct", "MethodHandle",
                "VarHandle", "ByteOrder",
                "FunctionDescriptor", "LibraryLookup",
                "MemoryLayout",
                "Arena", "NativeArena", "MemorySegment", "ValueLayout",
                "RuntimeHelper" -> true;
            default -> false;
        };
    }
}
