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

import java.lang.foreign.MemorySegment;
import java.lang.foreign.MemorySession;
import org.testng.annotations.Test;
import test.jextract.test8246400.*;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;
import static test.jextract.test8246400.test8246400_h.*;

/*
 * @test id=classes
 * @bug 8246400
 * @summary jextract should generate a utility to manage mutliple MemorySegments
 * @library /lib
 * @run main/othervm JtregJextract -l Test8246400 -t test.jextract.test8246400 test8246400.h
 * @run testng/othervm --enable-native-access=ALL-UNNAMED  LibTest8246400Test
 */
/*
 * @test id=sources
 * @bug 8246400
 * @summary jextract should generate a utility to manage mutliple MemorySegments
 * @library /lib
 * @run main/othervm JtregJextractSources -l Test8246400 -t test.jextract.test8246400 test8246400.h
 * @run testng/othervm --enable-native-access=ALL-UNNAMED LibTest8246400Test
 */
public class LibTest8246400Test {
    @Test
    public void testSegmentRegister() {
        MemorySegment sum = null;
        try (MemorySession session = MemorySession.openConfined()) {
            var v1 = Vector.allocate(session);
            Vector.x$set(v1, 1.0);
            Vector.y$set(v1, 0.0);

            var v2 = Vector.allocate(session);
            Vector.x$set(v2, 0.0);
            Vector.y$set(v2, 1.0);

            sum = add(session, v1, v2);

            assertEquals(Vector.x$get(sum), 1.0, 0.1);
            assertEquals(Vector.y$get(sum), 1.0, 0.1);

            MemorySegment callback = cosine_similarity$dot.allocate((a, b) -> {
                return (Vector.x$get(a) * Vector.x$get(b)) +
                    (Vector.y$get(a) * Vector.y$get(b));
            }, session);

            var value = cosine_similarity(v1, v2, callback);
            assertEquals(value, 0.0, 0.1);

            value = cosine_similarity(v1, v1, callback);
            assertEquals(value, 1.0, 0.1);
        }
        assertTrue(!sum.session().isAlive());
    }
}
