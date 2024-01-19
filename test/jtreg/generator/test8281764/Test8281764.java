/*
 * Copyright (c) 2022, 2024, Oracle and/or its affiliates. All rights reserved.
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
import test.jextract.test8281764.*;

/*
 * @test
 * @bug 8281764
 * @summary jextract does not generate parameter names for function pointer typedefs
 * @library /lib
 * @run main/othervm JtregJextract -l Test8281764 -t test.jextract.test8281764 test8281764.h
 * @build Test8281764
 * @run testng/othervm --enable-native-access=ALL-UNNAMED Test8281764
 */
public class Test8281764 {
    @Test
    public void testFunctionalInterfaceParameterNames() throws NoSuchMethodException {
        var apply = func.Function.class.getMethod("apply", int.class);
        assertEquals(apply.getParameters()[0].getName(), "foo");
        apply = fptr.Function.class.getMethod("apply", int.class, int.class);
        assertEquals(apply.getParameters()[0].getName(), "x");
        assertEquals(apply.getParameters()[1].getName(), "y");
    }
}
