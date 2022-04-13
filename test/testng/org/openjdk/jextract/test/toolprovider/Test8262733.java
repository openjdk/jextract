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

import testlib.TestUtils;
import org.testng.annotations.Test;
import testlib.JextractToolRunner;

import java.nio.file.Path;
import static org.testng.Assert.assertNotNull;

public class Test8262733 extends JextractToolRunner {
    @Test
    public void test() {
        Path output = getOutputFilePath("8262733gen");
        Path outputH = getInputFilePath("test8262733.h");
        run("-d", output.toString(), outputH.toString()).checkSuccess();
        try(TestUtils.Loader loader = TestUtils.classLoader(output)) {
            Class<?> cls = loader.loadClass("test8262733_h");
            assertNotNull(cls);
        } finally {
            TestUtils.deleteDir(output);
        }
    }
}
