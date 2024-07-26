/*
 * Copyright (c) 2024 Oracle and/or its affiliates. All rights reserved.
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

import org.openjdk.jextract.JextractTool;
import org.openjdk.jextract.Position;

import java.io.PrintWriter;
import java.text.MessageFormat;
import java.util.Locale;
import java.util.ResourceBundle;

/**
 * Minimal utility class to print jextract warning/errors.
 */
public class Logger {

    private static final ResourceBundle MESSAGES_BUNDLE =
            ResourceBundle.getBundle("org.openjdk.jextract.impl.resources.Messages", Locale.getDefault());

    final PrintWriter outWriter;
    final PrintWriter errWriter;
    private int nErrors;
    private int nClangErrors;

    public Logger(PrintWriter outWriter, PrintWriter errStream) {
        this.outWriter = outWriter;
        this.errWriter = errStream;
    }

    public void err(String key, Object... args) {
        err(Position.NO_POSITION, key, args);
    }

    public void err(Position pos, String key, Object... args) {
        String msg = Position.NO_POSITION.equals(pos) ?
            String.format("error: %1$s", format(key, args)) :
            String.format("%1$s: error: %2$s", position(pos), format(key, args));
        errWriter.println(msg);
        nErrors++;
    }

    public void warn(String key, Object... args) {
        warn(Position.NO_POSITION, key, args);
    }

    public void warn(Position pos, String key, Object... args) {
        String msg = Position.NO_POSITION.equals(pos) ?
            String.format("warning: %1$s", format(key, args)) :
            String.format("%1$s: warning: %2$s", position(pos), format(key, args));
        errWriter.println(msg);
    }

    public void info(String key, Object... args) {
        info(Position.NO_POSITION, key, args);
    }

    public void info(Position pos, String key, Object... args) {
        String msg = Position.NO_POSITION.equals(pos) ?
            format(key, args) : position(pos) + ": " + format(key, args);
        errWriter.println(msg);
    }

    public void clangErr(Position pos, String msg) {
        errWriter.println(
            Position.NO_POSITION.equals(pos) ?
                String.format("error: %1$s", msg) :
                String.format("%1$s: error: %2$s", position(pos), msg)
        );
        nClangErrors++;
    }

    public void clangWarn(Position pos, String msg) {
        errWriter.println(
            Position.NO_POSITION.equals(pos) ?
                String.format("warning: %1$s", msg) :
                String.format("%1$s: warning: %2$s", position(pos), msg)
        );
    }


    public void clangInfo(Position pos, String msg) {
        if (Position.NO_POSITION.equals(pos)) {
            errWriter.println(msg);
        } else {
            errWriter.println(position(pos) + ": " + msg);
        }
    }

    private String position(Position pos) {
        return pos.path().getFileName().toString() + ":" + pos.line() + ":" + pos.col();
    }

    public void printStackTrace(Throwable t) {
        t.printStackTrace(errWriter);
    }

    public void fatal(Throwable t, String msg, Object... args) {
        errWriter.println(String.format("fatal: %1$s", format(msg, args)));
        if (JextractTool.DEBUG) {
            printStackTrace(t);
        }
    }

    public void fatal(Throwable t) {
        fatal(t, "jextract.crash", t);
    }

    public boolean hasErrors() {
        return nErrors > 0;
    }

    public boolean hasClangErrors() {
        return nClangErrors > 0;
    }

    public String format(String key, Object... args) {
        return new MessageFormat(MESSAGES_BUNDLE.getString(key)).format(args);
    }

    public static Logger DEFAULT = new Logger(
            new PrintWriter(System.out, true),
            new PrintWriter(System.err, true));
}
