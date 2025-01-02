/*
 * Copyright (c) 2020, 2025, Oracle and/or its affiliates. All rights reserved.
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
import static test.jextract.test8249757.test8249757_h.*;

/*
 * @test
 * @library /lib
 * @build testlib.TestUtils
 * @bug 8249757
 * @summary jextract should expose a way to load library from a given absolute path
 * @run main/othervm JtregJextract -l Test8249757 --use-system-load-library -t test.jextract.test8249757 test8249757.h
 * @build LibTest8249757Test
 * @run testng/othervm --enable-native-access=ALL-UNNAMED LibTest8249757Test
 */
public class LibTest8249757Test {
    @Test
    public void testSquare() {
        assertEquals(square(5), 25);
        assertEquals(square(16), 256);
        assertEquals(square(20), 400);
    }
}
