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

import org.testng.annotations.Test;
import static org.testng.Assert.assertEquals;
import static test.jextract.test8239918.test8239918_h.*;

/*
 * @test id=classes
 * @bug 8239918
 * @summary jextract generates uncompilable code for no argument C function
 * @library /lib
 * @run main/othervm JtregJextract -l Test8239918 -t test.jextract.test8239918 test8239918.h
 * @build LibTest8239918Test
 * @run testng/othervm --enable-native-access=ALL-UNNAMED LibTest8239918Test
 */
/*
 * @test id=sources
 * @bug 8239918
 * @summary jextract generates uncompilable code for no argument C function
 * @library /lib
 * @run main/othervm JtregJextractSources -l Test8239918 -t test.jextract.test8239918 test8239918.h
 * @build LibTest8239918Test
 * @run testng/othervm --enable-native-access=ALL-UNNAMED LibTest8239918Test
 */
public class LibTest8239918Test {
    @Test
    public void testRand() {
        assertEquals(rand(), 1729);
    }
}
