/*
 * Copyright (c) 2021, 2024, Oracle and/or its affiliates. All rights reserved.
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
import test.jextract.testLinkageErrors.*;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;
import static test.jextract.testLinkageErrors.testLinkageErrors_h.*;

/*
 * @test id=classes
 * @bug 8259473
 * @summary jextract generated code should throw exception for unfound native symbols from calls, variable access, set immediately
 * @library /lib
 * @run main/othervm JtregJextract -t test.jextract.testLinkageErrors testLinkageErrors.h
 * @build TestLinkageErrors
 * @run testng/othervm --enable-native-access=ALL-UNNAMED TestLinkageErrors
 */
/*
 * @test id=sources
 * @bug 8259473
 * @summary jextract generated code should throw exception for unfound native symbols from calls, variable access, set immediately
 * @library /lib
 * @run main/othervm JtregJextractSources -t test.jextract.testLinkageErrors testLinkageErrors.h
 * @build TestLinkageErrors
 * @run testng/othervm --enable-native-access=ALL-UNNAMED TestLinkageErrors
 */
public class TestLinkageErrors {

    static void assertThrowsULE(Runnable action, String symbol) {
        try {
            action.run();
            throw new AssertionError("should not reach here");
        } catch (Throwable t) {
            while (t.getCause() != null) {
                t = t.getCause();
            }
            assertTrue(t.getMessage().contains("unresolved symbol: " + symbol));
        }
    }

    @Test
    public void nullChecksTest() {
        assertThrowsULE(() -> func(), "func");
        assertThrowsULE(() -> func$MH(), "func");
        assertThrowsULE(() -> x(), "x");
        assertThrowsULE(() -> x(1), "x");
        assertThrowsULE(() -> x$SEGMENT(), "x");
        assertThrowsULE(() -> y$SEGMENT(), "y");
        assertThrowsULE(() -> pt$SEGMENT(), "pt");
    }
}
