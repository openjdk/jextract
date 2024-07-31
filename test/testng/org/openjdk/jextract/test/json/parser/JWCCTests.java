/*
 * Copyright (c) 2024, Oracle and/or its affiliates. All rights reserved.
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
package org.openjdk.jextract.test.json.parser;

import java.util.*;
import java.util.stream.*;
import org.openjdk.jextract.json.parser.*;

import org.testng.annotations.Test;
import static org.testng.Assert.*;

public class JWCCTests {
    @Test
    public void testSingleLineComment() {
        var text = """
                   // this is a comment before the object
                   { // this is a comment after opening brace
                     "foo": "bar" // this is a comment after field
                   } // this is a comment after closening brace
                   // this is a comment after the object
                   """;
        var json = JWCC.parse(text);
        assertEquals("bar", json.get("foo").asString());
    }

    @Test
    public void testSingleLineCommentAfterKey() {
        var text = """
                   {
                     "foo": // comment
                         "bar"
                   }
                   """;
        var json = JWCC.parse(text);
        assertEquals("bar", json.get("foo").asString());
    }

    @Test
    public void testSingleLineCommentAfterKeyWithoutValue() {
        var text = """
                   {
                     "foo": // comment
                   }
                   """;
        assertThrows(IllegalStateException.class, () -> {
            JWCC.parse(text);
        });
    }

    @Test
    public void testInlineComment() {
        var text = """
                   /*
                    * This is a multi-line comment
                    *
                    */
                   /*
                    * This is another multi-line comment with JSON in it
                    {
                      "foo": 17
                    }
                    */
                   /* small comment */ { /* another
                   multi-line */
                     /* before */ "foo" /* a comment */ : /* another comment */ "bar" /* so many comments */
                   } /* after */
                   /*
                    * A final multi-line
                    */
                   """;
        var json = JWCC.parse(text);
        assertEquals("bar", json.get("foo").asString());
    }

    @Test
    public void testInlineAndLineComment() {
        var text = """
                   /*
                    * This is a multi-line comment
                    * // with a line comment inside it
                    */
                   /*
                    * This is another multi-line comment with JSON in it
                    {
                      "foo": 17
                    }
                    */
                   /* small comment */ { // until end-of-line with closing brace }
                     /* before */ "foo" /* a comment */ : /* another comment */ "bar" /////// end-of-line
                   } /* after */ // end-of-line /* with in-line */
                   /*
                    * A final multi-line
                    */
                    // A final singe-line
                   """;
        var json = JWCC.parse(text);
        assertEquals("bar", json.get("foo").asString());
    }

    @Test
    public void testInlineAndLineCommentWithArray() {
        var text = """
                   /*
                    * This is a multi-line comment
                    * // with a line comment inside it
                    */
                   /*
                    * This is another multi-line comment with JSON in it
                    {
                      "foo": 17
                    }
                    */
                   /* small comment */ [ // until end-of-line with closing brace }
                     /* before */ "foo" /* a comment */ , /* another comment */ "bar" /////// end-of-line
                   ] /* after */ // end-of-line /* with in-line */
                   /*
                    * A final multi-line
                    */
                    // A final singe-line
                   """;
        var json = JWCC.parse(text);
        assertEquals("foo", json.get(0).asString());
        assertEquals("bar", json.get(1).asString());
        assertEquals(2, json.asArray().size());
    }

    @Test
    public void testTrailingComma() {
        var text = """
                   {
                       "a": 1,
                       "b": 2,
                   }
                   """;
        var json = JWCC.parse(text);
        assertEquals(1, json.get("a").asInt());
        assertEquals(2, json.get("b").asInt());
    }

    @Test
    public void testTrailingCommaWithLineComment() {
        var text = """
                   {
                       "a": 1, // a comment
                       "b": 2, // another comment
                   }
                   """;
        var json = JWCC.parse(text);
        assertEquals(1, json.get("a").asInt());
        assertEquals(2, json.get("b").asInt());
    }

    @Test
    public void testTrailingCommaWithInLineComment() {
        var text = """
                   {
                       "a": 1, /* an in-line */
                       "b": 2, /* another in-line */
                   }
                   """;
        var json = JWCC.parse(text);
        assertEquals(1, json.get("a").asInt());
        assertEquals(2, json.get("b").asInt());
    }

    @Test
    public void testTrailingOnSameLine() {
        var text = """
                   {
                       "a": 1, "b": 2, /* in-line */ "c": 3,
                   }
                   """;
        var json = JWCC.parse(text);
        assertEquals(1, json.get("a").asInt());
        assertEquals(2, json.get("b").asInt());
        assertEquals(3, json.get("c").asInt());
    }

    @Test
    public void testTrailingWithArray() {
        var text = """
                   [
                       "a",
                   ]
                   """;
        var json = JWCC.parse(text);
        assertEquals("a", json.get(0).asString());
    }

    @Test
    public void testTrailingWithMultipleArray() {
        var text = """
                   [
                       "a",
                       "b",
                   ]
                   """;
        var json = JWCC.parse(text);
        assertEquals("a", json.get(0).asString());
        assertEquals("b", json.get(1).asString());
    }
}
