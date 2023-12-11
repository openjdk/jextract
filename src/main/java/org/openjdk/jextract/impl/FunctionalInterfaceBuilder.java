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

import java.lang.foreign.*;

import org.openjdk.jextract.Type;

import java.lang.invoke.MethodType;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

final class FunctionalInterfaceBuilder extends ClassSourceBuilder {

    private final MethodType fiType;
    private final MethodType downcallType;
    private final FunctionDescriptor fiDesc;
    private final Optional<List<String>> parameterNames;

    private FunctionalInterfaceBuilder(SourceFileBuilder builder, String className, ClassSourceBuilder enclosing,
                                       String runtimeHelperName, Type.Function funcType,
                                       Optional<List<String>> parameterNames) {
        super(builder, "public", Kind.INTERFACE, className, null, enclosing, runtimeHelperName);
        this.fiDesc = Type.descriptorFor(funcType).get();
        this.fiType = fiDesc.toMethodType();
        this.downcallType = fiDesc.toMethodType();
        this.parameterNames = parameterNames;
    }

    public static void generate(SourceFileBuilder builder, String className, ClassSourceBuilder enclosing, String runtimeHelperName,
                                Type.Function funcType, Optional<List<String>> parameterNames) {
        FunctionalInterfaceBuilder fib = new FunctionalInterfaceBuilder(builder, className, enclosing, runtimeHelperName,
                funcType, parameterNames);
        fib.emitDocComment(funcType, className);
        fib.classBegin();
        fib.emitDescriptorDecl();
        fib.emitFunctionalInterfaceMethod();
        fib.emitFunctionalFactories();
        fib.emitFunctionalFactoryForPointer();
        fib.classEnd();
    }

    private void emitFunctionalInterfaceMethod() {
        appendIndentedLines(STR."""
            \{fiRetType()} apply(\{paramExprs("")});
            """);
    }

    private void emitFunctionalFactories() {
        appendIndentedLines(STR."""
            MethodHandle UP$MH = \{runtimeHelperName()}.upcallHandle(\{className()}.class, \"apply\", $DESC);

            static MemorySegment allocate(\{className()} fi, Arena scope) {
                return Linker.nativeLinker().upcallStub(UP$MH.bindTo(fi), $DESC, scope);
            }
            """);
    }

    private void emitFunctionalFactoryForPointer() {
        appendIndentedLines(STR."""
            MethodHandle DOWN$MH = Linker.nativeLinker().downcallHandle($DESC);

            static \{className()} ofAddress(MemorySegment addr, Arena arena) {
                MemorySegment symbol = addr.reinterpret(arena, null);
                return (\{paramExprs("_")}) -> {
                    try {
                        \{retExpr()} DOWN$MH.invokeExact(symbol\{otherArgExprs()});
                    } catch (Throwable ex$) {
                        throw new AssertionError("should not reach here", ex$);
                    }
                };
            }
            """);
    }

    // private generation
    private String parameterName(int i) {
        String name = "";
        if (parameterNames.isPresent()) {
            name = parameterNames.get().get(i);
        }
        return name.isEmpty()? "_x" + i : name;
    }

    private String paramExprs(String paramNamePrefix) {
        StringBuilder result = new StringBuilder();
        String delim = "";
        for (int i = 0 ; i < fiType.parameterCount(); i++) {
            result.append(delim).append(fiType.parameterType(i).getSimpleName());
            result.append(" ");
            result.append(paramNamePrefix).append(parameterName(i));
            delim = ", ";
        }
        return result.toString();
    }

    private String fiRetType() {
        return fiType.returnType().getSimpleName();
    }

    private String downcallRetType() {
        return fiType.returnType().getSimpleName();
    }

    private String retExpr() {
        String retExpr = "";
        if (!fiType.returnType().equals(void.class)) {
            retExpr = STR."return (\{fiRetType()})";
            if (fiType.returnType() != downcallType.returnType()) {
                // add cast for invokeExact
                retExpr += STR." (\{downcallRetType()})";
            }
        }
        return retExpr;
    }

    private String otherArgExprs() {
        String argsExprs = "";
        if (fiType.parameterCount() > 0) {
            argsExprs += ", " + IntStream.range(0, fiType.parameterCount())
                    .mapToObj(i -> {
                        String paramExpr = "_" + parameterName(i);
                        if (fiType.parameterType(i) != downcallType.parameterType(i)) {
                            // add cast for invokeExact
                            return "(" + downcallType.parameterType(i).getName() + ")" + paramExpr;
                        } else {
                            return paramExpr;
                        }
                    })
                    .collect(Collectors.joining(", "));
        }
        return argsExprs;
    }

    private void emitDescriptorDecl() {
        appendIndentedLines(STR."""
            FunctionDescriptor $DESC = \{descriptorString(0, fiDesc)};
            """);
    }
}
