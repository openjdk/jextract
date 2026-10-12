/*
 *  Copyright (c) 2026, Oracle and/or its affiliates. All rights reserved.
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
 *  Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 *  or visit www.oracle.com if you need additional information or have any
 *  questions.
 *
 */

package org.openjdk.jextract.impl;

/*
 * JavaDoc comments consist of two separate parts: the main description and the block tags. After the first block tag,
 * no regular text may follow. Doxygen does not have this strict structure. We therefore can't append everything from
 * the Doxygen comment in order. Instead, we maintain two StringBuilders to be able to append to the main text and the
 * block tags separately.
 *
 * Further, we have two additional StringBuilders for text that should appear at the top of the comment, and for Doxygen
 * block tags that do not exist in JavaDoc, and are instead appended to the end of the main text (and so before the
 * other block tags).
 */
public class JavaDocBuilder {
    private final StringBuilder beginMain = new StringBuilder();
    private final StringBuilder main = new StringBuilder();
    private final StringBuilder endMain = new StringBuilder();
    private final StringBuilder blockTags = new StringBuilder();

    public JavaDocBuilder appendBeginMain(String string) {
        beginMain.append(string);
        return this;
    }

    public JavaDocBuilder appendMain(String string) {
        main.append(string);
        return this;
    }

    public JavaDocBuilder appendEndMain(String string) {
        endMain.append(string);
        return this;
    }

    public JavaDocBuilder appendBlockTag(String string) {
        blockTags.append(string);
        return this;
    }

    public StringBuilder getBeginMain() {
        return beginMain;
    }

    public StringBuilder getMain() {
        return main;
    }

    public StringBuilder getEndMain() {
        return endMain;
    }

    public StringBuilder getBlockTags() {
        return blockTags;
    }

    public String build() {
        StringBuilder result = new StringBuilder();
        if (!beginMain.isEmpty()) {
            result.append(beginMain).append("\n");
        }
        if (!main.isEmpty()) {
            result.append(main).append("\n");
        }
        if (!endMain.isEmpty()) {
            result.append(endMain).append("\n");
        }
        if (!blockTags.isEmpty()) {
            result.append(blockTags);
        }
        return result.toString();
    }
}
