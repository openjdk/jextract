/*
 * Copyright (c) 2021, 2024, Oracle and/or its affiliates. All rights reserved.
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
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;
import java.util.spi.ToolProvider;
import java.util.stream.Stream;

public class TestUtils {

    private static final ToolProvider JAVAC_TOOL = ToolProvider.findFirst("javac")
        .orElseThrow(() ->  new RuntimeException("javac tool not found"));
    private static final ToolProvider JAVADOC_TOOL = ToolProvider.findFirst("javadoc")
        .orElseThrow(() ->  new RuntimeException("javadoc tool not found"));

    public static Loader classLoader(Path... paths) {
        try {
            URL[] urls = new URL[paths.length];
            for (int i = 0; i < paths.length; i++) {
                urls[i] = paths[i].toUri().toURL();
            }
            URLClassLoader ucl = new URLClassLoader(urls,
                    TestUtils.class.getClassLoader());
            return new Loader(ucl);
        } catch (RuntimeException re) {
            throw re;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static void compile(Path sourcePath, Path outputDir) {
        List<String> files;
        try (Stream<Path> filesStream = Files.find(sourcePath.toAbsolutePath(), 999, (path, ignored) -> path.toString().endsWith(".java"))) {
            files = filesStream.map(p -> p.toAbsolutePath().toString()).toList();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        StringWriter writer = new StringWriter();
        PrintWriter pw = new PrintWriter(writer);

        try {
            System.err.println("compiling jextracted sources @ " + sourcePath.toAbsolutePath());
            List<String> commands = new ArrayList<>();
            commands.add("-parameters");
            commands.add("-d");
            commands.add(outputDir.toAbsolutePath().toString());
            commands.addAll(files);
            int result = JAVAC_TOOL.run(pw, pw, commands.toArray(new String[0]));
            if (result != 0) {
                System.err.println(writer);
                throw new RuntimeException("javac returns non-zero value");
            }
        } catch (Throwable t) {
            throw new AssertionError(t);
        }
        try {
            System.err.println("javadoc sources @ " + sourcePath.toAbsolutePath());
            List<String> commands = new ArrayList<>();
            commands.add("-Xdoclint:all,-missing");
            commands.addAll(files);
            int result = JAVADOC_TOOL.run(pw, pw, commands.toArray(new String[0]));
            if (result != 0) {
                System.err.println(writer);
                throw new RuntimeException("javadoc returns non-zero value");
            }
        } catch (Throwable t) {
            throw new AssertionError(t);
        }
    }

    public static class Loader implements AutoCloseable {

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

    public static void deleteDirIfExists(Path path) {
        if (Files.exists(path)) {
            deleteDir(path);
        }
    }

    public static void deleteDir(Path path) {
        try {
            deleteFileTreeWithRetry(path);
        } catch (IOException ioExp) {
            throw new RuntimeException(ioExp);
        }
    }

    private static final boolean IS_WINDOWS = System.getProperty("os.name").startsWith("Windows");
    private static final int RETRY_DELETE_MILLIS = IS_WINDOWS ? 500 : 0;
    private static final int MAX_RETRY_DELETE_TIMES = IS_WINDOWS ? 15 : 0;

    /**
     * Deletes a file, retrying if necessary.
     * No exception thrown if file doesn't exist.
     *
     * @param path  the file to delete
     *
     * @throws NoSuchFileException
     *         if the file does not exist (optional specific exception)
     * @throws DirectoryNotEmptyException
     *         if the file is a directory and could not otherwise be deleted
     *         because the directory is not empty (optional specific exception)
     * @throws IOException
     *         if an I/O error occurs
     */
    public static void deleteFileIfExistsWithRetry(Path path) throws IOException {
        try {
            if (!Files.notExists(path)) {
                deleteFileWithRetry0(path);
            }
        } catch (InterruptedException x) {
            throw new IOException("Interrupted while deleting.", x);
        }
    }

    private static void deleteFileWithRetry0(Path path)
            throws IOException, InterruptedException {
        int times = 0;
        IOException ioe = null;
        while (true) {
            try {
                Files.delete(path);
                // Checks for absence of the file. Semantics of Files.exists() is not the same.
                while (!Files.notExists(path)) {
                    times++;
                    if (times > MAX_RETRY_DELETE_TIMES) {
                        throw new IOException("File still exists after " + times + " waits.");
                    }
                    Thread.sleep(RETRY_DELETE_MILLIS);
                }
                break;
            } catch (NoSuchFileException | DirectoryNotEmptyException x) {
                throw x;
            } catch (IOException x) {
                // Backoff/retry in case another process is accessing the file
                times++;
                if (ioe == null) {
                    ioe = x;
                } else {
                    ioe.addSuppressed(x);
                }

                if (times > MAX_RETRY_DELETE_TIMES) {
                    throw ioe;
                }
                Thread.sleep(RETRY_DELETE_MILLIS);
            }
        }
    }

    /**
     * Deletes a directory and its subdirectories, retrying if necessary.
     *
     * @param dir  the directory to delete
     *
     * @throws  IOException
     *          If an I/O error occurs. Any such exceptions are caught
     *          internally. If only one is caught, then it is re-thrown.
     *          If more than one exception is caught, then the second and
     *          following exceptions are added as suppressed exceptions of the
     *          first one caught, which is then re-thrown.
     */
    public static void deleteFileTreeWithRetry(Path dir) throws IOException {
        IOException ioe = null;
        final List<IOException> excs = deleteFileTreeUnchecked(dir);
        if (!excs.isEmpty()) {
            ioe = excs.remove(0);
            for (IOException x : excs) {
                ioe.addSuppressed(x);
            }
        }
        if (ioe != null) {
            throw ioe;
        }
    }

    public static List<IOException> deleteFileTreeUnchecked(Path dir) {
        final List<IOException> excs = new ArrayList<>();
        try {
            java.nio.file.Files.walkFileTree(dir, new SimpleFileVisitor<>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                    try {
                        deleteFileWithRetry0(file);
                    } catch (IOException x) {
                        excs.add(x);
                    } catch (InterruptedException x) {
                        excs.add(new IOException("Interrupted while deleting.", x));
                        return FileVisitResult.TERMINATE;
                    }
                    return FileVisitResult.CONTINUE;
                }
                @Override
                public FileVisitResult postVisitDirectory(Path dir, IOException exc) {
                    try {
                        deleteFileWithRetry0(dir);
                    } catch (IOException x) {
                        excs.add(x);
                    } catch (InterruptedException x) {
                        excs.add(new IOException("Interrupted while deleting.", x));
                        return FileVisitResult.TERMINATE;
                    }
                    return FileVisitResult.CONTINUE;
                }
                @Override
                public FileVisitResult visitFileFailed(Path file, IOException exc) {
                    excs.add(exc);
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException x) {
            excs.add(x);
        }
        return excs;
    }

}
