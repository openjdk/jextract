/*
 * Copyright (c) 2026, Oracle and/or its affiliates. All rights reserved.
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

package org.openjdk.jextract.test.toolprovider.copiedComments;

import org.openjdk.jextract.test.toolprovider.docComments.TestDocComments;
import org.testng.annotations.Test;
import testlib.JextractToolRunner;
import testlib.TestUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.testng.Assert.*;

public class TestCopiedComments extends JextractToolRunner {

    private static final String COPIED_COMMENTS_HEADER = " * <p><strong>Copied comments:</strong></p>\n";

    @Test
    public void testArrays() throws IOException {
        Set<String> copiedComments = getCopiedComments("arrays.h", "arrays_h.java");
        TestDocComments.assertContains(copiedComments, List.of(
            "abc",
            """
            msg
            msg"""
        ));
    }

    @Test
    public void testEnums() throws IOException {
        Set<String> copiedComments = getCopiedComments("enums.h", "enums_h.java");
        TestDocComments.assertContains(copiedComments, List.of(
            "RED",
            "GREEN",
            "BLUE",
            "club",
            "diamonds",
            "hearts",
            "spades"
        ));
    }

    @Test
    public void testFunctionPointers() throws IOException {
        Set<String> copiedComments = getCopiedComments("funcptrs.h", "funcptrs_h.java");
        TestDocComments.assertContains(copiedComments, List.of(
            "this is a function pointer",
            "this is a signal"
        ));
    }

    @Test
    public void testFunctions() throws IOException {
        Set<String> copiedComments = getCopiedComments("functions.h", "functions_h.java");
        TestDocComments.assertContains(copiedComments, List.of(
            "one line",
            """
            two
            lines""",
            """
            these are four
            comments
            with whitespace
            in between""",
            """
            Doxygen
            &#64;param a parameter comment""",
            "a single line, multi-line comment",
            """
            a multi line,
            multi-line comment""",
            """
            internal

            comment""",
            """
            Doxygen
            &#64;param a par1
            &#64;param b par2
            &#64;return ret"""
        ));
    }

    @Test
    public void testMacros() throws IOException {
        Set<String> copiedComments = getCopiedComments("macros.h", "macros_h.java");
        TestDocComments.assertContains(copiedComments, List.of(
            "FOO",
            "MSG",
            "MSG_COMMENT"
        ));
    }

    @Test
    public void testStructs1() throws IOException {
        Set<String> copiedComments = getCopiedComments("structs.h", "Tuple.java");
        TestDocComments.assertContains(copiedComments, List.of(
            "Tuple comment",
            "x comment",
            "y comment"
        ));
    }

    @Test
    public void testStructs2() throws IOException {
        Set<String> copiedComments = getCopiedComments("structs.h", "Point3D.java");
        TestDocComments.assertContains(copiedComments, List.of(
            "Point3D comment",
            "x comment",
            "y comment",
            "z comment"
        ));
    }

    @Test
    public void testStructs3() throws IOException {
        Set<String> copiedComments = getCopiedComments("structs.h", "NestedAnon.java");
        TestDocComments.assertContains(copiedComments, List.of(
            "NestedAnon comment",
            "l comment",
            "h comment"
        ));
    }

    @Test
    public void testTypedefs() throws IOException {
        Set<String> copiedComments = getCopiedComments("typedefs.h", "typedefs_h.java");
        TestDocComments.assertContains(copiedComments, List.of(
            "size_t comment",
            "INT_32 comment",
            "INT_PTR comment",
            "OPAQUE_PTR comment"
        ));
    }

    @Test
    public void testVariables() throws IOException {
        Set<String> copiedComments = getCopiedComments("variables.h", "variables_h.java");
        TestDocComments.assertContains(copiedComments, List.of(
            "abc comment",
            "msg comment"
        ));
    }

    @Test
    public void testNormalization() throws IOException {
        Set<String> copiedComments = getCopiedComments("normalizationTests.h", "normalizationTests_h.java");
        TestDocComments.assertContains(copiedComments, List.of(
            "line comment without a space",
            "line comment with a space",
            "line   comment   with    many  spaces",
            "Doxygen line comment without a space",
            "Doxygen line comment with a space",
            "Doxygen    line   comment  with  many   spaces",
            "block comment without spaces",
            "block comment with spaces",
            """
            block
            comment
            with
            newlines""",
            """
            Doxygen
            comment
            with
            newlines""",
            "* line comment with leading asterisk",
            "* block comment with two leading asterisks"
        ));
    }

    @Test
    public void testSanitization() throws IOException {
        Set<String> copiedComments = getCopiedComments("sanitizationTests.h", "sanitizationTests_h.java");
        TestDocComments.assertContains(copiedComments, List.of(
            "&lt;!-- comment --&gt;",
            "&lt;p&gt;this is a paragraph",
            "&lt;p&gt;this is a paragraph too&lt;/p&gt;",
            "&lt;script&gt;alert(document.domain)&lt;/script&gt;",
            "&lt;img src=x onerror=\"alert(document.domain)\"&gt;",
            "A &amp; B",
            "A &amp;amp; B",
            "&#64;param param desc",
            "{ and }",
            "some {&#64;code code}",
            "email&#64;example.com",
            "block comment end *@/",
            "Unicode &#92;u20AC",
            "&#92;u002A&#92;u002F",
            "&#64;brief a variable"
        ));
    }

    private Set<String> getCopiedComments(String header, String outputFile)
            throws IOException {
        var output = getOutputFilePath("TestCopiedComments-parse-" + header);
        var outputH = getInputFilePath(header);
        run(output, outputH.toString(), "--copy-comments");
        try {
            return findCopiedComments(Files.readString(output.resolve(outputFile)));
        } finally {
            TestUtils.deleteDir(output);
        }
    }

    // find copied comments from the given the source content
    private static Set<String> findCopiedComments(String content) {
        var matcher = TestDocComments.JAVADOC_COMMENT.matcher(content);
        Set<String> strings = new HashSet<>();
        while (matcher.find()) {
            // doc comment text is matched in group 1
            String rawComment = matcher.group(1);

            int index = rawComment.indexOf(COPIED_COMMENTS_HEADER);
            if (index == -1) {
                continue;
            }
            String copiedComment = rawComment.substring(index + COPIED_COMMENTS_HEADER.length());

            // sanitize raw comment for test asserts
            strings.add(copiedComment
                // remove indentation and starting `* `
                .replaceAll("(?m)^ * \\* ", "")
                .strip());
        }
        return strings;
    }
}
