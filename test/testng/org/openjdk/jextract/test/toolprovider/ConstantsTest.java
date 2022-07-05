/*
 * Copyright (c) 2020, Oracle and/or its affiliates. All rights reserved.
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

import java.lang.foreign.MemorySegment;
import testlib.TestUtils;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import java.nio.file.Path;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Stream;
import java.lang.foreign.GroupLayout;
import java.lang.foreign.MemorySegment;
import testlib.JextractToolRunner;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertSame;
import static org.testng.Assert.assertTrue;

public class ConstantsTest extends JextractToolRunner {
    private Class<?> constants;
    private Path dirPath;
    private TestUtils.Loader loader;

    @BeforeTest
    public void setup() {
        dirPath = getOutputFilePath("ConstantsTest_output");
        run( "--output", dirPath.toString(), getInputFilePath("constants.h").toString()).checkSuccess();
        loader = TestUtils.classLoader(dirPath);
        constants = loader.loadClass("constants_h");
    }

    @AfterTest
    public void cleanup() {
        constants = null;
        loader.close();
        TestUtils.deleteDir(dirPath);
    }

    @Test(dataProvider = "definedConstants")
    public void checkConstantsTypesAndValues(String name, Class<?> type, Consumer<Object> checker) throws ReflectiveOperationException {
        var f = findMethod(constants, name);
        assertNotNull(f);
        assertSame(f.getReturnType(), type);
        f.setAccessible(true);
        Object actual = f.invoke(null);
        checker.accept(actual);
    }

    @Test(dataProvider = "missingConstants")
    public void checkMissingConstants(String name) {
        assertTrue(findMethod(constants, name) == null);
    }

    @DataProvider
    public static Object[][] definedConstants() {
        return new Object[][] {
                { "SUP", int.class, equalsTo(5) },
                { "ZERO", int.class, equalsTo(0) },
                { "ONE", int.class, equalsTo(1) },
                { "TWO", int.class, equalsTo(2) },
                { "THREE", int.class, equalsTo(3) },
                { "FOUR", long.class, equalsTo(4L) },
                { "FIVE", long.class, equalsTo(5L) },
                { "SIX", int.class, equalsTo(6) },
                { "FLOAT_VALUE", float.class, equalsTo(1.32f) },
                { "DOUBLE_VALUE", double.class, (Consumer<Double>) (actual -> assertEquals(actual, 1.32, 0.1)) },
                { "CHAR_VALUE", int.class, equalsTo(104) }, //integer char constants have type int
                { "MULTICHAR_VALUE", int.class, equalsTo(26728) },  //integer char constants have type int
                { "BOOL_VALUE", boolean.class, equalsTo(true) },
                { "SUB", int.class, equalsTo( 7 ) },
                // pointer type values
                { "STR", MemorySegment.class, equalsToJavaStr("Hello") },
                { "QUOTE", MemorySegment.class, equalsToJavaStr("QUOTE") },
                { "ZERO_PTR", MemorySegment.class, equalsPtrContents(0) },
                { "F_PTR", MemorySegment.class, equalsPtrContents(0xFFFFFFFFFFFFFFFFL) },
        };
    }

    static Consumer<Object> equalsTo(Object expected) {
        return actual -> assertEquals(actual, expected);
    }

    static Consumer<MemorySegment> equalsToJavaStr(String expected) {
        return actual -> assertEquals(actual.getUtf8String(0), expected);
    }

    static Consumer<MemorySegment> equalsPtrContents(long expected) {
        return actual -> assertEquals(actual.toRawLongValue(), expected);
    }

    @DataProvider
    public static Object[][] missingConstants() {
        return new Object[][] {
                { "ID" },
                { "SUM" },
                { "BLOCK_BEGIN" },
                { "BLOCK_END" },
                { "INTEGER_MAX_VALUE" },
                { "CYCLIC_1" },
                { "CYCLIC_2" },
                // array
                { "ARRAY" }
        };
    }
}
