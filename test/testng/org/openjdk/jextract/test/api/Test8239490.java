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
import org.testng.annotations.Test;
import testlib.JextractApiTestBase;

public class Test8239490 extends JextractApiTestBase {
    @Test
    public void test8239490() {
        Declaration.Scoped d = parse("Test8239490.h");
        // check Foo
        String[] fooBitfieldNames = { "a", "b", "c" };
        int[] fooBitfieldSizes = { 1, 1, 30 };
        Declaration.Scoped structFoo = checkStruct(d, "Foo", "");
        Declaration.Scoped bitfieldsFoo = checkBitfields(structFoo, "", "a", "b", "c");
        Type intType = ((Declaration.Variable)bitfieldsFoo.members().get(0)).type();
        for (int i = 0 ; i < fooBitfieldNames.length ; i++) {
            checkBitField(bitfieldsFoo, fooBitfieldNames[i], intType, fooBitfieldSizes[i]);
        }
        // check Bar
        String[] barBitfieldNames = { "x", "y" };
        int[] barBitfieldSizes = { 1, 31 };
        Declaration.Scoped structBar = checkStruct(d, "Bar", "", "z");
        Declaration.Scoped bitfieldsBar = checkBitfields(structBar, "", "x", "y");
        for (int i = 0 ; i < barBitfieldNames.length ; i++) {
            checkBitField(bitfieldsBar, barBitfieldNames[i], intType, barBitfieldSizes[i]);
        }
        checkField(structBar, "z", Type.array(1, Type.declared(structFoo)));

        // check Baz
        String[] bazBitfieldNames = { "x", "y" };
        int[] bazBitfieldSizes = { 1, 63 };
        Declaration.Scoped structBaz = checkStruct(d, "Baz", "", "z");
        Declaration.Scoped bitfieldsBaz = checkBitfields(structBaz, "", "x", "y");
        Type longType = ((Declaration.Variable)bitfieldsBaz.members().get(0)).type();
        for (int i = 0 ; i < bazBitfieldNames.length ; i++) {
            checkBitField(bitfieldsBaz, bazBitfieldNames[i], longType, bazBitfieldSizes[i]);
        }
        checkField(structBaz, "z", Type.array(1, Type.declared(structBar)));
    }
}
