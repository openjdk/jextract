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

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySession;
import org.testng.annotations.Test;
import test.jextract.test8258605.*;
import static java.lang.foreign.MemorySegment.NULL;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;
import static test.jextract.test8258605.funcParam_h.*;

/*
 * @test id=classes
 * @bug 8258605
 * @summary regression: jextract can not handle function prototypes as function arguments
 * @library /lib
 * @run main/othervm JtregJextract -l FuncParam -t test.jextract.test8258605 funcParam.h
 * @run testng/othervm --enable-native-access=ALL-UNNAMED LibTest8258605Test
 */
/*
 * @test id=sources
 * @bug 8258605
 * @summary regression: jextract can not handle function prototypes as function arguments
 * @library /lib
 * @run main/othervm JtregJextractSources -l FuncParam -t test.jextract.test8258605 funcParam.h
 * @run testng/othervm --enable-native-access=ALL-UNNAMED LibTest8258605Test
 */
public class LibTest8258605Test {
    @Test
    public void testFunctionCallback() {
        try (Arena arena = Arena.openConfined()) {
             boolean[] callbackReached = new boolean[1];
             f(CB.allocate(i -> {
                 assertTrue(i == 10);
                 callbackReached[0] = true;
             }, arena.session()));
             assertTrue(callbackReached[0]);
        }
    }

    @Test
    public void testStructFunctionPointerCallback() {
        try (Arena arena = Arena.openConfined()) {
             boolean[] callbackReached = new boolean[1];

             // get struct Foo instance
             var foo = getFoo(arena);
             // make sure that foo.bar is not NULL
             assertFalse(Foo.bar$get(foo).equals(NULL));

             f2(foo, CB.allocate(i -> {
                 assertTrue(i == 42);
                 callbackReached[0] = true;
             }, arena.session()));
             assertTrue(callbackReached[0]);
        }
    }
}
