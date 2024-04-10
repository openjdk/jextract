/*
 * Copyright (c) 2019, 2024, Oracle and/or its affiliates. All rights reserved.
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

import static test.jextract.alias_mul.libAsmSymbol_h.*;

import org.testng.annotations.Test;
import static org.testng.Assert.assertEquals;

/*
 * @test
 * @requires os.family != "windows"
 * @library /lib
 * @build testlib.TestUtils
 * @run main/othervm JtregJextract -l AsmSymbol --use-system-load-library -t test.jextract.alias_mul libAsmSymbol.h
 * @build TestAliasMul
 * @run testng/othervm --enable-native-access=ALL-UNNAMED TestAliasMul
 */
public class TestAliasMul {

    @Test
    public void checkGlobalVar() {
        assertEquals(2, foo());
    }

    @Test
    public void checkGlobalFunction() {
        assertEquals(2, func(1, 2));
    }
}
