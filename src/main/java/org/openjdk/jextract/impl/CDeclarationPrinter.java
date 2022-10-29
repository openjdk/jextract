/*
 *  Copyright (c) 2022, Oracle and/or its affiliates. All rights reserved.
 *  DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 *  This code is free software; you can redistribute it and/or modify it
 *  under the terms of the GNU General Public License version 2 only, as
 *  published by the Free Software Foundation.  Oracle designates this
 *  particular file as subject to the "Classpath" exception as provided
 *  by Oracle in the LICENSE file that accompanied this code.
 *
 *  This code is distributed in the hope that it will be useful, but WITHOUT
 *  ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 *  FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 *  version 2 for more details (a copy is included in the LICENSE file that
 *  accompanied this code).
 *
 *  You should have received a copy of the GNU General Public License version
 *  2 along with this work; if not, write to the Free Software Foundation,
 *  Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 *   Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 *  or visit www.oracle.com if you need additional information or have any
 *  questions.
 *
 */

package org.openjdk.jextract.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import org.openjdk.jextract.Declaration;
import org.openjdk.jextract.Type;

final class CDeclarationPrinter implements Declaration.Visitor<Void, Void> {
    private static String SPACES = " ".repeat(92);
    private int align = 0;
    private String prefix;

    private void incr() {
        align += 4;
    }

    private void decr() {
        align -= 4;
    }

    private CDeclarationPrinter(String prefix) {
        this.prefix = prefix;
    }

    private void indent() {
        builder.append(prefix);
        builder.append(SPACES.substring(0, align));
    }

    private final StringBuilder builder = new StringBuilder();

    private String print(Declaration decl) {
        decl.accept(this, null);
        return builder.toString();
    }

    // Return C source style signature for the given declaration.
    // The prefix is emitted for every line. This can be used
    // to prefix per line comment character "*" in generated javadoc.
    static String declaration(Declaration decl, String prefix) {
        Objects.requireNonNull(decl);
        Objects.requireNonNull(prefix);
        return new CDeclarationPrinter(prefix).print(decl);
    }

    static String declaration(Type.Function funcType, String name) {
        return nameAndType(funcType, "*" + name);
    }

    @Override
    public Void visitScoped(Declaration.Scoped d, Void ignored) {
        indent();
        var tag = typeTag(d);
        if (!tag.isEmpty()) {
            builder.append(tag);
            if (!d.name().isEmpty()) {
                builder.append(" " + d.name());
            }
            builder.append(" {");
            builder.append("\n");
            incr();
        }
        d.members().forEach(m -> m.accept(this, null));
        if (!tag.isEmpty()) {
            decr();
            indent();
            builder.append("};\n");
        }
        return null;
    }

    @Override
    public Void visitFunction(Declaration.Function d, Void ignored) {
        indent();

        // name and args part of the function
        StringBuilder buf = new StringBuilder();
        buf.append(d.name());
        buf.append('(');
        buf.append(
            d.parameters().
                stream().
                map(p -> nameAndType(p.type(), p.name())).
                collect(Collectors.joining(", "))
        );
        if (d.type().varargs()) {
            buf.append(",...");
        }
        buf.append(')');

        // The return type is handled later to take care of
        // pointer to function return type like signal from signal.h
        // void (*signal(int sig, void (*func)(int)))(int)

        String funcNameAndArgs = buf.toString();
        Type returnType = d.type().returnType();
        builder.append(nameAndType(returnType, funcNameAndArgs));
        builder.append(";\n");
        return null;
    }

    @Override
    public Void visitVariable(Declaration.Variable d, Void ignored) {
        indent();
        builder.append(nameAndType(d.type(), d.name()));
        builder.append(";\n");
        return null;
    }

    @Override
    public Void visitConstant(Declaration.Constant d, Void ignored) {
        indent();
        Optional<String> enumName = EnumConstantLifter.enumName(d);
        if (enumName.isPresent()) {
            builder.append("enum " + enumName.get() + "." + d.name());
            builder.append(" = ");
            builder.append(d.value());
            builder.append(";\n");
        } else {
            builder.append("#define ");
            builder.append(d.name());
            Object value = d.value();
            builder.append(" ");
            if (value instanceof String str) {
                builder.append("\"" + Utils.quote(str) + "\"");
            } else {
                builder.append(value);
            }
            builder.append("\n");
        }
        return null;
    }

