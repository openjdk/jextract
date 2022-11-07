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
import static org.testng.Assert.assertEquals;

import test.jextract.test8252465.*;
import static test.jextract.test8252465.test8252465_h.*;
import test.jextract.test8252465.*;

/*
 * @test id=classes
 * @bug 8252465
 * @summary jextract generates wrong layout and varhandle when different structs have same named field
 * @library /lib
 * @run main/othervm JtregJextract -t test.jextract.test8252465 test8252465.h
 * @run testng/othervm -Dforeign.restricted=permit LibTest8252465Test
 */
/*
 * @test id=sources
 * @bug 8252465
 * @summary jextract generates wrong layout and varhandle when different structs have same named field
 * @library /lib
 * @run main/othervm JtregJextractSources -t test.jextract.test8252465 test8252465.h
 * @run testng/othervm -Dforeign.restricted=permit LibTest8252465Test
 */
public class LibTest8252465Test {
    @Test
    public void test() {
        try (var arena = Arena.openConfined()) {
            var foo = Foo.allocate(arena);
            Foo.x$set(foo, 3.14f);
            assertEquals(Foo.x$get(foo), 3.14f, 0.001f);
            var bar = Bar.allocate(arena);
            Bar.x$set(bar, -42);
            assertEquals(Bar.x$get(bar), -42);
        }
    }
}
