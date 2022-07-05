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


import java.lang.foreign.MemorySegment;
import java.lang.foreign.NativeArena;

import org.testng.annotations.Test;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

import test.jextract.test8244412.*;
import static test.jextract.test8244412.test8244412_h.*;

/*
 * @test id=classes
 * @library /lib
 * @bug 8244412
 * @summary jextract should generate static utils class for primitive typedefs
 * @run main/othervm JtregJextract -t test.jextract.test8244412 test8244412.h
 * @run testng/othervm --enable-native-access=ALL-UNNAMED LibTest8244412Test
 */
/*
 * @test id=sources
 * @library /lib
 * @bug 8244412
 * @summary jextract should generate static utils class for primitive typedefs
 * @run main/othervm JtregJextractSources -t test.jextract.test8244412 test8244412.h
 * @run testng/othervm --enable-native-access=ALL-UNNAMED LibTest8244412Test
 */
public class LibTest8244412Test {
    @Test
    public void test() {
        try (var session = NativeArena.openConfined()) {
            var addr = session.allocate(mysize_t, 0L);
            assertEquals(addr.get(C_LONG_LONG, 0), 0L);
            addr.set(C_LONG_LONG, 0, 13455566L);
            assertEquals(addr.get(C_LONG_LONG, 0), 13455566L);
        }
    }
}
