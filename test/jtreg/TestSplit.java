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
import java.nio.file.Path;

import testlib.TestUtils;
import org.testng.annotations.Test;
import testlib.JextractToolRunner;

import static org.testng.Assert.*;

/*
 * @test
 * @library /lib
 * @run testng/othervm -Djextract.decls.per.header=1 TestSplit
 */
public class TestSplit extends JextractToolRunner {
    @Test
    public void testSplit() {
        Path splitOutput = getOutputFilePath("split");
        Path splitH = getInputFilePath("split.h");
        runAndCompile(splitOutput, splitH.toString());
        try(TestUtils.Loader loader = TestUtils.classLoader(splitOutput)) {
            checkPresent(loader, "split_h");
            checkPresent(loader, "split_h_1");
            checkPresent(loader, "split_h_2");
            checkPresent(loader, "split_h_3");
            checkPresent(loader, "split_h_4");
            checkMissing(loader, "split_h_5");
        } finally {
            TestUtils.deleteDir(splitOutput);
        }
    }

    private static void checkPresent(TestUtils.Loader loader, String name) {
        assertNotNull(loader.loadClass(name));
    }

    private static void checkMissing(TestUtils.Loader loader, String name) {
        assertNull(loader.loadClass(name));
    }
}
