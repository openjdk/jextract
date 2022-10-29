/*
 * Copyright (c) 2022, Oracle and/or its affiliates. All rights reserved.
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

import testlib.TestUtils;
import org.testng.annotations.Test;
import testlib.JextractToolRunner;

import static org.testng.Assert.assertNotNull;

public class Test7903339 extends JextractToolRunner {
    @Test
    public void testPrimitiveTypedefs() {
        Path test7903339Output = getOutputFilePath("test7903339gen");
        Path test7903339H = getInputFilePath("test7903339.h");
        run("--output", test7903339Output.toString(), test7903339H.toString()).checkSuccess();
        try(TestUtils.Loader loader = TestUtils.classLoader(test7903339Output)) {
            Class<?> headerCls = loader.loadClass("test7903339_h");
            assertNotNull(findField(headerCls, "S_SHORT"));
            assertNotNull(findField(headerCls, "U_SHORT"));
            assertNotNull(findField(headerCls, "S_INT"));
            assertNotNull(findField(headerCls, "U_INT"));
            assertNotNull(findField(headerCls, "S_LONG"));
            assertNotNull(findField(headerCls, "U_LONG"));
            assertNotNull(findField(headerCls, "S_LONG_LONG"));
            assertNotNull(findField(headerCls, "U_LONG_LONG"));
            assertNotNull(findField(headerCls, "SIGNED"));
            assertNotNull(findField(headerCls, "UNSIGNED"));
        } finally {
            TestUtils.deleteDir(test7903339Output);
        }
    }
}
