/*
 * Copyright (c) 2020, 2022, Oracle and/or its affiliates. All rights reserved.
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

package testlib;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.invoke.MethodType;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Objects;
import java.util.spi.ToolProvider;

import java.lang.foreign.MemoryLayout;
import java.lang.foreign.MemoryLayout.PathElement;
import java.lang.foreign.ValueLayout;
import org.openjdk.jextract.JextractTool;
import org.openjdk.jextract.Type;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.fail;

public class JextractToolRunner {

    private static final boolean IS_WINDOWS = System.getProperty("os.name").startsWith("Windows");

    public static final ValueLayout.OfBoolean C_BOOL = ValueLayout.JAVA_BOOLEAN;
    public static final ValueLayout.OfByte C_CHAR = ValueLayout.JAVA_BYTE;
    public static final ValueLayout.OfShort C_SHORT = ValueLayout.JAVA_SHORT.withBitAlignment(16);
    public static final ValueLayout.OfInt C_INT = ValueLayout.JAVA_INT.withBitAlignment(32);
    public static final ValueLayout C_LONG = IS_WINDOWS ? ValueLayout.JAVA_INT.withBitAlignment(32) : ValueLayout.JAVA_LONG.withBitAlignment(64);
    public static final ValueLayout.OfLong C_LONG_LONG = ValueLayout.JAVA_LONG.withBitAlignment(64);
    public static final ValueLayout.OfFloat C_FLOAT = ValueLayout.JAVA_FLOAT.withBitAlignment(32);
    public static final ValueLayout.OfDouble C_DOUBLE = ValueLayout.JAVA_DOUBLE.withBitAlignment(64);
    public static final ValueLayout.OfAddress C_POINTER = ValueLayout.ADDRESS.withBitAlignment(ValueLayout.ADDRESS.bitSize()).asUnbounded();

    // (private) exit codes from jextract tool. Copied from JextractTool.
    protected static final int SUCCESS       = 0;
    protected static final int OPTION_ERROR  = 1;
    protected static final int INPUT_ERROR   = 2;
    protected static final int CLANG_ERROR   = 3;
    protected static final int RUNTIME_ERROR = 4;
    protected static final int OUTPUT_ERROR  = 5;

    private static String safeFileName(String filename) {
        int ext = filename.lastIndexOf('.');
        return ext != -1 ? filename.substring(0, ext) : filename;
    }

    private static final ToolProvider JEXTRACT_TOOL = new JextractTool.JextractToolProvider();

    private final Path inputDir;
    private final Path outputDir;

    protected JextractToolRunner() {
        this(null, null);
    }

    protected JextractToolRunner(Path input, Path output) {
         inputDir = (input != null) ? input :
                Paths.get(System.getProperty("test.file")).getParent();
         outputDir = (output != null) ? output :
                Paths.get(System.getProperty("test.classes"), "test-output");
    }

    protected Path getInputFilePath(String fileName) {
        return inputDir.resolve(fileName).toAbsolutePath();
    }

    protected Path getOutputFilePath(String fileName) {
        return outputDir.resolve(fileName).toAbsolutePath();
    }

    protected static class JextractResult {
        private int exitCode;
        private String output;

        JextractResult(int exitCode, String output) {
            this.exitCode = exitCode;
            this.output = output;
        }

        public JextractResult checkSuccess() {
            assertEquals(exitCode, SUCCESS, "Sucess expected, failed: " + exitCode);
            return this;
        }

        public JextractResult checkFailure() {
            assertNotEquals(exitCode, SUCCESS, "Failure expected, succeeded!");
            return this;
        }

        public JextractResult checkFailure(int expectedExitCode) {
            assertEquals(exitCode, expectedExitCode, "Expected error code " + expectedExitCode);
            return this;
        }

        public JextractResult checkContainsOutput(String expected) {
            Objects.requireNonNull(expected);
            assertTrue(output.contains(expected), "Output does not contain string: " + expected);
            return this;
        }

        public JextractResult checkMatchesOutput(String regex) {
            Objects.requireNonNull(regex);
            assertTrue(output.trim().matches(regex), "Output does not match regex: " + regex);
            return this;
        }
    }

    protected static JextractResult run(Object... options) {
        return run(Arrays.stream(options).map(Objects::toString).toArray(String[]::new));
    }

    protected static JextractResult run(String... options) {
        StringWriter writer = new StringWriter();
        PrintWriter pw = new PrintWriter(writer);
        String[] args = new String[options.length + 1];
        int result = JEXTRACT_TOOL.run(pw, pw, options);
        String output = writer.toString();
        System.err.println(output);
        return new JextractResult(result, output);
    }

    protected static Loader classLoader(Path... paths) {
        try {
            URL[] urls = new URL[paths.length];
            for (int i = 0; i < paths.length; i++) {
                urls[i] = paths[i].toUri().toURL();
            }
            URLClassLoader ucl = new URLClassLoader(urls,
                    JextractToolRunner.class.getClassLoader());
            return new Loader(ucl);
        } catch (RuntimeException re) {
            throw re;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    protected static Field findField(Class<?> cls, String name) {
        try {
            return cls.getField(name);
        } catch (Exception e) {
            System.err.println(e);
            return null;
        }
    }

    protected Method checkIntGetter(Class<?> cls, String name, int value) {
        Method method = findMethod(cls, name);
        assertNotNull(method);
        assertEquals(method.getReturnType(), int.class);
        try {
            assertEquals((int)method.invoke(null), value);
        } catch (Exception exp) {
            System.err.println(exp);
            assertTrue(false, "should not reach here");
        }
        return method;
    }

    protected static Method findMethod(Class<?> cls, String name, Class<?>... argTypes) {
        try {
            return cls.getMethod(name, argTypes);
        } catch (Exception e) {
            System.err.println(e);
            return null;
        }
    }

    protected static Method findFirstMethod(Class<?> cls, String name) {
        try {
            for (Method m : cls.getMethods()) {
                if (name.equals(m.getName())) {
                    return m;
                }
            }
            return null;
        } catch (Exception e) {
            System.err.println(e);
            return null;
        }
    }

    protected static Class<?> findNestedClass(Class<?> clz, String name) {
        return findClass(clz.getClasses(), name);
    }

    protected static Class<?> findClass(Class<?>[] clz, String name) {
        for (Class<?> cls: clz) {
            if (cls.getSimpleName().equals(name)) {
                return cls;
            }
        }
        return null;
    }

    protected Method checkMethod(Class<?> cls, String name, MethodType type) {
        return checkMethod(cls, name, type.returnType(), type.parameterArray());
    }

    protected Method checkMethod(Class<?> cls, String name, Class<?> returnType, Class<?>... args) {
        Method m = findMethod(cls, name, args);
        assertNotNull(m);
        assertEquals(m.getReturnType(), returnType);
        assertEquals(m.getParameterTypes(), args);
        return m;
    }

    protected static MemoryLayout findLayout(Class<?> cls, String name) {
        Method method = findMethod(cls, name + "$LAYOUT");
        assertNotNull(method);
        assertEquals(method.getReturnType(), MemoryLayout.class);
        try {
            return (MemoryLayout)method.invoke(null);
        } catch (Exception exp) {
            System.err.println(exp);
            assertTrue(false, "should not reach here");
        }
        return null;
    }

    protected static MemoryLayout findLayout(Class<?> cls) {
        return findLayout(cls, "");
    }

    protected static void checkField(MemoryLayout group, String fieldName, MemoryLayout expected) {
        assertEquals(group.select(PathElement.groupElement(fieldName)), expected.withName(fieldName));
    }

    protected static class Loader implements AutoCloseable {

        private final URLClassLoader loader;

        public Loader(URLClassLoader loader) {
            this.loader = loader;
        }

        public Class<?> loadClass(String className) {
            try {
                return Class.forName(className, false, loader);
            } catch (ClassNotFoundException e) {
                // return null so caller can check if class loading
                // was successful with assertNotNull/assertNull
                return null;
            }
        }

        @Override
        public void close() {
            try {
                loader.close();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
