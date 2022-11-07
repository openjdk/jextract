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
import java.lang.foreign.MemorySegment;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.MemorySession;
import java.lang.foreign.SegmentAllocator;
import org.testng.annotations.Test;
import test.jextract.test8246341.*;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;
import static test.jextract.test8246341.test8246341_h.*;

/*
 * @test id=classes
 * @bug 8246341
 * @summary jextract should generate Cpointer utilities class
 * @library /lib
 * @run main/othervm JtregJextract -l Test8246341 -t test.jextract.test8246341 test8246341.h
 * @run testng/othervm --enable-native-access=ALL-UNNAMED LibTest8246341Test
 */
/*
 * @test id=sources
 * @bug 8246341
 * @summary jextract should generate Cpointer utilities class
 * @library /lib
 * @run main/othervm JtregJextractSources -l Test8246341 -t test.jextract.test8246341 test8246341.h
 * @run testng/othervm --enable-native-access=ALL-UNNAMED LibTest8246341Test
 */
public class LibTest8246341Test {
    @Test
    public void testPointerArray() {
        boolean[] callbackCalled = new boolean[1];
        try (Arena arena = Arena.openConfined()) {
            var callback = func$callback.allocate((argc, argv) -> {
                callbackCalled[0] = true;
                assertEquals(argc, 4);
                assertEquals(argv.get(C_POINTER, 0).getUtf8String(0), "java");
                assertEquals(argv.get(C_POINTER, C_POINTER.byteSize() * 1).getUtf8String(0), "python");
                assertEquals(argv.get(C_POINTER, C_POINTER.byteSize() * 2).getUtf8String(0), "javascript");
                assertEquals(argv.get(C_POINTER, C_POINTER.byteSize() * 3).getUtf8String(0), "c++");
            }, arena.session());
            func(callback);
        }
        assertTrue(callbackCalled[0]);
    }

    @Test
    public void testPointerAllocate() {
        try (var arena = Arena.openConfined()) {
            var addr = arena.allocate(C_POINTER);
            addr.set(C_POINTER, 0, MemorySegment.NULL);
            fillin(addr);
            assertEquals(addr.get(C_POINTER, 0).getUtf8String(0), "hello world");
        }
    }
}
