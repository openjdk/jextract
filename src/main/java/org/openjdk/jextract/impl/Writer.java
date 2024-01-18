/*
 *  Copyright (c) 2020, 2024, Oracle and/or its affiliates. All rights reserved.
 *  DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 *  This code is free software; you can redistribute it and/or modify it
 *  under the terms of the GNU General Public License version 2 only, as
 *  published by the Free Software Foundation.  Oracle designates this
 *  particular file as subject to the "Classpath" exception as provided
 *  by Oracle in the LICENSE file that accompanied this code.
 *
 *  This code is distributed in the hope that it will be useful, but WITHOUT
 *  ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 *  FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 *  version 2 for more details (a copy is included in the LICENSE file that
 *  accompanied this code).
 *
 *  You should have received a copy of the GNU General Public License version
 *  2 along with this work; if not, write to the Free Software Foundation,
 *  Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 *   Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 *  or visit www.oracle.com if you need additional information or have any
 *  questions.
 *
 */

package org.openjdk.jextract.impl;

import org.openjdk.jextract.JavaSourceFile;

import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public final class Writer {
    private final List<JavaSourceFile> files;
    private final Path dest;

    public Writer(Path dest, List<JavaSourceFile> files) {
        this.files = files;
        this.dest = dest;
    }

    public void write() throws IOException {
        Path destDir = createOutputDir();
        for (var entry : files) {
            String packagePath = packageNameToPath(entry.packageName());
            Path fullPath = destDir.resolve(packagePath, entry.className() + ".java").normalize();
            Path dir = fullPath.getParent();
            // In case the folder exist and is a link to a folder, this should be OK
            // Case in point, /tmp on MacOS link to /private/tmp
            if (Files.exists(dir)) {
                if (!Files.isDirectory(dir)) {
                    throw new FileAlreadyExistsException(dir.toAbsolutePath().toString());
                }
            } else {
                Files.createDirectories(fullPath.getParent());
            }
            Files.write(fullPath, List.of(entry.contents()));
        }
    }

    private static String packageNameToPath(String packageName) {
        return packageName.isEmpty() ? "" : packageName.replaceAll("\\.", "/") + "/";
    }

    private Path createOutputDir() throws IOException {
        Path absDest = dest.toAbsolutePath();
        if (!Files.exists(absDest)) {
            Files.createDirectories(absDest);
        }
        if (!Files.isDirectory(absDest)) {
            throw new IOException("Not a directory: " + dest);
        }
        return absDest;
    }
}
