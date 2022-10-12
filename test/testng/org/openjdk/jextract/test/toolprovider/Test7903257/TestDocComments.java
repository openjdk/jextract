/*
 * Copyright (c) 2022, Oracle and/or its affiliates. All rights reserved.
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

package org.openjdk.jextract.test.toolprovider.Test7903257;

import testlib.JextractToolRunner;
import testlib.TestUtils;
import org.testng.annotations.Test;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;
import static org.testng.Assert.assertEquals;

public class TestDocComments extends JextractToolRunner {
    // Regular expression for javadoc comment text
    //
    //   (?s)     dot matches all including newlines
    //   /\*\*    doc comment start
    //   (.*?)    comment text as a group (reluctant match)
    //   \*/      doc comment end
    private static final Pattern JAVADOC_COMMENT = Pattern.compile("(?s)/\\*\\*(.*?)\\*/");

    @Test
    public void testMacros() throws IOException {
        var comments = getDocComments("macros.h", "macros_h.java");
        assertEquals(comments, Set.of(
            "int FOO = 42;", "char* MSG = \"Hello\";"));
    }

    @Test
    public void testEnumConstants() throws IOException {
        var comments = getDocComments("enums.h", "enums_h.java");
        assertEquals(comments, Set.of(
            "int RED = 0;",
            "int GREEN = 1;",
            "int BLUE = 2;",
            "int club = 1;",
            "int diamonds = 2;",
            "int hearts = 3;",
            "int spades = 4;"));
    }

    @Test
    public void testTypedefs() throws IOException {
        var comments = getDocComments("typedefs.h", "typedefs_h.java");
        assertEquals(comments, Set.of(
            "typedef unsigned long size_t;",
            "typedef int INT_32;",
            "typedef int* INT_PTR;",
            "typedef struct Foo* OPAQUE_PTR;"));
    }

    @Test
    public void testArrays() throws IOException {
        var comments = getDocComments("arrays.h", "arrays_h.java");
        assertEquals(comments, Set.of(
            "int abc[10];",
            "float numbers[3];",
            "char* msg[5];"));
    }

    @Test
    public void testFunctions() throws IOException {
        var comments = getDocComments("functions.h", "functions_h.java");
        assertEquals(comments, Set.of(
            "int func(int* fp);",
            "double distance(struct Point p);",
            "int printf(char* fmt,...);"));
    }

    @Test
    public void testFunctionPointer() throws IOException {
        var comments = getDocComments("funcptrs.h", "funcptr.java");
        assertEquals(comments, Set.of(
            "void (*funcptr)(int*,int);"
        ));
    }

    @Test
    public void testVariables() throws IOException {
        var comments = getDocComments("variables.h", "variables_h.java");
        assertEquals(comments, Set.of(
            "Getter for variable: int abc;",
            "Setter for variable: int abc;",
            "Getter for variable: char* msg;",
            "Setter for variable: char* msg;"
        ));
    }

    @Test
    public void testStruct() throws IOException {
        var comments = getDocComments("structs.h", "Point.java");
        assertEquals(comments, Set.of(
            "struct Point { int x; int y; };"));
    }

    @Test
    public void testStruct2() throws IOException {
        var comments = getDocComments("structs.h", "Point3D.java");
        assertEquals(comments, Set.of(
            "struct Point3D { int x; int y; int z; };"));
    }

    @Test
    public void testStructTypdef() throws IOException {
        var comments = getDocComments("structs.h", "Point_t.java");
        assertEquals(comments, Set.of(
            "typedef struct Point Point_t;"));
    }

    private Set<String> getDocComments(String header, String outputFile)
            throws IOException {
        var output = getOutputFilePath("7903257-parse-" + header);
        var outputH = getInputFilePath(header);
        run("--source", "--output",
            output.toString(), outputH.toString()).checkSuccess();
        try {
            return findDocComments(Files.readString(output.resolve(outputFile)));
        } finally {
            TestUtils.deleteDir(output);
        }
    }

    // get doc comments from the given the source content
    private static Set<String> findDocComments(String content) {
        var matcher = JAVADOC_COMMENT.matcher(content);
        var strings = new HashSet<String>();
        while (matcher.find()) {
            // doc comment text is matched in group 1
            String rawComment = matcher.group(1);

            // sanitize raw comment for test asserts
            strings.add(rawComment
                // remove \n followed by whitespaces and then *
                .replaceAll("\n\\s+\\*", "")

                // replace one or more whitespaces as single whitespace
                .replaceAll("\\s+", " ")

                // remove trailing and leading whitespaces
                .trim());
        }
        return strings;
    }
}
