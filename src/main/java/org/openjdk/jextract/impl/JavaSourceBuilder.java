/*
 * Copyright (c) 2021, 2022, Oracle and/or its affiliates. All rights reserved.
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

import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.GroupLayout;
import java.lang.foreign.MemoryLayout;
import org.openjdk.jextract.Declaration;
import org.openjdk.jextract.Type;

import javax.tools.JavaFileObject;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;

public abstract class JavaSourceBuilder {

    public void addVar(Declaration.Variable varTree, String javaName,
        MemoryLayout layout, Optional<String> fiName) {
        throw new UnsupportedOperationException();
    }

    public void addFunction(Declaration.Function funcTree, FunctionDescriptor descriptor,
        String javaName, List<String> parameterNames) {
        throw new UnsupportedOperationException();
    }

    public void addConstant(Declaration.Constant constantTree, String javaName, Class<?> javaType) {
        throw new UnsupportedOperationException();
    }

    public void addTypedef(Declaration.Typedef typedefTree, String javaName, String superClass) {
        addTypedef(typedefTree, javaName, superClass, typedefTree.type());
    }

    public void addTypedef(Declaration.Typedef typedefTree, String javaName,
        String superClass, Type type) {
        throw new UnsupportedOperationException();
    }

    public StructBuilder addStruct(Declaration.Scoped structTree, boolean isNestedAnonStruct,
        String javaName, GroupLayout layout) {
        throw new UnsupportedOperationException();
    }

    public void addFunctionalInterface(Type.Function funcType, String javaName,
        FunctionDescriptor descriptor, Optional<List<String>> parameterNames) {
        throw new UnsupportedOperationException();
    }

    abstract public List<JavaFileObject> toFiles();

    public abstract String packageName();

    abstract boolean isEnclosedBySameName(String name);

    abstract protected void emitWithConstantClass(Consumer<ConstantBuilder> constantConsumer);
}
