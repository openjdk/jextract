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

import java.lang.foreign.MemorySegment;
import java.lang.foreign.MemoryLayout;
import testlib.TestUtils;
import org.testng.annotations.Test;
import testlib.JextractToolRunner;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.io.IOException;
import java.util.List;

import static org.testng.Assert.assertNotNull;

public class JextractToolProviderTest extends JextractToolRunner {
    private static final boolean IS_WINDOWS = System.getProperty("os.name").startsWith("Windows");

    @Test
    public void testHelp() {
        runNoOuput().checkFailure(OPTION_ERROR); // no options
        runNoOuput("--help").checkSuccess();
        runNoOuput("-h").checkSuccess();
        runNoOuput("-?").checkSuccess();
    }

    @Test
    public void testVersion() {
        runNoOuput("--version").checkSuccess();
    }

    // error for non-existent args file
    @Test
    public void testNonExistentArgsFile() {
        runNoOuput("@non_existent_args")
            .checkFailure(OPTION_ERROR)
            .checkContainsOutput("reading @argfile failed");
    }

    // error for non-existent header file
    @Test
    public void testNonExistentHeader() {
        runNoOuput(getInputFilePath("non_existent.h").toString())
            .checkFailure(CLANG_ERROR)
            .checkContainsOutput("file not found");
    }

    // error for header including non_existent.h file
    @Test
    public void testNonExistentIncluder() {
        runNoOuput(getInputFilePath("non_existent_includer.h").toString())
            .checkFailure(CLANG_ERROR)
            .checkContainsOutput("file not found");
    }

    @Test
    public void testDirectoryAsHeader() {
        runNoOuput(getInputFilePath("directory.h").toString())
            .checkFailure(CLANG_ERROR)
            .checkContainsOutput("file not found");
    }

    // error for header with parser errors
    @Test
    public void testHeaderWithDeclarationErrors() {
        runNoOuput(getInputFilePath("illegal_decls.h").toString())
            .checkFailure(CLANG_ERROR)
            .checkContainsOutput("cannot combine with previous 'short' declaration specifier");
    }

    // @bug 8267504: jextract should report unsupported language and exit rather
    // than generating partial nonworking code
    @Test
    public void testUnsupportedLanguage() throws IOException {
        Path compileFlagsTxt = Paths.get(".", "compile_flags.txt");
        try {
            Files.write(compileFlagsTxt, List.of("-xc++"));
            runNoOuput(getInputFilePath("unsupported_lang.h").toString())
                .checkFailure(FATAL_ERROR)
                .checkContainsOutput("Unsupported language: C++");
        } finally {
            Files.delete(compileFlagsTxt);
        }
    }

    @Test
    public void testOutputClass() {
        Path helloOutput = getOutputFilePath("hellogen");
        Path helloH = getInputFilePath("hello.h");
        runAndCompile(helloOutput, helloH.toString());
        try(TestUtils.Loader loader = TestUtils.classLoader(helloOutput)) {
            Class<?> cls = loader.loadClass("hello_h");
            checkHeaderMembers(cls);
        } finally {
            TestUtils.deleteDir(helloOutput);
        }
    }

    private static void checkHeaderMembers(Class<?> header) {
        // check a method for "void func(int)"
        assertNotNull(findMethod(header, "func", int.class));
        // check an interface for printf$invoker
        Class<?> invokerCls = findNestedClass(header, "printf");
        assertNotNull(invokerCls);
        // check a method for "MethodHandle handle()"
        assertNotNull(findMethod(invokerCls, "handle"));
        // check a method for "FunctionDescriptor descriptor()"
        assertNotNull(findMethod(invokerCls, "descriptor"));
        // check a method for "<invokerCls> invoker(MemoryLayout...)"
        assertNotNull(findMethod(invokerCls, "makeInvoker", MemoryLayout[].class));
    }

    @Test
    public void testArgsFile() {
        Path helloOutput = getOutputFilePath("hellogen");
        runAndCompile(helloOutput,
            "@" + getInputFilePath("helloargs").toString(),
            getInputFilePath("hello.h").toString());
        try(TestUtils.Loader loader = TestUtils.classLoader(helloOutput)) {
            Class<?> cls = loader.loadClass("com.acme.hello_h");
            assertNotNull(cls);
        } finally {
            TestUtils.deleteDir(helloOutput);
        }
    }

    private void testTargetPackage(String targetPkgOption) {
        Path helloOutput = getOutputFilePath("hellogen");
        Path helloH = getInputFilePath("hello.h");
        runAndCompile(helloOutput, targetPkgOption, "com.acme", helloH.toString());
        try(TestUtils.Loader loader = TestUtils.classLoader(helloOutput)) {
            Class<?> cls = loader.loadClass("com.acme.hello_h");
            checkHeaderMembers(cls);
        } finally {
            TestUtils.deleteDir(helloOutput);
        }
    }

    @Test
    public void testTargetPackageOption() {
        testTargetPackage("-t");
    }

    @Test
    public void testTargetPackageLongOption() {
        testTargetPackage("--target-package");
    }

