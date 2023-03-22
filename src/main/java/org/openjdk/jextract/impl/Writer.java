/*
 *  Copyright (c) 2020, Oracle and/or its affiliates. All rights reserved.
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

import javax.tools.JavaFileObject;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

public final class Writer {
    private final List<? extends JavaFileObject> files;
    private final Path dest;

    public Writer(Path dest, List<? extends JavaFileObject> files) {
        this.files = files;
        this.dest = dest;
    }

    private List<JavaFileObject> ensureSourcesCompiled() {
        List<JavaFileObject> sources = sources();
        if (sources.isEmpty()) {
            return List.of();
        } else {
            return InMemoryJavaCompiler.compile(sources,
                "--enable-preview",
                "--source", "20",
                "-d", dest.toAbsolutePath().toString(),
                "-cp", dest.toAbsolutePath().toString());
        }
    }

    public void writeAll(boolean compileSources) throws IOException {
        writeClassFiles(resources());
        writeClassFiles(classes());
        if (compileSources) {
            writeClassFiles(ensureSourcesCompiled());
        } else {
            writeSourceFiles();
        }
    }

    void writeClassFiles(List<JavaFileObject> files) throws IOException {
        Path destDir = createOutputDir();
        for (var entry : files) {
            String path = entry.getName();
            Path fullPath = destDir.resolve(path).normalize();
            Files.createDirectories(fullPath.getParent());
            try (InputStream is = entry.openInputStream()) {
                Files.write(fullPath, is.readAllBytes());
            }
        }
    }

    void writeSourceFiles() throws IOException {
        Path destDir = createOutputDir();
        for (var entry : sources()) {
            String srcPath = entry.getName();
            Path fullPath = destDir.resolve(srcPath).normalize();
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
            Files.write(fullPath, List.of(entry.getCharContent(false)));
        }
    }

    private List<JavaFileObject> sources() {
        return files.stream()
                .filter(jfo -> jfo.getKind() == JavaFileObject.Kind.SOURCE)
                .collect(Collectors.toList());
    }

    private List<JavaFileObject> classes() {
        return files.stream()
                .filter(jfo -> jfo.getKind() == JavaFileObject.Kind.CLASS)
                .collect(Collectors.toList());
    }

    private List<JavaFileObject> resources() {
        return files.stream()
                .filter(jfo -> (jfo.getKind() == JavaFileObject.Kind.HTML || jfo.getKind() == JavaFileObject.Kind.OTHER))
                .collect(Collectors.toList());
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
