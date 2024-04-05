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
import test.jextract.allocCallback.*;

import java.lang.foreign.Arena;

import static org.testng.Assert.assertEquals;

/*
 * @test
 * @bug 7903239
 * @summary ofAddress factory of function pointer type is wrong for struct returns
 * @library /lib
 * @build testlib.TestUtils
 * @run main/othervm JtregJextract -l AllocCallback --use-system-load-library -t test.jextract.allocCallback alloc_callback.h
 * @build TestAllocCallback
 * @run testng/othervm --enable-native-access=ALL-UNNAMED TestAllocCallback
 */
public class TestAllocCallback {
    @Test
    public void testOfAddress() {
        try (Arena arena = Arena.ofConfined()) {
            var foo = alloc_callback_h.foo();

            var barA = Foo.a.invoke(Foo.a(foo), arena);
            var barB = Foo.b.invoke(Foo.b(foo), arena, 100);

            assertEquals(Bar.a(barA), 5);
            assertEquals(Bar.a(barB), 100);
        }
    }
}
