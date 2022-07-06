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
import java.lang.foreign.NativeArena;
import test.jextract.test8261511.*;
import static org.testng.Assert.assertEquals;
import static test.jextract.test8261511.test8261511_h.*;

/*
 * @test id=classes
 * @bug 8261511
 * @summary jextract does not generate accessor for MemorySegement typed values
 * @library /lib
 * @run main/othervm JtregJextract -l Test8261511 -t test.jextract.test8261511 test8261511.h
 * @run testng/othervm --enable-native-access=ALL-UNNAMED Test8261511
 */
/*
 * @test id=sources
 * @bug 8261511
 * @summary jextract does not generate accessor for MemorySegement typed values
 * @library /lib
 * @run main/othervm JtregJextractSources -l Test8261511 -t test.jextract.test8261511 test8261511.h
 * @run testng/othervm --enable-native-access=ALL-UNNAMED Test8261511
 */
public class Test8261511 {
    @Test
    public void test() {
        try (NativeArena session = NativeArena.openConfined()) {
            var funcPtr = Foo.sum$get(get_foo(session));
            assertEquals(Foo.sum.apply(funcPtr, 15,20), 35);
            assertEquals(sum(1.2, 4.5), 5.7, 0.001);
        }
    }
}
