/*
 * Copyright (c) 2024, Oracle and/or its affiliates. All rights reserved.
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
package org.openjdk.jextract.test.toolprovider.fiMangle.nestedDecls;

import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import testlib.JextractToolRunner;

import java.lang.reflect.Modifier;
import java.nio.file.Path;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;

public class TestFunctionalInterfaceMangle extends JextractToolRunner {

    Loader loader;

    @BeforeClass
    public void beforeClass() {
        Path output = getOutputFilePath("TestFunctionalInterfaceMangler-fiMangle.h");
        Path outputH = getInputFilePath("fi_mangle.h");
        runAndCompile(output, outputH.toString());

        loader = classLoader(output);
    }

    @AfterClass
    public void afterClass() {
        loader.close();
    }

    @Test
    public void testFunctionPointer() {
        Class<?> fpClass = loader.loadClass("Function");
        assertNotNull(fpClass);
        Class<?> fiClass = findNestedClass(fpClass, "Function$");
    }
}
