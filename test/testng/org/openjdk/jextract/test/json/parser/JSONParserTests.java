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

public class JSONParserTests {
    private final JSONParser parser = new JSONParser();

    @Test
    void testParseTrue() {
        var value = parser.parse("true");
        assertEquals(value.asBoolean(), true);
    }

    @Test
    void testParseFalse() {
        var value = parser.parse("false");
        assertEquals(value.asBoolean(), false);
    }

    @Test
    void testParseInt() {
        var value = parser.parse("17");
        assertEquals(value.asInt(), 17);
    }

    @Test
    void testParseTrueWithWhitespace() {
        var value = parser.parse("    true   \n \t   \r\n");
        assertEquals(value.asBoolean(), true);
    }

    @Test
    void testParseFalseWithWhitespace() {
        var value = parser.parse("\r\n    \t    false  \t\t\t");
        assertEquals(value.asBoolean(), false);
    }

    @Test
    void testParseString() {
        var value = parser.parse("\"Hello, JSON\"");
        assertEquals(value.asString(), "Hello, JSON");
    }

    @Test
    void testParseArray() {
        var value = parser.parse("[1,2,3]");
        assertEquals(value.asArray().get(0).asInt(), 1);
        assertEquals(value.asArray().get(1).asInt(), 2);
        assertEquals(value.asArray().get(2).asInt(), 3);
    }

    @Test
    void testParseNull() {
        var value = parser.parse("null");
        assertEquals(value.asArray(), null);
    }

    @Test
    void testParseObject() {
        var value = parser.parse("{\"a\":1,\"b\":2,\"c\":3}");
        assertEquals(value.asObject().get("a").asInt(), 1);
        assertEquals(value.asObject().get("b").asInt(), 2);
        assertEquals(value.asObject().get("c").asInt(), 3);
    }

    @Test
    void testParseArrayWithWhitespace() {
        var value = parser.parse("\n\n\n\t [  1, \t\n   2, \r\n \t \t 3  ] \t\t\t\n");
        assertEquals(value.asArray().get(0).asInt(), 1);
        assertEquals(value.asArray().get(1).asInt(), 2);
        assertEquals(value.asArray().get(2).asInt(), 3);
    }

    @Test
    void testParseObjectWithWhitespace() {
        var value = parser.parse("   \t \r\n   {  \t \n \r\"a\" \n\n \t : \r\n 1 \n\n, \"b\"  :  2 ,  \"c\"  :  3  }  ");
        assertEquals(value.asObject().get("a").asInt(), 1);
        assertEquals(value.asObject().get("b").asInt(), 2);
        assertEquals(value.asObject().get("c").asInt(), 3);
    }

    @Test
    void testObjectShortcut() {
        var value = parser.parse("{ \"a\":1, \"b\":2, \"c\":3 }");
        assertEquals(value.get("a").asInt(), 1);
        assertEquals(value.get("b").asInt(), 2);
        assertEquals(value.get("c").asInt(), 3);
    }

    @Test
    void testArrayShortcut() {
        var value = parser.parse("[ 1, 2, 3]");
        assertEquals(value.get(0).asInt(), 1);
        assertEquals(value.get(1).asInt(), 2);
        assertEquals(value.get(2).asInt(), 3);
    }

    @Test
    void testIntToString() {
        var v = JSON.of(17);
        assertEquals(v.toString(), "17");
    }

    @Test
    void testDoubleToString() {
        var v = JSON.of(17.7);
        assertEquals(v.toString(), "17.7");
    }

    @Test
    void testBooleanToString() {
        var v = JSON.of(true);
        assertEquals(v.toString(), "true");
    }

    @Test
    void testNullToString() {
        var v = JSON.of();
        assertEquals(v.toString(), "null");
    }

    @Test
    void testStringToString() {
        var v = JSON.of("Hello, JSON");
        assertEquals(v.toString(), "\"Hello, JSON\"");
    }

    @Test
    void testArrayToString() {
        var v = new JSONArray();
        v.add(1);
        v.add(2);
        v.add(3);
        assertEquals(v.toString(), "[1,2,3]");
    }

