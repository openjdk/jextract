/*
 * Copyright (c) 2025, Oracle and/or its affiliates. All rights reserved.
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

import org.testng.annotations.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import testlib.JextractToolRunner;
import testlib.TestUtils;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;


/*
 * @test
 * @library /lib
 * @build testlib.JextractToolRunner testlib.TestUtils
 * @run testng/othervm --enable-native-access=ALL-UNNAMED TestLibraryPathResolver
 */
public class TestLibraryPathResolver extends JextractToolRunner {
    @Test
    public void testCustomResolver() throws Throwable {
        Path outputDir = getOutputFilePath("libraryPathResolverTestGen");
        Files.createDirectories(outputDir);

        try {
            Path pathResolverSource = outputDir.resolve("test/testLibraryPathResolver/MyPathResolver.java");
            Files.createDirectories(pathResolverSource.getParent());

            String pathResolverContent = """
                    package test.testLibraryPathResolver;

                    import java.nio.file.Files;
                    import java.nio.file.Path;
                    import java.nio.file.Paths;

                    public class MyPathResolver {
                        public static volatile boolean RESOLVER_CALLED = false;

                        public static String resolve(String libName) {
                            System.out.println("MyTestLoader: Custom resolver called for library: " + libName);
                            RESOLVER_CALLED = true;

                            String mappedLibName = System.mapLibraryName(libName);
                            String javaLibraryPath = System.getProperty("java.library.path");

                            if (javaLibraryPath == null) return mappedLibName;

                            String[] paths = javaLibraryPath.split(System.getProperty("path.separator"));
                            for (String dir : paths) {
                                Path path = Paths.get(dir).resolve(mappedLibName);
                                if (Files.exists(path)) {
                                    return path.toAbsolutePath().toString();
                                }
                            }

                            throw new UnsatisfiedLinkError("MyTestLoader: Could not find " + mappedLibName);
                        }
                    }
                    """;
            Files.writeString(pathResolverSource, pathResolverContent);

            Path headerFile = getInputFilePath("testLibraryPathResolver.h");
            run(outputDir,
                "-I", headerFile.getParent().toString(),
                "-t", "test.testLibraryPathResolver",
                "-l", "LibraryPathResolver",
                "--use-system-load-library",
                "--library-path-resolver", "test.testLibraryPathResolver.MyPathResolver#resolve",
                headerFile.toString()
            ).checkSuccess();

            TestUtils.compile(outputDir, outputDir);

            try (TestUtils.Loader loader = TestUtils.classLoader(outputDir)) {
                Class<?> pathResolverClass = loader.loadClass("test.testLibraryPathResolver.MyPathResolver");
                Class<?> headerClass = loader.loadClass("test.testLibraryPathResolver.testLibraryPathResolver_h");

                Method method = headerClass.getMethod("getMagicNumber");
                int result = (int) method.invoke(null);

                assertEquals(result, 990218, "Native function result mismatch");

                Field field = pathResolverClass.getField("RESOLVER_CALLED");
                boolean called = field.getBoolean(null);
                assertTrue(called, "Custom library resolver was NOT called!");
            }
        } finally {
            TestUtils.deleteDir(outputDir);
        }
    }
}
