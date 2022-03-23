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

import javax.tools.FileObject;
import javax.tools.ForwardingJavaFileManager;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileManager;
import javax.tools.JavaFileObject;
import javax.tools.SimpleJavaFileObject;
import javax.tools.ToolProvider;
import java.io.*;
import java.io.Writer;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

final class InMemoryJavaCompiler {
    private InMemoryJavaCompiler() {}

    static List<JavaFileObject> compile(List<JavaFileObject> files,
                                        String... options) {
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        FileManager fileManager = new FileManager(compiler.getStandardFileManager(null, null, null));

        Writer writer = new StringWriter();
        Boolean exitCode = compiler.getTask(writer, fileManager, null, Arrays.asList(options), null, files).call();
        if (!exitCode) {
            throw new CompilationFailedException("In memory compilation failed: " + writer.toString());
        }
        return fileManager.getCompiledFiles();
    }

    static JavaFileObject jfoFromByteArray(URI uri, byte[] bytes) {
        return new SimpleJavaFileObject(uri, JavaFileObject.Kind.CLASS) {
            @Override
            public InputStream openInputStream() {
                return new ByteArrayInputStream(bytes);
            }
        };
    }

    static JavaFileObject jfoFromString(URI uri, String contents) {
        return new SimpleJavaFileObject(uri, JavaFileObject.Kind.SOURCE) {
            @Override
            public CharSequence getCharContent(boolean ignoreEncodingErrors) throws IOException {
                return contents;
            }
        };
    }

    // Wraper for class byte array
    private static class ClassFile extends SimpleJavaFileObject {
        private final ByteArrayOutputStream baos = new ByteArrayOutputStream();

        protected ClassFile(String name) {
            super(URI.create(name.replace('.', '/') + Kind.CLASS.extension), Kind.CLASS);
        }

        @Override
        public ByteArrayOutputStream openOutputStream() {
            return this.baos;
        }

        @Override
        public InputStream openInputStream() {
            return new ByteArrayInputStream(baos.toByteArray());
        }
    }

    // File manager which spawns ClassFile instances on demand
    private static class FileManager extends ForwardingJavaFileManager<JavaFileManager> {
        private final List<JavaFileObject> compiled = new ArrayList<>();

        protected FileManager(JavaFileManager fileManager) {
            super(fileManager);
        }

        @Override
        public JavaFileObject getJavaFileForOutput(Location location, String name, JavaFileObject.Kind kind, FileObject source) throws IOException {
            JavaFileObject out = new ClassFile(name);
            compiled.add(out);
            return out;
        }

        public List<JavaFileObject> getCompiledFiles() {
            return compiled;
        }
    }
}
