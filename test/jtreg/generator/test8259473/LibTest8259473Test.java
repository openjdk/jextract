/*
 * Copyright (c) 2021, Oracle and/or its affiliates. All rights reserved.
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
import java.lang.foreign.MemorySegment;
import org.testng.annotations.Test;
import test.jextract.test8259473.*;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;
import static test.jextract.test8259473.test8259473_h.*;

/*
 * @test id=classes
 * @bug 8259473
 * @summary jextract generated code should throw exception for unfound native symbols from calls, variable access, set immediately
 * @library /lib
 * @run main/othervm JtregJextract -t test.jextract.test8259473 test8259473.h
 * @run testng/othervm --enable-native-access=ALL-UNNAMED LibTest8259473Test
 */
/*
 * @test id=sources
 * @bug 8259473
 * @summary jextract generated code should throw exception for unfound native symbols from calls, variable access, set immediately
 * @library /lib
 * @run main/othervm JtregJextractSources -t test.jextract.test8259473 test8259473.h
 * @run testng/othervm --enable-native-access=ALL-UNNAMED LibTest8259473Test
 */
public class LibTest8259473Test {
    @Test
    public void nullChecksTest() {
        try {
            func();
            throw new AssertionError("should not reach here");
        } catch (UnsatisfiedLinkError ule) {
            assertTrue(ule.getMessage().contains("unresolved symbol: func"));
        }

        try {
            func$MH();
            throw new AssertionError("should not reach here");
        } catch (UnsatisfiedLinkError ule) {
            assertTrue(ule.getMessage().contains("unresolved symbol: func"));
        }

        try {
            x$get();
            throw new AssertionError("should not reach here");
        } catch (UnsatisfiedLinkError ule) {
            assertTrue(ule.getMessage().contains("unresolved symbol: x"));
        }

        try {
            x$set(1);
            throw new AssertionError("should not reach here");
        } catch (UnsatisfiedLinkError ule) {
            assertTrue(ule.getMessage().contains("unresolved symbol: x"));
        }

        try {
            x$SEGMENT();
            throw new AssertionError("should not reach here");
        } catch (UnsatisfiedLinkError ule) {
            assertTrue(ule.getMessage().contains("unresolved symbol: x"));
        }

        try {
            y$SEGMENT();
            throw new AssertionError("should not reach here");
        } catch (UnsatisfiedLinkError ule) {
            assertTrue(ule.getMessage().contains("unresolved symbol: y"));
        }

        try {
            pt$SEGMENT();
            throw new AssertionError("should not reach here");
        } catch (UnsatisfiedLinkError ule) {
            assertTrue(ule.getMessage().contains("unresolved symbol: pt"));
        }
    }
}