    @Test
    void testObjectToString() {
        var v = new JSONObject();
        v.put("a", 1);
        v.put("b", 2);
        v.put("c", 3);
        assertEquals(v.toString(), "{\"a\":1,\"b\":2,\"c\":3}");
    }

    @Test
    void testNestedObjectToString() {
        var inner = new JSONObject();
        inner.put("a", 1);
        inner.put("b", 2);
        inner.put("c", 3);

        var outer = new JSONObject();
        outer.put("inner", inner);
        assertEquals(outer.toString(), "{\"inner\":{\"a\":1,\"b\":2,\"c\":3}}");
    }

    @Test
    void testToStringAndParse() {
        var inner = new JSONObject();
        inner.put("a", 1);
        inner.put("b", 2);
        inner.put("c", 3);

        var outer = new JSONObject();
        outer.put("inner", inner);

        var s = outer.toString();

        var parsed = parser.parse(s);
        assertEquals(parsed.get("inner").get("a").asInt(), 1);
        assertEquals(parsed.get("inner").get("b").asInt(), 2);
        assertEquals(parsed.get("inner").get("c").asInt(), 3);
    }

    @Test
    void testLargerJSONText() {
        var text = "{\n" +
                   "  \"name\": \"mighty_readme\",\n" +
                   "  \"head_sha\": \"ce587453ced02b1526dfb4cb910479d431683101\",\n" +
                   "  \"status\": \"completed\",\n" +
                   "  \"started_at\": \"2017-11-30T19:39:10Z\",\n" +
                   "  \"completed_at\": \"2017-11-30T19:49:10Z\",\n" +
                   "  \"output\": {\n" +
                   "    \"title\": \"Mighty Readme report\",\n" +
                   "    \"summary\": \"There are 0 failures, 2 warnings, and 1 notices.\",\n" +
                   "    \"text\": \"You may have some misspelled words on lines 2 and 4. You also may want to add a section in your README about how to install your app.\",\n" +
                   "    \"annotations\": [\n" +
                   "      {\n" +
                   "        \"path\": \"README.md\",\n" +
                   "        \"annotation_level\": \"warning\",\n" +
                   "        \"title\": \"Spell Checker\",\n" +
                   "        \"message\": \"Check your spelling for 'banaas'.\",\n" +
                   "        \"raw_details\": \"Do you mean 'bananas' or 'banana'?\",\n" +
                   "        \"start_line\": \"2\",\n" +
                   "        \"end_line\": \"2\"\n" +
                   "      },\n" +
                   "      {\n" +
                   "        \"path\": \"README.md\",\n" +
                   "        \"annotation_level\": \"warning\",\n" +
                   "        \"title\": \"Spell Checker\",\n" +
                   "        \"message\": \"Check your spelling for 'aples'\",\n" +
                   "        \"raw_details\": \"Do you mean 'apples' or 'Naples'\",\n" +
                   "        \"start_line\": \"4\",\n" +
                   "        \"end_line\": \"4\"\n" +
                   "      }\n" +
                   "    ],\n" +
                   "    \"images\": [\n" +
                   "      {\n" +
                   "        \"alt\": \"Super bananas\",\n" +
                   "        \"image_url\": \"http://example.com/images/42\"\n" +
                   "      }\n" +
                   "    ]\n" +
                   "  },\n" +
                   "  \"actions\": [\n" +
                   "    {\n" +
                   "      \"label\": \"Fix\",\n" +
                   "      \"identifier\": \"fix_errors\",\n" +
                   "      \"description\": \"Allow us to fix these errors for you\"\n" +
                   "    }\n" +
                   "  ]\n" +
                   "}";
        var v = parser.parse(text);
        assertEquals(v.get("name").asString(), "mighty_readme");
        assertEquals(v.get("output").get("annotations").get(0).get("path").asString(), "README.md");
    }

