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
import org.openjdk.jextract.impl.DeclarationImpl.JavaFunctionalInterfaceName;
import org.openjdk.jextract.impl.DeclarationImpl.JavaName;
import org.openjdk.jextract.impl.DeclarationImpl.JavaParameterNames;

import java.util.HashSet;
import java.util.Set;
import javax.lang.model.SourceVersion;

/*
 * This visitor handles java safe names for identifiers, type names and stores such names
 * in the corresponding declaration. The mangled name is later retrieved by
 * OutputFactory via the lookup methods provided by this class.
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

    NameMangler(String headerName) {
        this.headerName = headerName;
    }

    // entry point for this visitor
    Declaration.Scoped scan(Declaration.Scoped header) {
        String javaName = javaSafeIdentifier(headerName.replace(".h", "_h"), true);
        curScope = Scope.newHeader(javaName);
        JavaName.with(header, javaName);
        // Process all header declarations are collect java name mappings
        header.members().forEach(fieldTree -> fieldTree.accept(this, null));
        return header;
    }

    @Override
    public Void visitConstant(Declaration.Constant constant, Declaration parent) {
        JavaName.with(constant, makeJavaName(constant));
        return null;
    }

    @Override
    public Void visitFunction(Declaration.Function func, Declaration parent) {
        JavaName.with(func, makeJavaName(func));
        int i = 0;
        for (Declaration.Variable param : func.parameters()) {
            Type.Function f = Utils.getAsFunctionPointer(param.type());
            if (f != null) {
                String declFiName = func.name() + "$" + (param.name().isEmpty() ? "x" + i : param.name());
                JavaFunctionalInterfaceName.with(param, declFiName);
                i++;
            }
            JavaName.with(param, makeJavaName(param));
        }
        Type.Function returnFunc = Utils.getAsFunctionPointer(func.type().returnType());
        if (returnFunc != null) {
            JavaFunctionalInterfaceName.with(func, func.name() + "$return");
        }

        return null;
    }

    @Override
    public Void visitScoped(Declaration.Scoped scoped, Declaration parent) {
        String name = scoped.name();
        if (name.isEmpty() && parent != null) {
            name = parent.name();
        }
        if (JavaName.isPresent(scoped)) {
            //skip struct that's seen already
            return null;
        }

        Scope oldScope = curScope;
        boolean isNestedAnonStruct = scoped.name().isEmpty() &&
            (parent instanceof Declaration.Scoped);
        if (!isNestedAnonStruct) {
            this.curScope = Scope.newStruct(oldScope, name);
            JavaName.with(scoped, curScope.className());
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
        if (JavaName.isPresent(typedef)) {
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
        JavaName.with(typedef, javaName);
        Type.Function func = Utils.getAsFunctionPointer(typedef.type());
        if (func != null) {
           var paramNamesOpt = func.parameterNames();
           if (paramNamesOpt.isPresent()) {
               JavaParameterNames.with(func,
                   paramNamesOpt.
                      get().
                      stream().
                      map(NameMangler::javaSafeIdentifier).
                      toList()
               );
           }
           JavaFunctionalInterfaceName.with(typedef, javaName);
        }
        return null;
    }

    @Override
    public Void visitVariable(Declaration.Variable variable, Declaration parent) {
        JavaName.with(variable, makeJavaName(variable));
        var type = variable.type();
        if (type instanceof Type.Declared declared) {
            // declared type - visit declaration recursively
            declared.tree().accept(this, variable);
        }
        Type.Function func = Utils.getAsFunctionPointer(type);
        if (func != null) {
            String fiName = curScope.uniqueNestedClassName(variable.name());
            JavaFunctionalInterfaceName.with(variable, fiName);
        }
        return null;
    }

    @Override
    public Void visitDeclaration(Declaration decl, Declaration parent) {
        return null;
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
                "Arena", "NativeArena", "MemorySegment", "ValueLayout"
                    -> true;
            default -> false;
        };
    }
}
