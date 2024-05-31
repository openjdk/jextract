/*
 * Copyright (c) 2020, 2024, Oracle and/or its affiliates. All rights reserved.
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

import java.lang.invoke.MethodType;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

final class FunctionalInterfaceBuilder extends ClassSourceBuilder {

    private final Type.Function funcType;
    private final MethodType methodType;
    private final Optional<List<String>> parameterNames;

    private FunctionalInterfaceBuilder(SourceFileBuilder builder, String className, ClassSourceBuilder enclosing,
                                       String runtimeHelperName, Type.Function funcType, boolean isNested) {
        super(builder, isNested ? "public static" : "public", Kind.CLASS, className, null, enclosing, runtimeHelperName);
        this.parameterNames = funcType.parameterNames().map(NameMangler::javaSafeIdentifiers);
        this.funcType = funcType;
        this.methodType = Utils.methodTypeFor(funcType);
    }

    public static void generate(SourceFileBuilder builder, String className, ClassSourceBuilder enclosing, String runtimeHelperName,
                                Declaration parentDecl, Type.Function funcType, boolean isNested) {
        FunctionalInterfaceBuilder fib = new FunctionalInterfaceBuilder(builder, className,
                enclosing, runtimeHelperName, funcType, isNested);
        fib.appendBlankLine();
        fib.emitDocComment(parentDecl);
        fib.classBegin();
        fib.emitDefaultConstructor();
        String fiName = fib.emitFunctionalInterface();
        fib.emitDescriptorDecl();
        fib.emitFunctionalFactory(fiName);
        fib.emitInvoke();
        fib.classEnd();
    }

    private String emitFunctionalInterface() {
        // beware of mangling!
        String fiName = className().toLowerCase().equals("function") ?
                "Function$" : "Function";
        appendIndentedLines("""

            /**
             * The function pointer signature, expressed as a functional interface
             */
            public interface %1$s {
                %2$s apply(%3$s);
            }
            """,
            fiName, methodType.returnType().getSimpleName(), paramExprs());
        return fiName;
    }

    private void emitFunctionalFactory(String fiName) {
        appendIndentedLines("""

            private static final MethodHandle UP$MH = %1$s.upcallHandle(%2$s.%3$s.class, "apply", $DESC);

            /**
             * Allocates a new upcall stub, whose implementation is defined by {@code fi}.
             * The lifetime of the returned segment is managed by {@code arena}
             */
            public static MemorySegment allocate(%2$s.%3$s fi, Arena arena) {
                return Linker.nativeLinker().upcallStub(UP$MH.bindTo(fi), $DESC, arena);
            }
            """,
            runtimeHelperName(), className(), fiName);
    }

    private void emitInvoke() {
        boolean needsAllocator = Utils.isStructOrUnion(funcType.returnType());
        String allocParam = needsAllocator ? ", SegmentAllocator alloc" : "";
        String allocArg = needsAllocator ? ", alloc" : "";
        String paramStr = methodType.parameterCount() != 0 ? String.format(",%s", paramExprs()) : "";
        appendIndentedLines("""
        private static final MethodHandle DOWN$MH = Linker.nativeLinker().downcallHandle($DESC);

            /**
             * Invoke the upcall stub {@code funcPtr}, with given parameters
             */
            public static %1$s invoke(MemorySegment funcPtr%2$s%3$s) {
                try {
                    %4$s DOWN$MH.invokeExact(funcPtr%5$s%6$s);
                } catch (Throwable ex$) {
                    throw new AssertionError("should not reach here", ex$);
                }
            }
            """,
            methodType.returnType().getSimpleName(),
            allocParam,
            paramStr,
            retExpr(),
            allocArg,
            otherArgExprs());
    }

    // private generation
    private String parameterName(int i) {
        String name = "";
        if (parameterNames.isPresent()) {
            name = parameterNames.get().get(i);
        }
        return name.isEmpty()? "_x" + i : name;
    }

    private String paramExprs() {
        StringBuilder result = new StringBuilder();
        String delim = "";
        for (int i = 0 ; i < methodType.parameterCount(); i++) {
            result.append(delim).append(methodType.parameterType(i).getSimpleName());
            result.append(" ");
            result.append(parameterName(i));
            delim = ", ";
        }
        return result.toString();
    }

    private String retExpr() {
        String retExpr = "";
        if (!methodType.returnType().equals(void.class)) {
            retExpr = String.format("return (%s)", methodType.returnType().getSimpleName());
        }
        return retExpr;
    }

    private String otherArgExprs() {
        String argsExprs = "";
        if (methodType.parameterCount() > 0) {
            argsExprs += ", " + IntStream.range(0, methodType.parameterCount())
                    .mapToObj(this::parameterName)
                    .collect(Collectors.joining(", "));
        }
        return argsExprs;
    }

    private void emitDescriptorDecl() {
        appendIndentedLines("""

            private static final FunctionDescriptor $DESC = %s;

            /**
             * The descriptor of this function pointer
             */
            public static FunctionDescriptor descriptor() {
                return $DESC;
            }
            """, functionDescriptorString(0, funcType));
    }
}
