/*
 * Copyright (c) 2026, Oracle and/or its affiliates. All rights reserved.
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

import org.testng.annotations.Test;
import testlib.JextractToolRunner;
import testlib.TestUtils;

import java.io.IOException;
import java.lang.foreign.FunctionDescriptor;
import java.lang.reflect.Field;
import java.nio.file.Path;

import static org.testng.Assert.*;

public class VlaFunctionParameterTest extends JextractToolRunner {

    @Test
    public void testVlaFunctionParameter() throws IOException {
        Path output = getOutputFilePath("vlaFunctionParameter_out");
        Path input = getInputFilePath("vlaFunctionParameter.h");
        runAndCompile(output,
            "-t", "org.jextract",
            "-l", "VlaFunctionParameter", "--use-system-load-library",
            input);
        try (TestUtils.Loader loader = TestUtils.classLoader(output)) {
            Class<?> cls = loader.loadClass("org.jextract.vlaFunctionParameter_h");
            assertNotNull(cls);
            Class<?> fooCls = findClass(cls.getDeclaredClasses(), "foo");
            assertNotNull(fooCls);
            Field descField = findField(fooCls, "DESC");
            assertNotNull(descField);

            FunctionDescriptor actualDescriptor;
            try {
                actualDescriptor = (FunctionDescriptor) descField.get(null);
            } catch (IllegalAccessException e) {
                assertTrue(false, "should not reach here");
                return;
            }

            FunctionDescriptor expectedDescriptor = FunctionDescriptor.of(
                C_INT,
                C_INT,
                C_POINTER
            );

            assertEquals(actualDescriptor, expectedDescriptor);
        } finally {
            TestUtils.deleteDir(output);
        }
    }
}