    @Override
    public Void visitTypedef(Declaration.Typedef d, Void ignored) {
        indent();
        builder.append("typedef ");
        builder.append(nameAndType(d.type(), d.name()));
        builder.append(";\n");
        return null;
    }

    // In few cases, C type signature 'embeds' name.
    // Examples:
    //     int a[3]; // 'a' in between int and []
    //     int (*func)(int); // 'func' is inside paren after '*'
    // TypeVisitor accepts name and includes it in the appropriate
    // place as needed. If not included, boolean flag is set to false
    // in the result.

    private static String nameAndType(Type type, String name) {
        var result = type.accept(typeVisitor, name);
        var typeStr = result.typeStr();
        return result.nameIncluded() || name.isEmpty() ?
            typeStr : (typeStr + " " + name);
    }

    // result type for Type.Visitor
    private record TypeVisitorResult(boolean nameIncluded, String typeStr) {}

    private static Type.Visitor<TypeVisitorResult, String> typeVisitor = new Type.Visitor<>() {
        // context argument in this visitor usually starts with a name. But it may pick up
        // "*" prefixes for pointer type. [] suffix for array types. For pointer to function
        // return type, the context is name of the function + argument types as in declaration.

        @Override
        public TypeVisitorResult visitPrimitive(Type.Primitive t, String context) {
            return new TypeVisitorResult(false, t.kind().typeName());
        }

        private TypeVisitorResult prefixedType(String prefix, Type.Delegated delegated) {
            return new TypeVisitorResult(false,
                    prefix + " " + delegated.type().accept(this, "").typeStr());
        }

        @Override
        public TypeVisitorResult visitDelegated(Type.Delegated t, String context) {
            switch (t.kind()) {
                case POINTER: {
                    var result = t.type().accept(this, "*" + context);
                    if (result.nameIncluded()) {
                        return new TypeVisitorResult(true, result.typeStr());
                    } else {
                        return new TypeVisitorResult(false, result.typeStr() + "*");
                    }
                }
                case UNSIGNED:
                    return prefixedType("unsigned", t);
                case SIGNED:
                    return prefixedType("signed", t);
                case VOLATILE:
                    return prefixedType("volatile", t);
                case COMPLEX:
                    return prefixedType("complex", t);
                case default:
                    // defensive. If no name is present, we don't want to crash
                    return new TypeVisitorResult(false, t.name().orElse(defaultName(t)));
            }
        }

        @Override
        public TypeVisitorResult visitFunction(Type.Function t, String context) {
            String argsStr;
            // Function type may optionally have parameter names.
            // Include parameter names if available.
            var optParameterNames = t.parameterNames();
            if (optParameterNames.isPresent()) {
                List<Type> argTypes = t.argumentTypes();
                List<String> argNames = optParameterNames.get();
                int numArgs = argTypes.size();
                List<String> args = new ArrayList<>(numArgs);
                for (int i = 0; i < numArgs; i++) {
                    args.add(nameAndType(argTypes.get(i), argNames.get(i)));
                }
                argsStr = args.stream()
                    .collect(Collectors.joining(",", "(", ")"));
            } else {
                argsStr = t.argumentTypes().stream()
                    .map(a -> a.accept(this, "").typeStr())
                    .collect(Collectors.joining(",", "(", ")"));
            }
            String res = t.returnType().accept(this, "").typeStr();
            return new TypeVisitorResult(true, res + " (" + context + ")" + argsStr);
        }

        @Override
        public TypeVisitorResult visitDeclared(Type.Declared t, String context) {
            Declaration.Scoped scoped = t.tree();
            return new TypeVisitorResult(false, typeTag(scoped) + " " + scoped.name());
        }

        @Override
        public TypeVisitorResult visitArray(Type.Array t, String context) {
            String brackets = String.format(" %s[%s]", context,
                t.elementCount().isPresent() ? t.elementCount().getAsLong() : "");
            var result = t.elementType().accept(this, brackets);
            if (result.nameIncluded()) {
                return new TypeVisitorResult(true, result.typeStr());
            } else {
                return new TypeVisitorResult(true, result.typeStr() + brackets);
            }
        }

        @Override
        public TypeVisitorResult visitType(Type t, String context) {
            return new TypeVisitorResult(false, defaultName(t));
        }

        private String defaultName(Type t) {
            return t.toString();
        }
    };

    private static String typeTag(Declaration.Scoped scoped) {
        return switch (scoped.kind()) {
            case STRUCT -> "struct";
            case UNION -> "union";
            case ENUM -> "enum";
            default -> "";
        };
    }
}
