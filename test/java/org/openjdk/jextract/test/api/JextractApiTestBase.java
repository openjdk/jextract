/*
 * Copyright (c) 2020, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
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
package org.openjdk.jextract.test.api;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

import org.openjdk.jextract.Declaration;
import org.openjdk.jextract.JextractTool;
import org.openjdk.jextract.Type;

import static org.testng.Assert.*;

public class JextractApiTestBase {
    static final boolean isMacOSX =
            System.getProperty("os.name", "unknown").contains("OS X");
    static final boolean isWindows =
            System.getProperty("os.name", "unknown").startsWith("Windows");

    public static  Declaration.Scoped parse(String headerFilename, String... parseOptions) {
        Path header = Paths.get(System.getProperty("test.root", "."),
                "java", "org", "openjdk", "jextract", "test", "api", headerFilename);
        return JextractTool.parse(List.of(header), parseOptions);
    }

    public static void checkNames(List<Declaration> members, String... fields) {
        assertEquals(members.size(), fields.length);
        for (int i = 0; i < fields.length; i++) {
            assertEquals(members.get(i).name(), fields[i]);
        }
    }

    public static Declaration.Scoped checkScoped(Declaration.Scoped toplevel, String name, Declaration.Scoped.Kind kind,  String... fields) {
        Declaration.Scoped scoped = findDecl(toplevel, name, Declaration.Scoped.class);
        assertTrue(scoped.kind() == kind);
        checkNames(scoped.members(), fields);
        return scoped;
    }

    private static List<Declaration> getNamedFields(Declaration.Scoped scoped) {
        List<Declaration> fields = new ArrayList<>();
        scoped.members().forEach(d -> {
            if (d instanceof Declaration.Variable) {
                Declaration.Variable v = (Declaration.Variable) d;
                if (v.kind() == Declaration.Variable.Kind.FIELD) {
                    assert (!v.name().isEmpty());
                    fields.add(v);
                }
            } else if (d instanceof Declaration.Scoped) {
                Declaration.Scoped record = (Declaration.Scoped) d;
                if (record.name().isEmpty()) {
                    fields.addAll(getNamedFields(record));
                } else {
                    fields.add(record);
                }
            }
        });
        return fields;
    }

    public static Declaration.Scoped checkRecord(Declaration.Scoped scoped, String name, Declaration.Scoped.Kind kind,  String... fields) {
        assertTrue(scoped.kind() == kind);
        checkNames(getNamedFields(scoped), fields);
        return scoped;
    }

    public static Declaration.Scoped checkStruct(Declaration.Scoped toplevel, String name, String... fields) {
        return checkScoped(toplevel, name, Declaration.Scoped.Kind.STRUCT, fields);
    }

    public static Declaration.Scoped checkBitfields(Declaration.Scoped toplevel, String name, String... fields) {
        return checkScoped(toplevel, name, Declaration.Scoped.Kind.BITFIELDS, fields);
    }

    public static Declaration.Scoped checkUnion(Declaration.Scoped toplevel, String name, String... fields) {
        return checkScoped(toplevel, name, Declaration.Scoped.Kind.UNION, fields);
    }

    public static Declaration.Variable checkVariable(Declaration.Scoped scope, String name, Type type) {
        Declaration.Variable var = findDecl(scope, name, Declaration.Variable.class);
        assertTypeEquals(type, var.type());
        return var;
    }

    public static Declaration.Variable checkGlobal(Declaration.Scoped toplevel, String name, Type type) {
        Declaration.Variable global = checkVariable(toplevel, name, type);
        assertEquals(global.kind(), Declaration.Variable.Kind.GLOBAL);
        return global;
    }

    public static Declaration.Variable checkField(Declaration.Scoped record, String name, Type type) {
        Declaration.Variable global = checkVariable(record, name, type);
        assertEquals(global.kind(), Declaration.Variable.Kind.FIELD);
        return global;
    }

    public static Declaration.Variable checkBitField(Declaration.Scoped record, String name, Type type, int size) {
        Declaration.Variable global = checkVariable(record, name, type);
        assertEquals(global.kind(), Declaration.Variable.Kind.BITFIELD);
        assertEquals(global.layout().get().bitSize(), size);
        return global;
    }

    public static void checkFunction(Declaration.Function function, Type ret, Type... params) {
        assertTypeEquals(ret, function.type().returnType());
        assertEquals(function.parameters().size(), params.length);
        for (int i = 0 ; i < params.length ; i++) {
            assertTypeEquals(params[i], function.type().argumentTypes().get(i));
            Type paramType = function.parameters().get(i).type();
            if (paramType instanceof Type.Array) {
                assertTypeEquals(params[i], Type.pointer(((Type.Array) paramType).elementType()));
            } else {
                assertTypeEquals(params[i], function.parameters().get(i).type());
            }
        }
    }

    public static Declaration.Function checkFunction(Declaration.Scoped toplevel,String name , Type ret, Type... params) {
        Declaration.Function function = findDecl(toplevel, name, Declaration.Function.class);
        checkFunction(function, ret,params);
        return function;
    }

    public static Declaration.Constant checkConstant(Declaration.Scoped toplevel, String name, Type type, Object value) {
        Declaration.Constant constant = findDecl(toplevel, name, Declaration.Constant.class);
        assertTypeEquals(type, constant.type());
        assertEquals(value, constant.value());
        return constant;
    }

    public static Predicate<Declaration> byName(final String name) {
        return d -> d.name().equals(name);
    }

    public static Predicate<Declaration> byNameAndType(final String name, Class<? extends Declaration> declType) {
        return d -> declType.isAssignableFrom(d.getClass()) && d.name().equals(name);
    }

    public static Optional<Declaration> findDecl(Declaration.Scoped toplevel, Predicate<Declaration> filter) {
        return toplevel.members().stream().filter(filter).findAny();
    }

    @SuppressWarnings("unchecked")
    public static <D extends Declaration> D findDecl(Declaration.Scoped toplevel, String name, Class<D> declType) {
        Optional<Declaration> d = findDecl(toplevel, byNameAndType(name, declType));
        if (d.isEmpty()) {
            fail("No declaration with name " + name + " found in " + toplevel);
            return null;
        }
        return (D) d.get();
    }

    public static void assertTypeEquals(Type expected, Type found) {
        assertEquals(expected.getClass(), found.getClass());
        if (expected instanceof Type.Primitive) {
            assertEquals(expected, found);
        } else if (expected instanceof Type.Delegated) {
            assertEquals(((Type.Delegated)expected).kind(), ((Type.Delegated)found).kind());
            assertTypeEquals(((Type.Delegated)expected).type(), ((Type.Delegated)found).type());
        } else if (expected instanceof Type.Array) {
            assertEquals(((Type.Array)expected).kind(), ((Type.Array)found).kind());
            assertEquals(((Type.Array)expected).elementCount(), ((Type.Array)found).elementCount());
            assertTypeEquals(((Type.Array)expected).elementType(), ((Type.Array)found).elementType());
        } else if (expected instanceof Type.Declared) {
            assertEquals(((Type.Declared)expected).tree(), ((Type.Declared)found).tree());
        } else if (expected instanceof Type.Function) {
            assertTypeEquals(((Type.Function)expected).returnType(), ((Type.Function)found).returnType());
            assertEquals(((Type.Function)expected).argumentTypes().size(), ((Type.Function)found).argumentTypes().size());
            assertEquals(((Type.Function)expected).varargs(), ((Type.Function)found).varargs());
            for (int i = 0 ; i < ((Type.Function)expected).argumentTypes().size() ; i++) {
                assertTypeEquals(((Type.Function)expected).argumentTypes().get(i), ((Type.Function)found).argumentTypes().get(i));
            }
        }
    }

    public static Type unwrapDelegatedType(Type type, Type.Delegated.Kind kind) {
        assertTrue(type instanceof Type.Delegated,
                "Expecting Type.Delegated, got " + type.getClass());
        Type.Delegated delegated = (Type.Delegated) type;
        assertEquals(delegated.kind(), kind);
        return delegated.type();
    }

    public static Type unwrapPointerType(Type type) {
        return unwrapDelegatedType(type, Type.Delegated.Kind.POINTER);
    }

    public static Type unwrapTypedefType(Type type) {
        return unwrapDelegatedType(type, Type.Delegated.Kind.TYPEDEF);
    }

    public static Type unwrapArrayType(Type type, long size) {
        assertTrue(type instanceof Type.Array,
                "Expecting Type.Array, got " + type.getClass());
        Type.Array arType = (Type.Array) type;
        assertEquals(arType.elementCount().getAsLong(), size);
        return arType.elementType();
    }

    public static Type unwrapArrayType(Type type) {
        assertTrue(type instanceof Type.Array,
                "Expecting Type.Array, got " + type.getClass());
        Type.Array arType = (Type.Array) type;
        assertTrue(arType.elementCount().isEmpty());
        return arType.elementType();
    }

    static class TypeUnwrapper {
        private Type type;

        private TypeUnwrapper(Type type) {
            this.type = type;
        }

        public static TypeUnwrapper of(Type type) {
            return new TypeUnwrapper(type);
        }

        public TypeUnwrapper unwrapPointer() {
            type = unwrapPointerType(type);
            return this;
        }

        public TypeUnwrapper unwrapTypedef() {
            type = unwrapTypedefType(type);
            return this;
        }

        public TypeUnwrapper unwrapArray(long size) {
            type = unwrapArrayType(type, size);
            return this;
        }

        public TypeUnwrapper unwrapArray() {
            type = unwrapArrayType(type);
            return this;
        }

        public Type get() {
            return type;
        }
    }
}
