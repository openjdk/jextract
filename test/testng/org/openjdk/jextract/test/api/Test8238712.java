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

import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertTrue;

public class Test8238712 extends JextractApiTestBase {
    @Test
    public void test8238712() {
        Declaration.Scoped d = parse("Test8238712.h");
        Declaration.Scoped structFoo = checkStruct(d, "foo", "n", "ptr");
        Type intType = ((Declaration.Variable) structFoo.members().get(0)).type();
        Type fooType = Type.declared(structFoo);
        checkFunction(d, "withRecordTypeArg", intType, intType, fooType);
        checkFunction(d, "returnRecordType", fooType);
        // Opaque struct, have no field
        Declaration.Scoped structBar = checkStruct(d, "bar");
        //assertTrue(Declaration.layoutFor(structBar).isEmpty());
        Type barType = Type.declared(structBar);
        // Function with opaque struct won't work but should have cursor for tool to handle
        checkFunction(d, "returnBar", barType);
        checkFunction(d, "withBar", Type.void_(), barType);
        // Function use pointer to opaque struct should be OK
        Type barPointer = Type.pointer(barType);
        checkFunction(d, "nextBar", barPointer, barPointer);
    }
}