    @Test
    void testAPI() {
        var o = JSON.object()
                    .put("a", 1)
                    .put("b", 2)
                    .put("c", 3);

        var parsed = JSON.parse(o.toString());
        assertEquals(parsed.get("a").asInt(), 1);
        assertEquals(parsed.get("b").asInt(), 2);
        assertEquals(parsed.get("c").asInt(), 3);

        var a = JSON.array()
                    .add("a")
                    .add(2)
                    .add(false)
                    .add(3.14);

        parsed = JSON.parse(a.toString());
        assertEquals(parsed.get(0).asString(), "a");
        assertEquals(parsed.get(1).asInt(), 2);
        assertEquals(parsed.get(2).asBoolean(), false);
        assertEquals(parsed.get(3).asDouble(), 3.14);

        var o2 = JSON.object()
                     .put("inner",
                        JSON.object()
                            .put("x", 1)
                            .put("y", "user_2")
                            .put("z", 2.1))
                     .put("array",
                        JSON.array()
                            .add(4)
                            .add(false)
                            .add("user_1"));

        parsed = JSON.parse(o2.toString());
        assertEquals(parsed.get("inner").get("x").asInt(), 1);
        assertEquals(parsed.get("inner").get("y").asString(), "user_2");
        assertEquals(parsed.get("inner").get("z").asDouble(), 2.1);
        assertEquals(parsed.get("array").get(0).asInt(), 4);
        assertEquals(parsed.get("array").get(1).asBoolean(), false);
        assertEquals(parsed.get("array").get(2).asString(), "user_1");
    }

    @Test
    void testParseStringWithCitation() {
        var v = JSON.parse("\"hello, \\\"citation\\\"\"");
        assertEquals("hello, \"citation\"", v.asString());
    }

    @Test
    void testParseStringBackslash() {
        var v = JSON.parse("\"hello, backslash: \\\\ \"");
        assertEquals("hello, backslash: \\ ", v.asString());
    }

    @Test
    void testParseStringBackslashAndN() {
        var v = JSON.parse("\"hello, backslash: \\\\n \"");
        assertEquals("hello, backslash: \\n ", v.asString());
    }

    @Test
    void testParseEmptyString() {
        var v = JSON.parse("\"\"");
        assertEquals("", v.asString());
    }

    @Test
    void testParseStringWithNewline() {
        var v = JSON.parse("\"hello newline\\n\"");
        assertEquals("hello newline\n", v.asString());
    }

    @Test
    void testStreamAPI() {
        var v = JSON.array().add(1).add(2).add(3);
        var a = v.stream().mapToInt(JSONValue::asInt).toArray();
        assertEquals(a[0], 1);
        assertEquals(a[1], 2);
        assertEquals(a[2], 3);

        var v2 = JSON.of(17.7);
        assertEquals(v2.stream().count(), 1L);
    }

    @Test
    void testIterateFieldsInObject() {
        var o = JSON.object()
                    .put("a", 1)
                    .put("b", 2)
                    .put("c", 3);

        var fields = o.fields();
        assertEquals(fields.size(), 3);

        var seen = new HashSet<String>();
        fields.forEach(f -> seen.add(f.name()));
        assertTrue(seen.contains("a"));
        assertTrue(seen.contains("b"));
        assertTrue(seen.contains("c"));
    }

    @Test
    void testObjectContains() {
        var o = JSON.object().put("a", 1);
        assertTrue(o.contains("a"));
        assertFalse(o.contains("b"));
    }

    @Test
    void testArrayIterator() {
        var array = JSON.array().add(1).add(2).add(3);
        var count = 0;
        for (var e : array) {
            count++;
        }
        assertEquals(count, 3);
    }

    @Test
    void testStringEncodingWithEscapedChars() {
        var s = JSON.of("hello newline\n");
        assertEquals("\"hello newline\\n\"", s.toString());

        s = JSON.of("backslash: \\");
        assertEquals("\"backslash: \\\\\"", s.toString());
    }

    @Test
    void testLongNumber() {
        var l = 1337L;
        var json = JSON.of(l);
        assertEquals("1337", json.toString());
        assertEquals(1337L, json.asLong());
        assertEquals(1337, json.asInt());
    }

