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

import org.openjdk.jextract.Declaration;
import org.openjdk.jextract.Type;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import testlib.JextractApiTestBase;

public class TestMacros extends JextractApiTestBase {
    Declaration.Scoped badMacro;
    Declaration.Scoped foo;
    Declaration.Scoped bar;
    private final static Type C_INT = Type.primitive(Type.Primitive.Kind.Int);

    @BeforeClass
    public void parse() {
        // We need stdint.h for pointer macro, otherwise evaluation failed and Constant declaration is not created
        Path builtinInc = Paths.get(System.getProperty("java.home"), "conf", "jextract");
        badMacro = parse("badMacros.h", "-I", builtinInc.toString());

        foo = checkStruct(badMacro, "foo", "ptrFoo", "ptrBar");
        bar = checkStruct(badMacro, "bar", "ptrFoo", "arFooPtr");
    }

    @Test
    public void testBadMacros() {
        checkConstant(badMacro, "INVALID_INT_CONSUMER",
            Type.pointer(Type.function(false, Type.void_(), C_INT)),
            0L);
        // Record type in macro definition are erased to void
        checkConstant(badMacro, "NO_FOO", Type.pointer(), 0L);
        checkConstant(badMacro, "INVALID_INT_ARRAY_PTR", Type.pointer(Type.pointer(C_INT)), 0L);
    }

    @Test
    public void verifyFunctions() {
        checkFunction(badMacro, "func", Type.void_(),
            Type.pointer(Type.declared(bar)), Type.pointer(Type.declared(foo)));
        checkFunction(badMacro, "withArray", Type.void_(),
            Type.pointer(Type.typedef("foo_t", Type.pointer(Type.declared(foo)))));
    }

    @Test
    public void verifyGlobals() {
        checkGlobal(badMacro, "op", Type.pointer(
                Type.function(false, Type.void_(), C_INT, Type.pointer(C_INT))));
    }

    @Test
    public void verifyFields() {
        Type foo_t = Type.typedef("foo_t", Type.pointer(Type.declared(foo)));
        checkField(foo, "ptrFoo", foo_t);
        checkField(foo, "ptrBar", Type.pointer(Type.declared(bar)));
        checkField(bar, "ptrFoo", foo_t);
        checkField(bar, "arFooPtr", Type.pointer(foo_t));
    }
}
