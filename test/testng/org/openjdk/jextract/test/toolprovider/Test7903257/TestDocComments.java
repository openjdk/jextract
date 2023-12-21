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
import java.util.ArrayList;
import java.util.List;
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
        assertEquals(comments, List.of(
            "#define FOO 42", "#define MSG \"Hello\""));
    }

    @Test
    public void testEnumConstants() throws IOException {
        var comments = getDocComments("enums.h", "enums_h.java");
        assertEquals(comments, List.of(
            "enum Color.RED = 0",
            "enum Color.GREEN = 1",
            "enum Color.BLUE = 2",
            "enum Suit.club = 1",
            "enum Suit.diamonds = 2",
            "enum Suit.hearts = 3",
            "enum Suit.spades = 4"));
    }

    @Test
    public void testTypedefs() throws IOException {
        var comments = getDocComments("typedefs.h", "typedefs_h.java");
        assertEquals(comments, List.of(
            "typedef unsigned long long size_t",
            "typedef int INT_32",
            "typedef int *INT_PTR",
            "typedef struct Foo *OPAQUE_PTR"));
    }

    @Test
    public void testArrays() throws IOException {
        var comments = getDocComments("arrays.h", "arrays_h.java");
        assertEquals(comments, List.of(
            "int abc[10]",
            "float numbers[3]",
            "char *msg[5]",
            "int pixels[200][100]",
            "int points[10][20][30]"));
    }

    @Test
    public void testFunctions() throws IOException {
        var comments = getDocComments("functions.h", "functions_h.java");
        assertEquals(comments, List.of(
            "int func(int *fp)",
            "double distance(struct Point p)",
            "int printf(char *fmt, ...)",
            "int printf(char *fmt, ...)"));
    }

    @Test
    public void testFunctionPointer() throws IOException {
        var comments = getDocComments("funcptrs.h", "funcptr.java");
        assertEquals(comments, List.of(
            "void (*funcptr)(int *, int)"
        ));
    }

    @Test
    public void testFunctionPointer2() throws IOException {
        var comments = getDocComments("funcptrs.h", "signal$func.java");
        assertEquals(comments, List.of(
            "void (*func)(int)"
        ));
    }

    @Test
    public void testFunctionPointer3() throws IOException {
        var comments = getDocComments("funcptrs.h", "signal$return.java");
        assertEquals(comments, List.of(
            "void (*signal(int sig, void (*func)(int)))(int)"
        ));
    }

    @Test
    public void testFunctionPointer4() throws IOException {
        var comments = getDocComments("funcptrs.h", "funcptrs_h.java");
        assertEquals(comments, List.of(
            "Getter for variable: void (*funcptr)(int *, int)",
            "Setter for variable: void (*funcptr)(int *, int)",
            "void (*signal(int sig, void (*func)(int)))(int)"
        ));
    }

    @Test
    public void testVariables() throws IOException {
        var comments = getDocComments("variables.h", "variables_h.java");
        assertEquals(comments, List.of(
            "Getter for variable: int abc",
            "Setter for variable: int abc",
            "Getter for variable: char *msg",
            "Setter for variable: char *msg"
        ));
    }

    @Test
    public void testStruct() throws IOException {
        var comments = getDocComments("structs.h", "Point.java");
        assertEquals(comments, List.of(
            "struct Point { int x; int y; }",
            "Getter for field: int x",
            "Setter for field: int x",
            "Getter for field: int y",
            "Setter for field: int y"));
    }

    @Test
    public void testStruct2() throws IOException {
        var comments = getDocComments("structs.h", "Point3D.java");
        assertEquals(comments, List.of(
            "struct Point3D { int x; int y; int z; }",
            "Getter for field: int x",
            "Setter for field: int x",
            "Getter for field: int y",
            "Setter for field: int y",
            "Getter for field: int z",
            "Setter for field: int z"));
    }

    @Test
    public void testStructTypdef() throws IOException {
        var comments = getDocComments("structs.h", "Point_t.java");
        assertEquals(comments, List.of(
            "typedef struct Point { int x; int y; } Point_t"));
    }

    private List<String> getDocComments(String header, String outputFile)
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
    private static List<String> findDocComments(String content) {
        var matcher = JAVADOC_COMMENT.matcher(content);
        var strings = new ArrayList<String>();
        while (matcher.find()) {
            // doc comment text is matched in group 1
            String rawComment = matcher.group(1);

            // sanitize raw comment for test asserts
            strings.add(rawComment
                // remove \n followed by whitespaces and then *
                .replaceAll("\n\\s+\\*", "")

                // get rid of "{@snippet :" prefix
                .replaceAll("\\{@snippet lang=c :", "")

                // replace one or more whitespaces as single whitespace
                .replaceAll("\\s+", " ")

                // get rid of last "}" suffix closing the snippet
                .replaceAll("\\s+}\\s+$", "")

                .trim());
        }
        return strings;
    }
}