    @Test
    void testEscapedUnicodeCodePoint() {
        var s = "\"\\ud83d\\ude04\"";
        var json = JSON.parse(s);
        assertEquals("\ud83d\ude04", json.asString());

        s = "\"\\u003c\"";
        json = JSON.parse(s);
        assertEquals("\u003c", json.asString());
        assertEquals("<", json.asString());
    }

    @Test
    void testLargeGitLabExample() {
        var s =
        "[" +
            "{" +
                "\"id\":369," +
                "\"iid\":2," +
                "\"project_id\":55," +
                "\"title\":\"Add some useful whitespace\","+
                "\"description\":\"It is that time.\\n\\n\\u003c!-- " +
                                  "Anything below this marker will be " +
                                  "automatically updated, please do not " +
                                  "edit manually! --\\u003e\\n\\n- [x] " +
                                  "Your change must have been available for " +
                                  "review at least 24 hours\\n- [ ] Title must " +
                                  "be of the format id: description where id " +
                                  "matches an existing JBS issue\"," +
                "\"state\":\"opened\"," +
                "\"created_at\":\"2018-09-06T11:52:39.314Z\"," +
                "\"updated_at\":\"2018-09-10T13:08:27.648Z\"," +
                "\"target_branch\":\"master\"," +
                "\"source_branch\":\"rwtest-1\"," +
                "\"upvotes\":0," +
                "\"downvotes\":0,"+
                "\"author\":{" +
                    "\"id\":2," +
                    "\"name\":\"User Number 3\"," +
                    "\"username\":\"user_3\"," +
                    "\"state\":\"active\"," +
                    "\"avatar_url\":\"avatar.png\"," +
                    "\"web_url\":\"https://host.com/user_3\"" +
                    "}," +
                "\"assignee\":null," +
                "\"source_project_id\":55," +
                "\"target_project_id\":55," +
                "\"labels\":[]," +
                "\"draft\":false," +
                "\"milestone\":null," +
                "\"merge_when_pipeline_succeeds\":false," +
                "\"merge_status\":\"can_be_merged\"," +
                "\"sha\":\"e282f1d56fa0710783d1c5d77a6c850669937a72\"," +
                "\"merge_commit_sha\":null," +
                "\"user_notes_count\":2," +
                "\"discussion_locked\":null," +
                "\"should_remove_source_branch\":null," +
                "\"force_remove_source_branch\":false," +
                "\"web_url\":\"https://host.com/user_3/test/merge_requests/2\"," +
                "\"time_stats\":{" +
                    "\"time_estimate\":0," +
                    "\"total_time_spent\":0," +
                    "\"human_time_estimate\":null," +
                    "\"human_total_time_spent\":null" +
                    "}," +
                "\"squash\":false" +
            "}" +
        "]";

        var json = JSON.parse(s);
        assertEquals(369, json.get(0).get("id").asInt());
        assertEquals("active", json.get(0).get("author").get("state").asString());
    }

    @Test
    public void testIsNull() {
        var json = JSON.parse("[{\"id\":705,\"type\":null,\"body\":\"description\"}]");
        assertTrue(json.get(0).get("type").isNull());
        assertFalse(json.get(0).get("type").isInt());
        assertFalse(json.get(0).get("type").isLong());
        assertFalse(json.get(0).get("type").isDouble());
        assertFalse(json.get(0).get("type").isString());
        assertFalse(json.get(0).get("type").isBoolean());
        assertFalse(json.get(0).get("type").isArray());
        assertFalse(json.get(0).get("type").isObject());

        assertFalse(json.get(0).get("id").isNull());
    }

    @Test
    public void testContainsShortcut() {
        var json = JSON.parse("{\"id\":705,\"type\":null,\"body\":\"description\"}");
        assertTrue(json.contains("id"));
        assertFalse(json.contains("header"));
        assertTrue(json.contains("type"));
    }

    @Test
    public void testFieldsShortcut() {
        var json = JSON.parse("{\"id\":705,\"type\":null,\"body\":\"description\"}");
        var names = json.fields().stream().map(JSONObject.Field::name).collect(Collectors.toSet());
        assertEquals(Set.of("id", "type", "body"), names);
    }

