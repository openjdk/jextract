/*
 * Copyright (c) 2023, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
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
package org.openjdk.jextract.impl;

import javax.tools.JavaFileObject;
import java.util.function.Function;

public class SourceFileBuilder {

    private static final boolean SHOW_GENERATING_CLASS = Boolean.getBoolean("jextract.showGeneratingClass");

    private final String packageName;
    private final String className;

    // code buffer
    private final StringBuilder sb = new StringBuilder();
    // current line alignment (number of 4-spaces)
    private int align;

    public SourceFileBuilder(String packageName, String className) {
        this.packageName = packageName;
        this.className = className;
    }

    public String packageName() {
        return packageName;
    }

    public String className() {
        return className;
    }

    protected void emitPackagePrefix() {
        assert packageName().indexOf('/') == -1 : "package name invalid: " + packageName();
        append("// Generated by jextract");
        if (SHOW_GENERATING_CLASS) {
            append(" (via ");
            append(getClass().getName());
            append(")");
        }
        append("\n\n");
        if (!packageName().isEmpty()) {
            append("package ");
            append(packageName());
            append(";\n\n");
        }
    }

    protected void emitImportSection() {
        append("import java.lang.invoke.MethodHandle;\n");
        append("import java.lang.invoke.VarHandle;\n");
        append("import java.nio.ByteOrder;\n");
        append("import java.lang.foreign.*;\n");
        append("import static java.lang.foreign.ValueLayout.*;\n");
    }


    // Internal generation helpers (used by other builders)

    void append(String s) {
        sb.append(s);
    }

    void append(char c) {
        sb.append(c);
    }

    void append(long l) {
        sb.append(l);
    }

    void indent() {
        for (int i = 0; i < align; i++) {
            append("    ");
        }
    }

    void incrAlign() {
        align++;
    }

    void decrAlign() {
        align--;
    }

    int align() {
        return align;
    }

    public JavaFileObject toFile(Function<String, String> finisher) {
        return Utils.fileFromString(packageName, className, finisher.apply(sb.toString()));
    }

    public JavaFileObject toFile() {
        return toFile(s -> s);
    }
}
