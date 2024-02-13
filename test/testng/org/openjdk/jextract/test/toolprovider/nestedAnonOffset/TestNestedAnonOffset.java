/*
 * Copyright (c) 2023, 2024, Oracle and/or its affiliates. All rights reserved.
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
import java.lang.foreign.UnionLayout;
import java.nio.file.Path;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;

public class TestNestedAnonOffset extends JextractToolRunner {

    Loader loader;

    @BeforeClass
    public void beforeClass() {
        Path output = getOutputFilePath("TestAnon-anonymousStructs.h");
        Path outputH = getInputFilePath("anonymousStructs.h");
        runAndCompile(output, outputH.toString());

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
        assertEquals(layout.memberLayouts().get(0), C_CHAR.withName("c"));
        assertEquals(layout.memberLayouts().get(1), MemoryLayout.paddingLayout(3));

        StructLayout nestedAnon1 = (StructLayout) layout.memberLayouts().get(2);
        assertEquals(nestedAnon1.memberLayouts().get(0), MemoryLayout.paddingLayout(4));

        StructLayout nestedAnon2 = (StructLayout) nestedAnon1.memberLayouts().get(1);
        assertEquals(nestedAnon2.memberLayouts().get(0), MemoryLayout.paddingLayout(4));
        assertEquals(nestedAnon2.memberLayouts().get(1), C_INT.withName("x"));
    }

    @Test
    public void testBar() {
        Class<?> bar = loader.loadClass("Bar");
        assertNotNull(bar);
        StructLayout layout = (StructLayout) findLayout(bar);
        assertEquals(layout.memberLayouts().get(0), C_CHAR.withName("c"));
        assertEquals(layout.memberLayouts().get(1), MemoryLayout.paddingLayout(3));

        StructLayout nestedAnon1 = (StructLayout) layout.memberLayouts().get(2);
        assertEquals(nestedAnon1.memberLayouts().get(0), MemoryLayout.paddingLayout(4));

        UnionLayout nestedAnon2 = (UnionLayout) nestedAnon1.memberLayouts().get(1);
        assertEquals(nestedAnon2.memberLayouts().get(0), C_INT.withName("x"));
    }

    @Test
    public void testBaz() {
        Class<?> baz = loader.loadClass("Baz");
        assertNotNull(baz);
        StructLayout layout = (StructLayout) findLayout(baz);
        assertEquals(layout.memberLayouts().get(0), C_CHAR.withName("c"));
        // Note here: only on Windows, the bitfield needs to be aligned and requires more padding
        assertEquals(layout.memberLayouts().get(1), MemoryLayout.paddingLayout(IS_WINDOWS ? 11 : 8));
    }

    @Test
    public void testBoo() {
        Class<?> boo = loader.loadClass("Boo");
        assertNotNull(boo);
        StructLayout layout = (StructLayout) findLayout(boo);
        assertEquals(layout.memberLayouts().get(0), C_CHAR.withName("c"));
        assertEquals(layout.memberLayouts().get(1), MemoryLayout.paddingLayout(3));

        StructLayout nestedAnon1 = (StructLayout) layout.memberLayouts().get(2);
        assertEquals(nestedAnon1.memberLayouts().get(0), MemoryLayout.paddingLayout(8));

        StructLayout nestedAnon2 = (StructLayout) nestedAnon1.memberLayouts().get(1);
        assertEquals(nestedAnon2.memberLayouts().get(0), C_INT.withName("x"));
    }
}
