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

import org.openjdk.jextract.Declaration;
import org.openjdk.jextract.Type;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import static org.testng.Assert.*;

public class TestTypedef extends JextractApiTestBase {
    Declaration.Scoped root;

    @BeforeClass
    public void parse() {
        root = parse("testTypedef.h");
        System.out.println(root);
    }

    private Declaration[] findAllWithName(Declaration.Scoped scope, String name) {
        return scope.members().stream().filter(byName(name)).toArray(Declaration[]::new);
    }

    public static Type getTypedefType(Declaration.Scoped scope, String name) {
        Declaration.Typedef d = findDecl(scope, name, Declaration.Typedef.class);
        Type type = d.type();
        // Typedef declaration should return canonical type
        if (type instanceof Type.Delegated) {
            assertNotEquals(((Type.Delegated) type).kind(), Type.Delegated.Kind.TYPEDEF);
        }
        return d.type();
    }

    private Declaration.Scoped assertDeclaredTypedef(Declaration.Typedef decl) {
        Type type = decl.type();
        assertTrue(type instanceof Type.Declared, "Expecting Type.Declared, got " + type.getClass());
        return ((Type.Declared) type).tree();
    }

    private Declaration.Scoped assertAnonymousRecord(Declaration.Scoped scope, String name) {
        Declaration[] ar = findAllWithName(scope, name);
        assertEquals(ar.length, 1);
        assertTrue(ar[0] instanceof Declaration.Typedef, "Expectint Declaration.Typedef, but got " + ar[0].getClass());
        Declaration.Scoped record = assertDeclaredTypedef((Declaration.Typedef) ar[0]);
        return record;
    }

    private Declaration.Scoped assertNamedRecord(Declaration.Scoped scope, String name) {
        Declaration[] ar = findAllWithName(scope, name);
        assertEquals(ar.length, 1);
        assertTrue(ar[0] instanceof Declaration.Scoped, "Expectint Declaration.Scoped, but got " + ar[0].getClass());
        return (Declaration.Scoped) ar[0];
    }

    @Test
    public void NoDuplicateSameNameTypedef() {
        // When typedef a named record with the same name, present the scoped
        // declaration and ignore the typedef
        Declaration.Scoped s = assertNamedRecord(root, "Point3D");
        assertEquals(s.kind(), Declaration.Scoped.Kind.STRUCT);
        checkNames(s.members(), "i", "j", "k");

        s = assertNamedRecord(root, "SIZE");
        assertEquals(s.kind(), Declaration.Scoped.Kind.ENUM);
        checkNames(s.members(), "XS", "S", "M", "L", "XL");
    }

    @Test
    public void TypedefReferences() {
        // When reference to a typedef, the Type should be Type.Delegated
        // With the type to be the referenced type
        Declaration.Scoped pt3d = checkStruct(root, "Point3D", "i", "j", "k");
        Declaration.Function drawParamid = findDecl(root, "drawParamid", Declaration.Function.class);
        Type.Function fnType = drawParamid.type();
        // Array in function argument is lowered to pointer
        Type type = TypeUnwrapper.of(fnType.argumentTypes().get(0))
                        .unwrapPointer().unwrapTypedef().get();
        assertEquals(type, Type.declared(pt3d));

        Declaration.Function do_ops = findDecl(root, "do_ops", Declaration.Function.class);
        fnType = do_ops.type();
        type = unwrapTypedefType(fnType.returnType());
        assertEquals(type, getTypedefType(root, "op_sequence"));
        type = fnType.argumentTypes().get(0);
        type = unwrapTypedefType(type);
        assertEquals(type, getTypedefType(root, "int_op"));
        type = fnType.argumentTypes().get(1);
        type = unwrapTypedefType(type);
        assertEquals(type, getTypedefType(root, "count_t"));
    }

