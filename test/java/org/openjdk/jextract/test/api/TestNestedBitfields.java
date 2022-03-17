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
import org.testng.annotations.Test;

public class TestNestedBitfields extends JextractApiTestBase {

    @Test
    public void testNestedBitfields() {
        Declaration.Scoped d = parse("nestedbitfields.h");
        Declaration.Scoped foo = checkStruct(d, "Foo", "");
        Declaration.Scoped foo$anon = checkStruct(foo, "", "");
        checkBitfields(foo$anon, "", "a", "b");

        Declaration.Scoped bar = checkStruct(d, "Bar", "");
        Declaration.Scoped bar$anon = checkStruct(bar, "", "");
        Declaration.Scoped bar$anon$anon = checkStruct(bar$anon, "", "");
        checkBitfields(bar$anon$anon, "", "a", "b");
    }
}

