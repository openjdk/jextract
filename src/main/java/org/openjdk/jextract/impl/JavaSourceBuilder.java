/*
 * Copyright (c) 2021, Oracle and/or its affiliates. All rights reserved.
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

import jdk.incubator.foreign.FunctionDescriptor;
import jdk.incubator.foreign.GroupLayout;
import jdk.incubator.foreign.MemoryLayout;
import org.openjdk.jextract.Declaration;
import org.openjdk.jextract.Type;

import javax.tools.JavaFileObject;
import java.lang.invoke.MethodType;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;

public abstract class JavaSourceBuilder {

    public void addVar(String javaName, String nativeName, MemoryLayout layout, Optional<String> fiName) {
        throw new UnsupportedOperationException();
    }

    public void addFunction(String javaName, String nativeName, FunctionDescriptor descriptor, boolean isVarargs, List<String> parameterNames) {
        throw new UnsupportedOperationException();
    }

    public void addConstant(String javaName, Class<?> type, Object value) {
        throw new UnsupportedOperationException();
    }

    public void addTypedef(String name, String superClass, Type type) {
        throw new UnsupportedOperationException();
    }

    public StructBuilder addStruct(String name, Declaration parent, GroupLayout layout, Type type) {
        throw new UnsupportedOperationException();
    }

    public String addFunctionalInterface(String name, FunctionDescriptor descriptor, Optional<List<String>> parameterNames) {
        throw new UnsupportedOperationException();
    }

    abstract public List<JavaFileObject> toFiles();

    public abstract String packageName();

    Set<String> nestedClassNames = new HashSet<>();
    int nestedClassNameCount = 0;

    /*
     * We may have case-insensitive name collision! A C program may have
     * defined structs/unions/typedefs with the names FooS, fooS, FoOs, fOOs.
     * Because we map structs/unions/typedefs to nested classes of header classes,
     * such a case-insensitive name collision is problematic. This is because in
     * a case-insensitive file system javac will overwrite classes for
     * Header$CFooS, Header$CfooS, Header$CFoOs and so on! We solve this by
     * generating unique case-insensitive names for nested classes.
     */
    final String uniqueNestedClassName(String name) {
        name = Utils.javaSafeIdentifier(name);
        var notSeen = nestedClassNames.add(name.toLowerCase());
        var notEnclosed = !isEnclosedBySameName(name.toLowerCase());
        return notSeen && notEnclosed? name : (name + "$" + nestedClassNameCount++);
    }

    abstract boolean isEnclosedBySameName(String name);

    abstract protected void emitWithConstantClass(Consumer<ConstantBuilder> constantConsumer);
}
