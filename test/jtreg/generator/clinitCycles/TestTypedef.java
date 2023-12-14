/* Copyright (c) 2023, Oracle and/or its affiliates. All rights reserved.
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

import test.jextract.clinit.*;

import java.lang.foreign.ValueLayout;

/*
 * @test id=classes
 * @library /lib
 * @run main/othervm JtregJextract -l Func -t test.jextract.clinit -Djextract.decls.per.header=1 clinit_typedef.h
 * @build TestTypedef
 * @run testng/othervm --enable-native-access=ALL-UNNAMED TestTypedef
 */
/*
 * @test id=sources
 * @library /lib
 * @run main/othervm JtregJextractSources -t test.jextract.clinit -Djextract.decls.per.header=1 clinit_typedef.h
 * @build TestTypedef
 * @run testng/othervm --enable-native-access=ALL-UNNAMED TestTypedef
 */
public class TestTypedef {

    @Test
    public void TestTypedef() {
        ValueLayout layout = clinit_typedef_h.C_INT;
        assertNotNull(layout);
        assertEquals(layout, clinit_typedef_h.one);
        assertEquals(layout, clinit_typedef_h.two);
        assertEquals(layout, clinit_typedef_h.three);
    }
}
