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

package test.jextract.testLibraryResolver;

import org.testng.annotations.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;
import static test.jextract.testLibraryResolver.testLibraryResolver_h.*;

/*
 * @test
 * @library /lib
 * @build testlib.TestUtils
 * @run main/othervm JtregJextract -l TestLibraryResolver --use-system-load-library --library-path-resolver test.jextract.testLibraryResolver.TestLibraryResolver$MyTestLoader#resolve -t test.jextract.testLibraryResolver testLibraryResolver.h
 * @build test.jextract.testLibraryResolver.TestLibraryResolver
 * @run testng/othervm --enable-native-access=ALL-UNNAMED test.jextract.testLibraryResolver.TestLibraryResolver
 */
public class TestLibraryResolver {
    /**
     * A simple static inner class to act as the library path resolver.
     * This will be referenced by the @run command.
     */
    public static class MyTestLoader {
        public static volatile boolean RESOLVER_CALLED = false;

        public static String resolve(String libName) {
            System.out.println("MyTestLoader: Custom resolver called for library: " + libName);
            RESOLVER_CALLED = true;

            String mappedLibName = System.mapLibraryName(libName);

            String javaLibraryPath = System.getProperty("java.library.path");
            if (javaLibraryPath == null || javaLibraryPath.isEmpty()) {
                throw new RuntimeException("java.library.path is not set");
            }

            String[] paths = javaLibraryPath.split(System.getProperty("path.separator"));
            for (String dir : paths) {
                Path path = Paths.get(dir).resolve(mappedLibName);
                if (Files.exists(path)) {
                    String absolutePath = path.toAbsolutePath().toString();
                    System.out.println("MyTestLoader: Resolved " + libName + " to " + absolutePath);
                    return absolutePath;
                }
            }

            throw new UnsatisfiedLinkError("MyTestLoader: Could not find library '" + mappedLibName +
                                           "' in java.library.path: " + javaLibraryPath);
        }
    }

    @Test
    void testCustomResolverWasCalled() {
        int magic = getMagicNumber();

        assertEquals(magic, 990218,
                     "The native function call returned an unexpected value.");
        assertTrue(MyTestLoader.RESOLVER_CALLED,
                   "The custom --library-path-resolver (MyTestLoader.resolve) was not invoked.");
    }
}
