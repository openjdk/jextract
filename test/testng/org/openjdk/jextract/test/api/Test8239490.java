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
        Declaration.Scoped structFoo = checkStruct(d, "Foo");
        // check Bar
        Declaration.Scoped structBar = checkStruct(d, "Bar", "z");
        checkField(structBar, "z", Type.array(1, Type.declared(structFoo)));

        // check Baz
        Declaration.Scoped structBaz = checkStruct(d, "Baz", "z");
        checkField(structBaz, "z", Type.array(1, Type.declared(structBar)));
    }
}
