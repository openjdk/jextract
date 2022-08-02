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

import java.lang.foreign.MemorySession;
import org.testng.annotations.Test;
import test.jextract.test7903239.*;
import static org.testng.Assert.assertEquals;

/*
 * @test id=classes
 * @bug 7903239
 * @summary ofAddress factory of function pointer type is wrong for struct returns
 * @library /lib
 * @run main/othervm JtregJextract -l Test7903239 -t test.jextract.test7903239 test7903239.h
 * @run testng/othervm --enable-native-access=ALL-UNNAMED Test7903239
 */
/*
 * @test id=sources
 * @bug 7903239
 * @summary ofAddress factory of function pointer type is wrong for struct returns
 * @library /lib
 * @run main/othervm JtregJextractSources -l Test7903239 -t test.jextract.test7903239 test7903239.h
 * @run testng/othervm --enable-native-access=ALL-UNNAMED Test7903239
 */
public class Test7903239 {
    @Test
    public void testOfAddress() {
        try (MemorySession session = MemorySession.openConfined()) {
            var foo = test7903239_h.foo$SEGMENT();

            var barA = Foo.a(foo, session).apply();
            var barB = Foo.b(foo, session).apply(100);

            assertEquals(Bar.a$get(barA), 5);
            assertEquals(Bar.a$get(barB), 100);
        }
    }
}
