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

import org.testng.annotations.Test;

import java.lang.foreign.Arena;
import java.util.stream.IntStream;
import java.lang.foreign.MemorySegment;
import static org.testng.Assert.assertEquals;

import test.jextract.arrayparam.*;
import static test.jextract.arrayparam.arrayparam_h.*;

/*
 * @test
 * @bug 8252121
 * @summary jextract generated code fails with ABI for typedefed array type parameters
 * @library /lib
 * @run main/othervm JtregJextract -t test.jextract.arrayparam -l Arrayparam --use-system-load-library arrayparam.h
 * @build Test8252121
 * @run testng/othervm --enable-native-access=ALL-UNNAMED Test8252121
 */
public class Test8252121 {
    @Test
    public void test() {
        try (var arena = Arena.ofConfined()) {
            int[] array = { 3, 5, 89, 34, -33 };
            MemorySegment seg = arena.allocateFrom(C_INT, array);
            assertEquals(IntStream.of(array).sum(), sum(seg));
            assertEquals(IntStream.of(array).reduce(1, (a,b) -> a*b), mul(seg));
        }
    }
}
