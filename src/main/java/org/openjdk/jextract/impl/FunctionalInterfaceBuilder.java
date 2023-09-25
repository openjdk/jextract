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

import org.openjdk.jextract.impl.Constants.Constant;
import org.openjdk.jextract.Type;

import java.lang.invoke.MethodType;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class FunctionalInterfaceBuilder extends ClassSourceBuilder {

    private static final String MEMBER_MODS = "static";

    private final Type.Function funcType;
    private final MethodType fiType;
    private final MethodType downcallType;
    private final FunctionDescriptor fiDesc;
    private final Optional<List<String>> parameterNames;

    FunctionalInterfaceBuilder(JavaSourceBuilder enclosing, Type.Function funcType, String className,
                               FunctionDescriptor descriptor, Optional<List<String>> parameterNames) {
        super(enclosing, Kind.INTERFACE, className);
        this.funcType = funcType;
        this.fiType = descriptor.toMethodType();
        this.downcallType = descriptor.toMethodType();
        this.fiDesc = descriptor;
        this.parameterNames = parameterNames;
    }

    @Override
    void classDeclBegin() {
        emitDocComment(funcType, className());
    }

    @Override
    JavaSourceBuilder classEnd() {
        emitFunctionalInterfaceMethod();
        emitFunctionalFactories();
        emitFunctionalFactoryForPointer();
        return super.classEnd();
    }

    // private generation
    private String parameterName(int i) {
        String name = "";
        if (parameterNames.isPresent()) {
            name = parameterNames.get().get(i);
        }
        return name.isEmpty()? "_x" + i : name;
    }

    private void emitFunctionalInterfaceMethod() {
        incrAlign();
        indent();
        append(fiType.returnType().getName() + " apply(");
        String delim = "";
        for (int i = 0 ; i < fiType.parameterCount(); i++) {
            append(delim + fiType.parameterType(i).getName());
            append(" ");
            append(parameterName(i));
            delim = ", ";
        }
        append(");\n");
        decrAlign();
    }

    private void emitFunctionalFactories() {
        Constant functionDesc = constants().addFunctionDesc(fiDesc);
        Constant upcallHandle = constants().addUpcallMethodHandle(fullName(), "apply", fiDesc);
        incrAlign();
        indent();
        append(MEMBER_MODS + " MemorySegment allocate(" + className() + " fi, Arena scope) {\n");
        incrAlign();
        indent();
        append("return RuntimeHelper.upcallStub(" +
            upcallHandle.accessExpression() + ", fi, " + functionDesc.accessExpression() + ", scope);\n");
        decrAlign();
        indent();
        append("}\n");
        decrAlign();
    }

    private void emitFunctionalFactoryForPointer() {
        Constant mhConstant = constants().addVirtualDowncallMethodHandle(fiDesc);
        incrAlign();
        indent();
        append(MEMBER_MODS + " " + className() + " ofAddress(MemorySegment addr, Arena arena) {\n");
        incrAlign();
        indent();
        append("MemorySegment symbol = addr.reinterpret(");
        append("arena, null);\n");
        indent();
        append("return (");
        String delim = "";
        for (int i = 0 ; i < fiType.parameterCount(); i++) {
            append(delim + fiType.parameterType(i).getName());
            append(" ");
            append("_" + parameterName(i));
            delim = ", ";
        }
        append(") -> {\n");
        incrAlign();
        indent();
        append("try {\n");
        incrAlign();
        indent();
        if (!fiType.returnType().equals(void.class)) {
            append("return (" + fiType.returnType().getName() + ")");
            if (fiType.returnType() != downcallType.returnType()) {
                // add cast for invokeExact
                append("(" + downcallType.returnType().getName() + ")");
            }
        }
        append(mhConstant.accessExpression() + ".invokeExact(symbol");
        if (fiType.parameterCount() > 0) {
            String params = IntStream.range(0, fiType.parameterCount())
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
            append(", " + params);
        }
        append(");\n");
        decrAlign();
        indent();
        append("} catch (Throwable ex$) {\n");
        incrAlign();
        indent();
        append("throw new AssertionError(\"should not reach here\", ex$);\n");
        decrAlign();
        indent();
        append("}\n");
        decrAlign();
        indent();
        append("};\n");
        decrAlign();
        indent();
        append("}\n");
        decrAlign();
    }
}
