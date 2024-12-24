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
import java.lang.foreign.MemorySegment;
import org.testng.annotations.Test;
import test.jextract.test8246400.*;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;
import static test.jextract.test8246400.test8246400_h.*;

/*
 * @test
 * @bug 8246400
 * @summary jextract should generate a utility to manage mutliple MemorySegments
 * @library /lib
 * @build testlib.TestUtils
 * @run main/othervm JtregJextract -l Test8246400 --use-system-load-library -t test.jextract.test8246400 test8246400.h
 * @build LibTest8246400Test
 * @run testng/othervm --enable-native-access=ALL-UNNAMED  LibTest8246400Test
 */
public class LibTest8246400Test {
    @Test
    public void testSegmentRegister() {
        MemorySegment sum = null;
        try (Arena arena = Arena.ofConfined()) {
            var v1 = Vector.allocate(arena);
            Vector.x(v1, 1.0);
            Vector.y(v1, 0.0);

            var v2 = Vector.allocate(arena);
            Vector.x(v2, 0.0);
            Vector.y(v2, 1.0);

            sum = add(arena, v1, v2);

            assertEquals(Vector.x(sum), 1.0, 0.1);
            assertEquals(Vector.y(sum), 1.0, 0.1);

            MemorySegment callback = cosine_similarity$dot.allocate((a, b) -> {
                return (Vector.x(a) * Vector.x(b)) +
                    (Vector.y(a) * Vector.y(b));
            }, arena);

            var value = cosine_similarity(v1, v2, callback);
            assertEquals(value, 0.0, 0.1);

            value = cosine_similarity(v1, v1, callback);
            assertEquals(value, 1.0, 0.1);
        }
        assertTrue(!sum.scope().isAlive());
    }
}
