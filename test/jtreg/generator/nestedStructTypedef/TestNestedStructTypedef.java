/*
 * Copyright (c) 2023, 2024, Oracle and/or its affiliates. All rights reserved.
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

import org.testng.annotations.Test;

import static org.testng.Assert.*;

import test.jextract.nested.typedef.*;

import java.lang.foreign.GroupLayout;

/*
 * @test
 * @library /lib
 * @build testlib.TestUtils
 * @run main/othervm JtregJextract -t test.jextract.nested.typedef nestedStructTypedef.h
 * @build TestNestedStructTypedef
 * @run testng/othervm --enable-native-access=ALL-UNNAMED TestNestedStructTypedef
 */
public class TestNestedStructTypedef {

    @Test
    public void testMacroFields() {
        checkLayout(T.layout());
    }

    void checkLayout(GroupLayout layout) {
        assertEquals(layout.memberLayouts().get(0), nestedStructTypedef_h.C_INT.withName("x"));
    }
}
