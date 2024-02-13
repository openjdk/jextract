/*
 * Copyright (c) 2020, 2024, Oracle and/or its affiliates. All rights reserved.
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
import static org.testng.Assert.assertEquals;
import static test.jextract.test8253390.test8253390_h.*;

/*
 * @test
 * @library /lib
 * @bug 8253390
 * @summary jextract should quote string literals
 * @run main/othervm JtregJextract -t test.jextract.test8253390 test8253390.h
 * @build LibTest8253390Test
 * @run testng/othervm --enable-native-access=ALL-UNNAMED LibTest8253390Test
 */
public class LibTest8253390Test {
    @Test
    public void testSquare() {
        assertEquals(GREETING().getString(0), "hello\nworld");
        assertEquals(GREETING2().getString(0), "hello\tworld");
    }
}
