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
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import testlib.JextractApiTestBase;

import static org.testng.Assert.*;
import static org.openjdk.jextract.Type.Primitive.Kind.*;
import static org.openjdk.jextract.Type.Delegated.Kind.*;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.EnumSet;

public class TestAtomic extends JextractApiTestBase {
    Declaration.Scoped atomic;

    @BeforeClass
    public void parse() {
        // We need stdatomic.h
        Path builtinInc = Paths.get(System.getProperty("java.home"), "conf", "jextract");
        atomic = parse("atomic.h", "-I", builtinInc.toString());
    }

    @Test(dataProvider = "atomicTypes")
    public void testAtomic(String varName, Type expected) {
        Declaration.Variable var = findDecl(atomic, varName, Declaration.Variable.class);
        var kinds = EnumSet.of(ATOMIC);
        if (varName.startsWith("U_")) {
            kinds.add(UNSIGNED);
        }
        if (varName.startsWith("S_")) {
            kinds.add(SIGNED);
        }
        checkType(var.type(), kinds, expected);
    }

    @DataProvider
    static Object[][] atomicTypes() {
        return new Object[][]{
                new Object[] { "BOOL", Type.primitive(Bool) },
                new Object[] { "CHAR", Type.primitive(Char) },
                new Object[] { "S_CHAR", Type.primitive(Char) },
                new Object[] { "U_CHAR", Type.primitive(Char) },
                new Object[] { "SHORT", Type.primitive(Short) },
                new Object[] { "U_SHORT", Type.primitive(Short) },
                new Object[] { "INT", Type.primitive(Int) },
                new Object[] { "U_INT", Type.primitive(Int) },
                new Object[] { "LONG", Type.primitive(Long) },
                new Object[] { "U_LONG", Type.primitive(Long) },
                new Object[] { "LONGLONG", Type.primitive(LongLong) },
                new Object[] { "U_LONGLONG", Type.primitive(LongLong) },
        };
    }

    static void checkType(Type t, EnumSet<Type.Delegated.Kind> kinds, Type expected) {
        while (t instanceof Type.Delegated delegated) {
            kinds.remove(delegated.kind());
            t = delegated.type();
        }
        assertTrue(kinds.isEmpty(), "Missing kinds: " + kinds);
        assertEquals(t, expected);
    }
}