    @Test
    public void testHeaderClassName() {
        Path helloOutput = getOutputFilePath("hellogen");
        Path helloH = getInputFilePath("hello.h");
        runAndCompile(helloOutput, "--header-class-name", "MyHello", "-t", "com.acme", helloH.toString());
        try(TestUtils.Loader loader = TestUtils.classLoader(helloOutput)) {
            Class<?> cls = loader.loadClass("com.acme.MyHello");
            checkHeaderMembers(cls);
        } finally {
            TestUtils.deleteDir(helloOutput);
        }
    }

    @Test
    public void tesIncludeDirOption() {
        Path includerOutput = getOutputFilePath("includergen");
        Path includerH = getInputFilePath("includer.h");
        runAndCompile(includerOutput, "-I", includerH.getParent().resolve("inc").toString(),
            includerH.toString());
        try(TestUtils.Loader loader = TestUtils.classLoader(includerOutput)) {
            Class<?> cls = loader.loadClass("includer_h");
            // check a method for "void included_func(int)"
            assertNotNull(findMethod(cls, "included_func", int.class));
        } finally {
            TestUtils.deleteDir(includerOutput);
        }
    }

    @Test
    public void tesIncludeDirOption2() {
        Path includerOutput = getOutputFilePath("includergen2");
        Path includerH = getInputFilePath("includer.h");
        runAndCompile(includerOutput, "--include-dir", includerH.getParent().resolve("inc").toString(),
            includerH.toString());
        try(TestUtils.Loader loader = TestUtils.classLoader(includerOutput)) {
            Class<?> cls = loader.loadClass("includer_h");
            // check a method for "void included_func(int)"
            assertNotNull(findMethod(cls, "included_func", int.class));
        } finally {
            TestUtils.deleteDir(includerOutput);
        }
    }

    @Test
    public void testSpecialHeaderSyntax() {
        Path helloOutput = getOutputFilePath("hello_special");
        runAndCompile(helloOutput, "-I", getInputDir().toString(), "<hello.h>");
        try(TestUtils.Loader loader = TestUtils.classLoader(helloOutput)) {
            Class<?> cls = loader.loadClass("hello_h");
            checkHeaderMembers(cls);
        }
    }


    @Test
    public void testSpecialStandardHeaderSyntax() {
        // standard include header files cannot be found on Windows without setting
        // the location of standard header explicitly!
        if (IS_WINDOWS) {
            System.err.println("skipping test that uses standard header");
        } else {
            Path stdioOutput = getOutputFilePath("stdio");
            runAndCompile(stdioOutput, "<stdio.h>");
            try(TestUtils.Loader loader = TestUtils.classLoader(stdioOutput)) {
                Class<?> cls = loader.loadClass("stdio_h");
                Class<?> invokerCls = findNestedClass(cls, "printf");
                assertNotNull(invokerCls);
                // check a method for "MethodHandle handle()"
                assertNotNull(findMethod(invokerCls, "handle"));
                // check a method for "FunctionDescriptor descriptor()"
                assertNotNull(findMethod(invokerCls, "descriptor"));
                // check a method for "<invokerCls> invoker(MemoryLayout...)"
                assertNotNull(findMethod(invokerCls, "makeInvoker", MemoryLayout[].class));
            }
        }
    }

    @Test
    public void testMultipleHeadersWithoutHeaderClassName() {
        runNoOuput("foo.h", "bar.h")
            .checkFailure(OPTION_ERROR)
            .checkContainsOutput("multiple headers specified without --header-class-name");
    }

    @Test
    public void testMultipleHeaders() {
        Path multiOutput = getOutputFilePath("multi");
        runAndCompile(multiOutput,
            "-I", getInputDir().toString(),
            "--header-class-name", "Multi",
            "hello.h", "typedefs.h");
        try(TestUtils.Loader loader = TestUtils.classLoader(multiOutput)) {
            Class<?> cls = loader.loadClass("Multi");

            // check stuff from hello.h
            checkHeaderMembers(cls);

            // check stuff from typedefs.h
            assertNotNull(findField(cls, "byte_t"));
            assertNotNull(findField(cls, "mysize_t"));
        }
    }

    @Test
    public void testStandardMultipleHeaders() {
        // standard include header files cannot be found on Windows without setting
        // the location of standard header explicitly!
        if (IS_WINDOWS) {
            System.err.println("skipping test that uses standard headers");
        } else {
            Path unixOutput = getOutputFilePath("unix");
            runAndCompile(unixOutput, "--header-class-name", "Unix", "<stdio.h>", "<stdlib.h>");
            try(TestUtils.Loader loader = TestUtils.classLoader(unixOutput)) {
                Class<?> cls = loader.loadClass("Unix");
                Class<?> invokerCls = findNestedClass(cls, "printf");
                assertNotNull(invokerCls);
                // check a method for "MethodHandle handle()"
                assertNotNull(findMethod(invokerCls, "handle"));
                // check a method for "FunctionDescriptor descriptor()"
                assertNotNull(findMethod(invokerCls, "descriptor"));
                // check a method for "<invokerCls> invoker(MemoryLayout...)"
                assertNotNull(findMethod(invokerCls, "makeInvoker", MemoryLayout[].class));

                // check qsort function
                assertNotNull(findMethod(cls, "qsort", MemorySegment.class, long.class, long.class, MemorySegment.class));
            }
        }
    }
}
