/*
 * Copyright (c) 2021, Oracle and/or its affiliates. All rights reserved.
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

import java.nio.file.Path;

import org.openjdk.jextract.test.TestUtils;
import org.testng.annotations.Test;
import jdk.incubator.foreign.MemorySegment;
import static org.testng.Assert.assertNotNull;

public class Test8260929 extends JextractToolRunner {
    @Test
    public void test() {
        Path outputPath = getOutputFilePath("output");
        Path headerFile = getInputFilePath("test8260929.h");
        run("-d", outputPath.toString(), headerFile.toString()).checkSuccess();
        try(TestUtils.Loader loader = TestUtils.classLoader(outputPath)) {
            assertNotNull(loader.loadClass("rab"));
            Class<?> rab2Class = loader.loadClass("rab2");
            assertNotNull(rab2Class);

            checkMethod(rab2Class, "y$get", int.class, MemorySegment.class);
            checkMethod(rab2Class, "y$get", int.class, MemorySegment.class, long.class);
            checkMethod(rab2Class, "y$set", void.class, MemorySegment.class, int.class);
            checkMethod(rab2Class, "y$set", void.class, MemorySegment.class, long.class, int.class);

            checkMethod(rab2Class, "x$get", short.class, MemorySegment.class);
            checkMethod(rab2Class, "x$get", short.class, MemorySegment.class, long.class);
            checkMethod(rab2Class, "x$set", void.class, MemorySegment.class, short.class);
            checkMethod(rab2Class, "x$set", void.class, MemorySegment.class, long.class, short.class);
        } finally {
            TestUtils.deleteDir(outputPath);
        }
    }
}
