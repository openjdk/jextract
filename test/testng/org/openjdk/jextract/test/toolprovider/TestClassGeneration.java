/*
 * Copyright (c) 2020, 2025, Oracle and/or its affiliates. All rights reserved.
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

import java.lang.foreign.Arena;
import java.lang.foreign.MemoryLayout.PathElement;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.MemoryLayout;

import testlib.TestUtils;
import org.testng.annotations.*;
import testlib.JextractToolRunner;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodType;
import java.lang.invoke.VarHandle;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;

import static java.lang.invoke.MethodType.methodType;
import static java.lang.foreign.MemoryLayout.PathElement.sequenceElement;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.fail;

public class TestClassGeneration extends JextractToolRunner {

    private static final VarHandle VH_bytes = C_CHAR.varHandle();

    private Path outputDir;
    private TestUtils.Loader loader;
    private Class<?> cls;

    @DataProvider
    public static Object[][] simpleConstants() {
        return new Object[][]{
            { "macro_byte",         byte.class,   (byte) 1                         },
            { "macro_short",        short.class, (short) 1                         },
            { "macro_int",          int.class,           1                         },
            { "macro_long",         long.class,          1L                        },
            { "macro_float",        float.class,         1.0F                      },
            { "macro_double",       double.class,        1.0D                      },
            { "macro_address_NULL", MemorySegment.class, MemorySegment.NULL        },
            { "macro_address_123",  MemorySegment.class, MemorySegment.ofAddress(123) },
            { "enum_0",             int.class,           0                         },
            { "enum_1",             int.class,           1                         },
            { "enum_2",             int.class,           2                         },
            { "enum_anon_0",        int.class,           0                         },
            { "enum_anon_1",        int.class,           1                         },
            { "enum_anon_2",        int.class,           2                         },
        };
    }

    @DataProvider
    public static Object[][] stringConstants() {
        return new Object[][]{
            { "macro_string",         "abc"      },
            { "macro_string_noident", "123.asdf" },
        };
    }

    private static final Object[] NO_ARGS = {};

    @DataProvider
    public static Object[][] method() {
        return new Object[][]{
            { "func_byte",   methodType(byte.class),   (byte) 1,  NO_ARGS },
            { "func_short",  methodType(short.class), (short) 2,  NO_ARGS },
            { "func_int",    methodType(int.class),           3,  NO_ARGS },
            { "func_long",   methodType(long.class),          4L, NO_ARGS },
            { "func_float",  methodType(float.class),         5F, NO_ARGS },
            { "func_double", methodType(double.class),        6D, NO_ARGS },
        };
    }

    @DataProvider
    public static Object[][] globals() {
        return new Object[][]{
            { "global_byte",   byte.class,   C_CHAR,   (byte) 1  },
            { "global_short",  short.class,  C_SHORT, (short) 2  },
            { "global_int",    int.class,    C_INT,           3  },
            { "global_long",   long.class,   C_LONG_LONG,      4L },
            { "global_float",  float.class,  C_FLOAT,         5F },
            { "global_double", double.class, C_DOUBLE,        6D },
        };
    }

    @DataProvider
    public static Object[][] structMembers() {
        return new Object[][] {
            { "Foo", C_CHAR.withName("c"),      byte.class,   (byte) 10  },
            { "Foo", C_SHORT.withName("s"),     short.class, (short) 10  },
            { "Foo", C_INT.withName("i"),       int.class,           10  },
            { "Foo", C_LONG_LONG.withName("ll"), long.class,          10L },
            { "Foo", C_FLOAT.withName("f"),     float.class,         10F },
            { "Foo", C_DOUBLE.withName("d"),    double.class,        10D },
            { "Bar", C_INT.withName("a"),       int.class,           10 },
            { "Bar", C_INT.withName("b"),       int.class,           10 },
        };
    }

    @DataProvider
    public static Object[][] functionalInterfaces() {
        return new Object[][]{
            { "CB", methodType(void.class, int.class) }
        };
    }

    @Test(dataProvider = "simpleConstants")
    public void testConstant(String name, Class<?> expectedType, Object expectedValue) throws Throwable {
        Method getter = checkMethod(cls, name, expectedType);
        if (expectedType == MemorySegment.class) {
            assertEquals(((MemorySegment)getter.invoke(null)).address(), ((MemorySegment)expectedValue).address());
        } else {
            assertEquals(getter.invoke(null), expectedValue);
        }
    }

    @Test(dataProvider = "stringConstants")
    public void testStringConstant(String name, String expectedValue) throws Throwable {
        Method getter = checkMethod(cls, name, MemorySegment.class);
        MemorySegment actual = (MemorySegment) getter.invoke(null);
        byte[] expected = expectedValue.getBytes(StandardCharsets.UTF_8);
        assertEquals(actual.byteSize(), expected.length + 1);
        for (int i = 0; i < expected.length; i++) {
            assertEquals((byte) VH_bytes.get(actual, (long) i), expected[i]);
        }
    }

    @Test(dataProvider = "method")
    public void testMethod(String name, MethodType expectedType, Object expectedReturn, Object[] args) throws Throwable {
        Method func = checkMethod(cls, name, expectedType);
        assertEquals(func.invoke(null, args), expectedReturn);
    }

    @Test(dataProvider = "globals")
    public void testGlobal(String name, Class<?> expectedType, MemoryLayout expectedLayout, Object expectedValue) throws Throwable {
        Method getter = checkMethod(cls, name, expectedType);
        assertEquals(getter.invoke(null), expectedValue);

        checkMethod(cls, name, void.class, expectedType);
    }

    @Test(dataProvider = "structMembers")
    public void testStructMember(String structName, MemoryLayout memberLayout, Class<?> expectedType, Object testValue) throws Throwable {
        String memberName = memberLayout.name().orElseThrow();

        Class<?> structCls = loader.loadClass("com.acme." + structName);
        checkDefaultConstructor(cls);
        Method layout_getter = checkMethod(structCls, "layout", MemoryLayout.class);
        MemoryLayout structLayout = (MemoryLayout) layout_getter.invoke(null);
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment struct = arena.allocate(structLayout);
            Method offsetMethod = findMethod(structCls, memberName + "$offset");
            assertNotNull(offsetMethod);
            assertEquals(offsetMethod.getReturnType(), long.class);
            assertEquals(offsetMethod.invoke(null), structLayout.byteOffset(PathElement.groupElement(memberName)));

            Method getter = checkMethod(structCls, memberName, expectedType, MemorySegment.class);
            Method setter = checkMethod(structCls, memberName, void.class, MemorySegment.class, expectedType);
            MemorySegment addr = struct;
            setter.invoke(null, addr, testValue);
            assertEquals(getter.invoke(null, addr), testValue);
        }
    }

    @Test(dataProvider = "functionalInterfaces")
    public void testFunctionalInterface(String name, MethodType type) {
        Class<?> fpClass = loader.loadClass("com.acme." + name);
        checkPrivateConstructor(fpClass);
        Class<?> fiClass = findNestedClass(fpClass, "Function");
        assertNotNull(fiClass);
        checkMethod(fiClass, "apply", type);
        Class<?> cbClass = loader.loadClass("com.acme." + name);
        assertNotNull(cbClass);
        checkMethod(cbClass, "allocate", MemorySegment.class, fiClass, Arena.class);
    }

    @BeforeClass
    public void setup() {
        outputDir = getOutputFilePath("exmples_out");
        Path inputHeader = getInputFilePath("examples.h");
        runAndCompile(outputDir,
            "-t", "com.acme",
            "-l", "Examples", "--use-system-load-library",
            inputHeader
        );
        loader = TestUtils.classLoader(outputDir);
        cls = loader.loadClass("com.acme.examples_h");
    }

    @AfterClass
    public void tearDown() {
        loader.close();
        TestUtils.deleteDir(outputDir);
    }

    private void checkDefaultConstructor(Class<?> cls) {
        try {
            Constructor<?> c = cls.getDeclaredConstructor();
            assertEquals(c.getModifiers(), 0, "Unexpected constructor modifiers");
        } catch (ReflectiveOperationException ex) {
            fail("Default constructor not found!");
        }
    }

    private void checkPrivateConstructor(Class<?> cls) {
        try {
            Constructor<?> c = cls.getDeclaredConstructor();
            assertEquals(c.getModifiers(), Modifier.PRIVATE, "Unexpected constructor modifiers");
        } catch (ReflectiveOperationException ex) {
            fail("Private constructor not found!");
        }
    }
}