    @Test
    public void testArrayWithWhitespace() {
        var json = JSON.parse("{ \"foo\": [ ] }");
        assertEquals(0, json.get("foo").asArray().size());
    }

    @Test
    public void testObjectWithWhitespace() {
        var json = JSON.parse("{ \"foo\": { } }");
        assertEquals(0, json.get("foo").asObject().fields().size());
    }

    @Test
    public void testIsInt() {
        var json = JSON.parse("{ \"foo\": 1 }");

        assertTrue(json.get("foo").isInt());
        assertTrue(json.get("foo").isLong());

        assertFalse(json.get("foo").isDouble());
        assertFalse(json.get("foo").isString());
        assertFalse(json.get("foo").isBoolean());
        assertFalse(json.get("foo").isArray());
        assertFalse(json.get("foo").isObject());
        assertFalse(json.get("foo").isNull());
    }

    @Test
    public void testIsLong() {
        var json = JSON.parse("{ \"foo\": 1337 }");

        assertTrue(json.get("foo").isInt());
        assertTrue(json.get("foo").isLong());

        assertFalse(json.get("foo").isDouble());
        assertFalse(json.get("foo").isString());
        assertFalse(json.get("foo").isBoolean());
        assertFalse(json.get("foo").isArray());
        assertFalse(json.get("foo").isObject());
        assertFalse(json.get("foo").isNull());
    }

    @Test
    public void testIsDouble() {
        var json = JSON.parse("{ \"foo\": 17.7 }");

        assertTrue(json.get("foo").isDouble());

        assertFalse(json.get("foo").isInt());
        assertFalse(json.get("foo").isLong());
        assertFalse(json.get("foo").isBoolean());
        assertFalse(json.get("foo").isString());
        assertFalse(json.get("foo").isArray());
        assertFalse(json.get("foo").isObject());
        assertFalse(json.get("foo").isNull());
    }

    @Test
    public void testIsString() {
        var json = JSON.parse("{ \"foo\": \"bar\" }");

        assertTrue(json.get("foo").isString());

        assertFalse(json.get("foo").isInt());
        assertFalse(json.get("foo").isLong());
        assertFalse(json.get("foo").isDouble());
        assertFalse(json.get("foo").isBoolean());
        assertFalse(json.get("foo").isArray());
        assertFalse(json.get("foo").isObject());
        assertFalse(json.get("foo").isNull());
    }

    @Test
    public void testIsBoolean() {
        var json = JSON.parse("{ \"foo\": true }");

        assertTrue(json.get("foo").isBoolean());

        assertFalse(json.get("foo").isInt());
        assertFalse(json.get("foo").isLong());
        assertFalse(json.get("foo").isDouble());
        assertFalse(json.get("foo").isString());
        assertFalse(json.get("foo").isArray());
        assertFalse(json.get("foo").isObject());
        assertFalse(json.get("foo").isNull());
    }

    @Test
    public void testIsArray() {
        var json = JSON.parse("{ \"foo\": [1,2,3] }");

        assertTrue(json.get("foo").isArray());

        assertFalse(json.get("foo").isInt());
        assertFalse(json.get("foo").isLong());
        assertFalse(json.get("foo").isDouble());
        assertFalse(json.get("foo").isBoolean());
        assertFalse(json.get("foo").isString());
        assertFalse(json.get("foo").isObject());
        assertFalse(json.get("foo").isNull());
    }

    @Test
    public void testIsObject() {
        var json = JSON.parse("{ \"foo\": { \"bar\": true } }");

        assertTrue(json.get("foo").isObject());

        assertFalse(json.get("foo").isInt());
        assertFalse(json.get("foo").isLong());
        assertFalse(json.get("foo").isDouble());
        assertFalse(json.get("foo").isBoolean());
        assertFalse(json.get("foo").isString());
        assertFalse(json.get("foo").isArray());
        assertFalse(json.get("foo").isNull());
    }

    @Test
    public void testJSONObjectWithNullField() {
        var json = JSON.parse("{ \"foo\": null }");

        assertNotNull(json.get("foo"));
        assertTrue(json.get("foo").isNull());
    }
}
