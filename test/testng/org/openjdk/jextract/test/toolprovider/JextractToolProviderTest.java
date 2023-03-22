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
import org.testng.annotations.Test;
import testlib.JextractToolRunner;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.io.IOException;
import java.util.List;

import static org.testng.Assert.assertNotNull;

public class JextractToolProviderTest extends JextractToolRunner {
    @Test
    public void testHelp() {
        run().checkFailure(OPTION_ERROR); // no options
        run("--help").checkSuccess();
        run("-h").checkSuccess();
        run("-?").checkSuccess();
    }

    @Test
    public void testVersion() {
        run("--version").checkSuccess();
    }

    // error for non-existent args file
    @Test
    public void testNonExistentArgsFile() {
        run("@non_existent_args")
            .checkFailure(OPTION_ERROR)
            .checkContainsOutput("reading @argfile failed");
    }

    // error for non-existent header file
    @Test
    public void testNonExistentHeader() {
        run(getInputFilePath("non_existent.h").toString())
            .checkFailure(INPUT_ERROR)
            .checkContainsOutput("cannot read header file");
    }

    // error for header including non_existent.h file
    @Test
    public void testNonExistentIncluder() {
        run(getInputFilePath("non_existent_includer.h").toString())
            .checkFailure(CLANG_ERROR)
            .checkContainsOutput("file not found");
    }

    @Test
    public void testDirectoryAsHeader() {
        run(getInputFilePath("directory.h").toString())
            .checkFailure(INPUT_ERROR)
            .checkContainsOutput("not a file");
    }

    // error for header with parser errors
    @Test
    public void testHeaderWithDeclarationErrors() {
        run(getInputFilePath("illegal_decls.h").toString())
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
            run(getInputFilePath("unsupported_lang.h").toString())
                .checkFailure(RUNTIME_ERROR)
                .checkContainsOutput("Unsupported language: C++");
        } finally {
            Files.delete(compileFlagsTxt);
        }
    }

    @Test
    public void testOutputClass() {
        Path helloOutput = getOutputFilePath("hellogen");
        Path helloH = getInputFilePath("hello.h");
        run("--output", helloOutput.toString(), helloH.toString()).checkSuccess();
        try(TestUtils.Loader loader = TestUtils.classLoader(helloOutput)) {
            Class<?> cls = loader.loadClass("hello_h");
            // check a method for "void func(int)"
            assertNotNull(findMethod(cls, "func", int.class));
            // check a method for "int printf(MemorySegment, Object[])"
            assertNotNull(findMethod(cls, "printf", MemorySegment.class, Object[].class));
        } finally {
            TestUtils.deleteDir(helloOutput);
        }
    }

    @Test
    public void testArgsFile() {
        Path helloOutput = getOutputFilePath("hellogen");
        run("--output", helloOutput.toString(),
            "@" + getInputFilePath("helloargs").toString(),
            getInputFilePath("hello.h").toString()).checkSuccess();
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
        run(targetPkgOption, "com.acme", "--output",
            helloOutput.toString(), helloH.toString()).checkSuccess();
        try(TestUtils.Loader loader = TestUtils.classLoader(helloOutput)) {
            Class<?> cls = loader.loadClass("com.acme.hello_h");
            // check a method for "void func(int)"
            assertNotNull(findMethod(cls, "func", int.class));
            // check a method for "int printf(MemorySegment, Object[])"
            assertNotNull(findMethod(cls, "printf", MemorySegment.class, Object[].class));
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
        run("--header-class-name", "MyHello", "-t", "com.acme", "--output",
            helloOutput.toString(), helloH.toString()).checkSuccess();
        try(TestUtils.Loader loader = TestUtils.classLoader(helloOutput)) {
            Class<?> cls = loader.loadClass("com.acme.MyHello");
            // check a method for "void func(int)"
            assertNotNull(findMethod(cls, "func", int.class));
            // check a method for "int printf(MemorySegment, Object[])"
            assertNotNull(findMethod(cls, "printf", MemorySegment.class, Object[].class));
        } finally {
            TestUtils.deleteDir(helloOutput);
        }
    }

    @Test
    public void tesIncludeDirOption() {
        Path includerOutput = getOutputFilePath("includergen");
        Path includerH = getInputFilePath("includer.h");
        run("-I", includerH.getParent().resolve("inc").toString(),
            "--output", includerOutput.toString(), includerH.toString()).checkSuccess();
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
        run("--include-dir", includerH.getParent().resolve("inc").toString(),
            "--output", includerOutput.toString(), includerH.toString()).checkSuccess();
        try(TestUtils.Loader loader = TestUtils.classLoader(includerOutput)) {
            Class<?> cls = loader.loadClass("includer_h");
            // check a method for "void included_func(int)"
            assertNotNull(findMethod(cls, "included_func", int.class));
        } finally {
            TestUtils.deleteDir(includerOutput);
        }
    }
}
