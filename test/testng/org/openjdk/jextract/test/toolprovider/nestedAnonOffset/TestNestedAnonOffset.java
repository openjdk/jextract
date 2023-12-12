/*
 * Copyright (c) 2023, Oracle and/or its affiliates. All rights reserved.
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
package org.openjdk.jextract.test.toolprovider.nestedAnonOffset;

import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import testlib.JextractToolRunner;

import java.lang.foreign.MemoryLayout;
import java.lang.foreign.StructLayout;
import java.nio.file.Path;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;

public class TestNestedAnonOffset extends JextractToolRunner {

    Loader loader;

    @BeforeClass
    public void beforeClass() {
        Path output = getOutputFilePath("TestAnon-anonymousStructs.h");
        Path outputH = getInputFilePath("anonymousStructs.h");
        run("--output",
            output.toString(), outputH.toString()).checkSuccess();

        loader = classLoader(output);
    }

    @AfterClass
    public void afterClass() {
        loader.close();
    }

    @Test
    public void testFoo() {
        Class<?> foo = loader.loadClass("Foo");
        assertNotNull(foo);
        StructLayout layout = (StructLayout) findLayout(foo);
        assertEquals(C_CHAR.withName("c"), layout.memberLayouts().get(0));
        assertEquals(MemoryLayout.paddingLayout(3), layout.memberLayouts().get(1));

        StructLayout nestedAnon1 = (StructLayout) layout.memberLayouts().get(2);
        assertEquals(MemoryLayout.paddingLayout(4), nestedAnon1.memberLayouts().get(0));

        StructLayout nestedAnon2 = (StructLayout) nestedAnon1.memberLayouts().get(1);
        assertEquals(MemoryLayout.paddingLayout(4), nestedAnon2.memberLayouts().get(0));
        assertEquals(C_INT.withName("x"), nestedAnon2.memberLayouts().get(1));
    }

    @Test
    public void testBar() {
        Class<?> bar = loader.loadClass("Bar");
        assertNotNull(bar);
        StructLayout layout = (StructLayout) findLayout(bar);
        assertEquals(C_CHAR.withName("c"), layout.memberLayouts().get(0));
        assertEquals(MemoryLayout.paddingLayout(3), layout.memberLayouts().get(1));

        StructLayout nestedAnon1 = (StructLayout) layout.memberLayouts().get(2);
        assertEquals(MemoryLayout.paddingLayout(4), nestedAnon1.memberLayouts().get(0));

        StructLayout nestedAnon2 = (StructLayout) nestedAnon1.memberLayouts().get(1);
        assertEquals(MemoryLayout.paddingLayout(4), nestedAnon2.memberLayouts().get(0));
        assertEquals(C_INT.withName("x"), nestedAnon2.memberLayouts().get(1));
    }
}
