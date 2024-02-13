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

import java.lang.foreign.Arena;
import java.lang.foreign.GroupLayout;
import java.lang.foreign.MemorySegment;
import org.testng.annotations.Test;
import static org.testng.Assert.assertEquals;
import static test.jextract.test8254983.test8254983_h.*;
import test.jextract.test8254983.*;

/*
 * @test
 * @library /lib
 * @bug 8254983
 * @summary jextract fails to hande layout paths nested structs/union
 * @run main/othervm JtregJextract -t test.jextract.test8254983 test8254983.h
 * @build LibTest8254983Test
 * @run testng/othervm -Dforeign.restricted=permit LibTest8254983Test
 */
public class LibTest8254983Test {
    @Test
    public void testOuterStruct() {
        try (Arena arena = Arena.ofConfined()) {
            assertEquals(((GroupLayout)Foo._struct.layout()).memberLayouts().size(), 1);
            MemorySegment str = Foo._struct.allocate(arena);
            Foo._struct.x(str, 42);
            assertEquals(Foo._struct.x(str), 42);
        }
    }

    @Test
    public void testInnerStruct() {
        assertEquals(((GroupLayout)Foo._union._struct.layout()).memberLayouts().size(), 2);
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment str = Foo._union._struct.allocate(arena);
            Foo._union._struct.x(str, 42);
            assertEquals(Foo._union._struct.x(str), 42);
        }
    }
}