    @Test
    public void TypedefsToSameType()  {
        // For typedef declaration, the type will be the canonical type
        // Which means, the type will not be another typedef
        // However, it can be other delegated type or an array
        Declaration.Scoped pt = checkStruct(root, "Point", "i", "j");
        Type.Declared type = Type.declared(pt);
        assertEquals(getTypedefType(root, "POINT"), type);
        assertEquals(getTypedefType(root, "point_t"), type);

        Type canonical = TypeUnwrapper.of(getTypedefType(root, "rectangle"))
                .unwrapArray(4)
                // FIXME? If we would like to generate array using typedef type
                // then we need to use typedef as array element type and
                // requires following line to pass the test.
                // .unwrapTypedef()
                .get();
        assertEquals(canonical, type);

        Declaration.Variable canvas = findDecl(root, "canvas", Declaration.Variable.class);
        assertEquals(canvas.kind(), Declaration.Variable.Kind.GLOBAL);
        Type ref = TypeUnwrapper.of(canvas.type())
            .unwrapTypedef()
            .unwrapArray(4)
            .get();
        assertEquals(ref, type);

        getTypedefType(root, "count_t");
    }

    @Test
    public void TypedefsArrays()  {
        Type intType = getTypedefType(root, "cordinate_t");

        // As noted earlier, we currently have canonical array element type from typedef
        Type type = getTypedefType(root, "location2D");
        Type elementType = unwrapArrayType(type, 2);
        assertEquals(elementType, intType);

        Type count_t = getTypedefType(root, "count_t");
        type = getTypedefType(root, "dimensions");
        elementType = unwrapArrayType(type);
        assertEquals(elementType, count_t);
        type = getTypedefType(root, "count_ptr");
        assertEquals(type, Type.pointer(count_t));
    }

    @Test
    public void AnonymousRecordTypedef() {
        // For anonymous typedef, present the typedef declaration and
        // the Scope declaration can be obtained via Variable.type()
        Declaration.Scoped record = assertAnonymousRecord(root, "op_sequence");
        assertEquals(record.kind(), Declaration.Scoped.Kind.STRUCT);
        checkNames(record.members(), "times", "op");

        record = assertAnonymousRecord(root, "IntOrFloat");
        assertEquals(record.kind(), Declaration.Scoped.Kind.UNION);
        checkNames(record.members(), "i", "f");

        record = assertAnonymousRecord(root, "codetype_t");
        assertEquals(record.kind(), Declaration.Scoped.Kind.ENUM);
        checkNames(record.members(), "Java", "C", "CPP", "Python", "Ruby");
    }

    @Test
    public void CheckAnonyousDeclarations() {
        // Should we expunge anonymous declaration?
        // They only needed if referenced as a field or gloabal variable
        // Exception enum, as they can be used as pleased, so we need to
        // elevate them into constants.
        // Anyhow, current implementation pass through enum, not elevate them.
        // So we just check that
        Declaration[] ar = findAllWithName(root, "");
        assertEquals(ar.length, 2);
        Declaration.Scoped e = (Declaration.Scoped) ar[0];
        assertEquals(e.kind(), Declaration.Scoped.Kind.ENUM);
        checkNames(e.members(), "RED", "GREEN", "BLUE");
        e = (Declaration.Scoped) ar[1];
        assertEquals(e.kind(), Declaration.Scoped.Kind.ENUM);
        checkNames(e.members(), "Java", "C", "CPP", "Python", "Ruby");
    }

    @Test
    public void CheckFunctionPointers() {
        Type intType = getTypedefType(root, "cordinate_t");
        Type intOpType = getTypedefType(root, "int_op");
        assertEquals(intOpType, Type.pointer(Type.function(false, intType, intType)));
        Type intOp2Type = getTypedefType(root, "int_op2");
        assertEquals(intOp2Type, Type.pointer(Type.function(false, intType, intType, intType)));

        checkGlobal(root, "another_int_op", intOpType);

        Declaration.Function getFn = findDecl(root, "getFn", Declaration.Function.class);
        assertEquals(getFn.parameters().size(), 0);
        Type.Delegated retType = (Type.Delegated) getFn.type().returnType();
        assertTrue(retType.kind() == Type.Delegated.Kind.POINTER);
        Type.Function fnType = (Type.Function) retType.type();
        assertEquals(fnType.returnType(), Type.void_());
        assertEquals(fnType.argumentTypes().get(1),
                Type.typedef("count_t", getTypedefType(root, "count_t")));
    }
}
