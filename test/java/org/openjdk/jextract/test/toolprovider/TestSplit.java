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
package org.openjdk.jextract.test.toolprovider;

import java.nio.file.Path;

import org.openjdk.jextract.test.TestUtils;
import org.testng.annotations.Ignore;
import org.testng.annotations.Test;
import static org.testng.Assert.*;

// This test is ignored because we cannot set the "-Djextract.decls.per.header" here (as all testng tests are
// executed in the same VM). Moving the test in a non-testng folder is possible, but this test depends on
// JextractToolRunner and TestUtils which makes it hard.
@Ignore
public class TestSplit extends JextractToolRunner {
    @Test
    public void testSplit() {
        Path splitOutput = getOutputFilePath("split");
        Path splitH = getInputFilePath("split.h");
        run("--output", splitOutput.toString(), splitH.toString()).checkSuccess();
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
