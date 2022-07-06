/*
 * Copyright (c) 2020, 2022, Oracle and/or its affiliates. All rights reserved.
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

import org.openjdk.jextract.impl.ConstantBuilder.Constant;

import java.lang.invoke.MethodType;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class FunctionalInterfaceBuilder extends ClassSourceBuilder {

    private static final String MEMBER_MODS = "static";

    private final MethodType fiType;
    private final FunctionDescriptor fiDesc;
    private final Optional<List<String>> parameterNames;

    FunctionalInterfaceBuilder(JavaSourceBuilder enclosing, String className,
                               FunctionDescriptor descriptor, Optional<List<String>> parameterNames) {
        super(enclosing, Kind.INTERFACE, className);
        this.fiType = Linker.methodType(descriptor);
        this.fiDesc = descriptor;
        this.parameterNames = parameterNames;
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
        return name.isEmpty()? "_x" + i : Utils.javaSafeIdentifier(name);
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
        emitWithConstantClass(constantBuilder -> {
            Constant functionDesc = constantBuilder.addFunctionDesc(className(), fiDesc);
            incrAlign();
            indent();
            append(MEMBER_MODS + " MemorySegment allocate(" + className() + " fi, Arena session) {\n");
            incrAlign();
            indent();
            append("return RuntimeHelper.upcallStub(" + className() + ".class, fi, " +
                functionDesc.accessExpression() + ", session);\n");
            decrAlign();
            indent();
            append("}\n");
            decrAlign();
        });
    }

    private void emitFunctionalFactoryForPointer() {
        emitWithConstantClass(constantBuilder -> {
            Constant mhConstant = constantBuilder.addMethodHandle(className(), className(),
                 fiDesc, false, true);
            incrAlign();
            indent();
            append(MEMBER_MODS + " " + fiType.returnType().getName() + " apply(MemorySegment symbol\n");
            String delim = ", ";
            for (int i = 0 ; i < fiType.parameterCount(); i++) {
                append(delim + fiType.parameterType(i).getName());
                append(" ");
                append("_" + parameterName(i));
            }
            append(") {\n");
            incrAlign();
            indent();
            append("try {\n");
            incrAlign();
            indent();
            if (!fiType.returnType().equals(void.class)) {
                append("return (" + fiType.returnType().getName() + ")");
            }
            append(mhConstant.accessExpression() + ".invokeExact(symbol");
            if (fiType.parameterCount() > 0) {
                String params = IntStream.range(0, fiType.parameterCount())
                        .mapToObj(i -> {
                            String paramExpr = "_" + parameterName(i);
                            return paramExpr;
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
            append("}\n");
            decrAlign();
        });
    }

    @Override
    protected void emitWithConstantClass(Consumer<ConstantBuilder> constantConsumer) {
        enclosing.emitWithConstantClass(constantConsumer);
    }
}
