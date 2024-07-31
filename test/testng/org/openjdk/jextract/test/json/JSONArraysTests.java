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
package org.openjdk.jextract.test.json;

import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;
import java.util.function.Function;

import org.openjdk.jextract.json.JSONArrays;
import org.openjdk.jextract.json.JSONObjects;
import org.openjdk.jextract.json.parser.JSONArray;
import org.openjdk.jextract.json.parser.JSONBoolean;
import org.openjdk.jextract.json.parser.JSONNumber;
import org.openjdk.jextract.json.parser.JSONDecimal;
import org.openjdk.jextract.json.parser.JSONObject;
import org.openjdk.jextract.json.parser.JSONString;
import org.openjdk.jextract.json.parser.JSONValue;

import org.testng.annotations.Test;
import static org.testng.Assert.*;

public class JSONArraysTests {

    private void checkArrayElementTypes(JSONArray array, Class<?> type) {
        for (JSONValue elem : array) {
            assertEquals(elem.getClass(), type);
        }
    }

    @Test
    public void testBooleanArray() {
        var array = new boolean[] { true, false, false, true, false };
        JSONArray jsonArray = JSONArrays.from(array);
        assertEquals(jsonArray.size(), array.length);
        checkArrayElementTypes(jsonArray, JSONBoolean.class);
        // round trip
        assertEquals(JSONArrays.toBooleanArray(jsonArray), array);
        assertEquals((boolean[])JSONObjects.toObject(jsonArray, boolean[].class), array);
    }

    @Test
    public void testEmptyBooleanArray() {
        var array = new boolean[0];
        JSONArray jsonArray = JSONArrays.from(array);
        assertTrue(jsonArray.size() == 0);
        // round trip
        assertEquals(JSONArrays.toBooleanArray(jsonArray), array);
        assertEquals((boolean[])JSONObjects.toObject(jsonArray, boolean[].class), array);
    }

    @Test
    public void testCharArray() {
        var array = new char[] { 'J', 'a', 'v', 'a' };
        JSONArray jsonArray = JSONArrays.from(array);
        assertEquals(jsonArray.size(), array.length);
        checkArrayElementTypes(jsonArray, JSONString.class);
        // round trip
        assertEquals(JSONArrays.toCharArray(jsonArray), array);
        assertEquals((char[])JSONObjects.toObject(jsonArray, char[].class), array);
    }

    @Test
    public void testEmptyCharArray() {
        var array = new boolean[0];
        JSONArray jsonArray = JSONArrays.from(array);
        assertTrue(jsonArray.size() == 0);
        // round trip
        assertEquals(JSONArrays.toCharArray(jsonArray), array);
        assertEquals((char[])JSONObjects.toObject(jsonArray, char[].class), array);
    }

    @Test
    public void testByteArray() {
        var array = new byte[] { 34, Byte.MAX_VALUE, 63, -72, Byte.MIN_VALUE };
        JSONArray jsonArray = JSONArrays.from(array);
        assertEquals(jsonArray.size(), array.length);
        checkArrayElementTypes(jsonArray, JSONNumber.class);
        // round trip
        assertEquals(JSONArrays.toByteArray(jsonArray), array);
        assertEquals((byte[])JSONObjects.toObject(jsonArray, byte[].class), array);
    }

    @Test
    public void testEmptyByteArray() {
        var array = new byte[0];
        JSONArray jsonArray = JSONArrays.from(array);
        assertTrue(jsonArray.size() == 0);
        // round trip
        assertEquals(JSONArrays.toByteArray(jsonArray), array);
        assertEquals((byte[])JSONObjects.toObject(jsonArray, byte[].class), array);
    }

    @Test
    public void testShortArray() {
        var array = new short[] { Short.MAX_VALUE, 1 + Byte.MAX_VALUE,
            263, -3372, Short.MIN_VALUE, Byte.MIN_VALUE - 1 };
        JSONArray jsonArray = JSONArrays.from(array);
        assertEquals(jsonArray.size(), array.length);
        checkArrayElementTypes(jsonArray, JSONNumber.class);
        // round trip
        assertEquals(JSONArrays.toShortArray(jsonArray), array);
        assertEquals((short[])JSONObjects.toObject(jsonArray, short[].class), array);
    }

    @Test
    public void testEmptyShortArray() {
        var array = new short[0];
        JSONArray jsonArray = JSONArrays.from(array);
        assertTrue(jsonArray.size() == 0);
        // round trip
        assertEquals(JSONArrays.toShortArray(jsonArray), array);
        assertEquals((short[])JSONObjects.toObject(jsonArray, short[].class), array);
    }

    @Test
    public void testIntArray() {
        var array = new int[] { Integer.MAX_VALUE, 53, 563, -729,
            Integer.MIN_VALUE, Short.MAX_VALUE + 1, Short.MIN_VALUE - 1 };
        JSONArray jsonArray = JSONArrays.from(array);
        assertEquals(jsonArray.size(), array.length);
        checkArrayElementTypes(jsonArray, JSONNumber.class);
        // round trip
        assertEquals(JSONArrays.toIntArray(jsonArray), array);
        assertEquals((int[])JSONObjects.toObject(jsonArray, int[].class), array);
    }

    @Test
    public void testEmptyIntArray() {
        var array = new short[0];
        JSONArray jsonArray = JSONArrays.from(array);
        assertTrue(jsonArray.size() == 0);
        // round trip
        assertEquals(JSONArrays.toIntArray(jsonArray), array);
        assertEquals((int[])JSONObjects.toObject(jsonArray, int[].class), array);
    }

    @Test
    public void testLongArray() {
        var array = new long[] { Long.MAX_VALUE, Long.MIN_VALUE,
            Integer.MAX_VALUE + 1L, Integer.MIN_VALUE - 1L };
        JSONArray jsonArray = JSONArrays.from(array);
        assertEquals(jsonArray.size(), array.length);
        checkArrayElementTypes(jsonArray, JSONNumber.class);
        // round trip
        assertEquals(JSONArrays.toLongArray(jsonArray), array);
        assertEquals((long[])JSONObjects.toObject(jsonArray, long[].class), array);
    }

    @Test
    public void testEmptyLongArray() {
        var array = new long[0];
        JSONArray jsonArray = JSONArrays.from(array);
        assertTrue(jsonArray.size() == 0);
        // round trip
        assertEquals(JSONArrays.toLongArray(jsonArray), array);
        assertEquals((long[])JSONObjects.toObject(jsonArray, long[].class), array);
    }

    @Test
    public void testFloatArray() {
        final var DELTA = 0.001f;
        var array = new float[] { 3.14f, 2.18f, -56.91f, 92.62f, -555.19f };
        JSONArray jsonArray = JSONArrays.from(array);
        assertEquals(jsonArray.size(), array.length);
        checkArrayElementTypes(jsonArray, JSONDecimal.class);
        // round trip
        assertEquals(JSONArrays.toFloatArray(jsonArray), array, DELTA);
        assertEquals((float[])JSONObjects.toObject(jsonArray, float[].class), array);
    }

    @Test
    public void testEmptyFloatArray() {
        var array = new long[0];
        JSONArray jsonArray = JSONArrays.from(array);
        assertTrue(jsonArray.size() == 0);
        // round trip
        assertEquals(JSONArrays.toFloatArray(jsonArray), array);
        assertEquals((float[])JSONObjects.toObject(jsonArray, float[].class), array);
    }

    @Test
    public void testDoubleArray() {
        final var DELTA = 0.001;
        var array = new double[] { 34.23, 53.71, 563.89, -729.76, 43555.19 };
        JSONArray jsonArray = JSONArrays.from(array);
        assertEquals(jsonArray.size(), array.length);
        checkArrayElementTypes(jsonArray, JSONDecimal.class);
        // round trip
        assertEquals(JSONArrays.toDoubleArray(jsonArray), array, DELTA);
        assertEquals((double[])JSONObjects.toObject(jsonArray, double[].class), array);
    }

    @Test
    public void testEmptyDoubleArray() {
        var array = new double[0];
        JSONArray jsonArray = JSONArrays.from(array);
        assertTrue(jsonArray.size() == 0);
        // round trip
        assertEquals(JSONArrays.toDoubleArray(jsonArray), array);
        assertEquals((double[])JSONObjects.toObject(jsonArray, double[].class), array);
    }

    @Test
    public void testStringArray() {
        var array = new String[] { "Java", "is", "great!" };
        JSONArray jsonArray = JSONArrays.from(array);
        assertEquals(jsonArray.size(), array.length);
        checkArrayElementTypes(jsonArray, JSONString.class);
        // round trip
        assertEquals(JSONArrays.toStringArray(jsonArray), array);
        assertEquals((String[])JSONObjects.toObject(jsonArray, String[].class), array);
    }

    @Test
    public void testEmptyStringArray() {
        var array = new String[0];
        JSONArray jsonArray = JSONArrays.from(array);
        assertTrue(jsonArray.size() == 0);
        // round trip
        assertEquals(JSONArrays.toStringArray(jsonArray), array);
        assertEquals((String[])JSONObjects.toObject(jsonArray, String[].class), array);
    }

    public record Point(int x, int y) {}
    public enum Color { RED, GREEN, BLUE };

    private void checkPointArray(JSONArray jsonArray, Point[] points) {
        assertEquals(jsonArray.size(), points.length);
        for (int i = 0; i < points.length; i++) {
            JSONValue jv = jsonArray.get(i);
            assertTrue(jv.isObject());
            JSONObject jo = jv.asObject();
            assertTrue(jo.get("x").isInt());
            assertTrue(jo.get("y").isInt());
            assertEquals(jo.get("x").asInt(), points[i].x());
            assertEquals(jo.get("y").asInt(), points[i].y());
        }

        // check round trip conversion as well
        assertEquals(JSONArrays.toArray(jsonArray, new Point[0]), points);
    }

    private void checkColorArray(JSONArray jsonArray, Color[] colors) {
        assertEquals(jsonArray.size(), colors.length);
        for (int i = 0; i < colors.length; i++) {
            JSONValue jv = jsonArray.get(i);
            assertTrue(jv.isString());
            assertEquals(jv.asString(), colors[i].name());
        }

        // check round trip conversion as well
        assertEquals(JSONArrays.toArray(jsonArray, new Color[0]), colors);
    }

    private void checkStringArray(JSONArray jsonArray, String[] strings) {
        assertEquals(jsonArray.size(), strings.length);
        for (int i = 0; i < strings.length; i++) {
            JSONValue jv = jsonArray.get(i);
            assertTrue(jv.isString());
            assertEquals(jv.asString(), strings[i]);
        }

        // check round trip conversion as well
        assertEquals(JSONArrays.toStringArray(jsonArray), strings);
    }

    @Test
    public void testRecordArray() {
        var array = new Point[] { new Point(23, 45), new Point(-313, 4543) };
        JSONArray jsonArray = JSONArrays.from(array);
        assertEquals(jsonArray.size(), array.length);
        checkArrayElementTypes(jsonArray, JSONObject.class);
        checkPointArray(jsonArray, array);
    }

    @Test
    public void testEmptyRecordArray() {
        var array = new Point[0];
        JSONArray jsonArray = JSONArrays.from(array);
        assertTrue(jsonArray.size() == 0);
        checkPointArray(jsonArray, array);
    }

    @Test
    public void testEnumArray() {
        var array = new Color[] { Color.GREEN, Color.RED, Color.RED, Color.BLUE, Color.GREEN };
        JSONArray jsonArray = JSONArrays.from(array);
        assertEquals(jsonArray.size(), array.length);
        checkArrayElementTypes(jsonArray, JSONString.class);
        checkColorArray(jsonArray, array);
    }

    @Test
    public void testEmptyEnumArray() {
        var array = new Color[0];
        JSONArray jsonArray = JSONArrays.from(array);
        assertTrue(jsonArray.size() == 0);
        checkColorArray(jsonArray, array);
    }

    @Test
    public void testObjectArray() {
        var map = JSONObjects.toMap(JSONObjects.from(new Point(34, 124)));

        // array with mixed type elements
        var array = new Object[] {
            true, 'Z', false, -34, 431246, Integer.MIN_VALUE,
            (long)Integer.MAX_VALUE + 1L, Long.MAX_VALUE,
            Color.GREEN.name(), "Java", map, null,
            new String[] { "Java", "is", "great!" },
            new int[] { 33, 4343, -23434, Integer.MAX_VALUE }
        };
        JSONArray jsonArray = JSONArrays.from(array);
        assertEquals(jsonArray.size(), array.length);
        // round trip
        assertEquals(JSONArrays.toObjectArray(jsonArray), array);
        assertEquals((Object[])JSONObjects.toObject(jsonArray, Object[].class), array);
        // Object.class as target also returns an Object[]
        assertEquals((Object[])JSONObjects.toObject(jsonArray, Object.class), array);
    }

    @Test
    public void testEmptyObjectArray() {
        var array = new Object[0];
        JSONArray jsonArray = JSONArrays.from(array);
        assertTrue(jsonArray.size() == 0);
        // round trip
        assertEquals(JSONArrays.toObjectArray(jsonArray), array);
        assertEquals((Object[])JSONObjects.toObject(jsonArray, Object[].class), array);
        // Object.class as target also returns an Object[]
        assertEquals((Object[])JSONObjects.toObject(jsonArray, Object.class), array);
    }

    @Test
    public void testMultidimensionalArray() {
        int[][] int2DArray = new int[][] {
            { 23, 45, 345, -2389 },
            { 3423, 23 },
            { -23, 212, 129 }
        };
        JSONArray jsonArray = JSONArrays.from(int2DArray);
        assertTrue(jsonArray.size() == int2DArray.length);
        for (int i = 0; i < int2DArray.length; i++) {
            JSONArray jia = jsonArray.get(i).asArray();
            assertTrue(jia.size() == int2DArray[i].length);
            checkArrayElementTypes(jia, JSONNumber.class);
        }
        // round trip
        int[][] convertedIntArray = (int[][]) JSONObjects.toObject(jsonArray, int[][].class);
        assertTrue(Arrays.deepEquals(convertedIntArray, int2DArray));

        String[][] str2DArray = new String[][] {
            { "java", "is", "great!" },
            { "hello", "world" },
            { "abc", "xyz", "uvw", "ijk", "pqrs" }
        };
        jsonArray = JSONArrays.from(str2DArray);
        assertTrue(jsonArray.size() == str2DArray.length);
        for (int i = 0; i < str2DArray.length; i++) {
            JSONArray jia = jsonArray.get(i).asArray();
            assertTrue(jia.size() == str2DArray[i].length);
            checkArrayElementTypes(jia, JSONString.class);
        }
        // round trip
        String[][] convertedStrArray = (String[][]) JSONObjects.toObject(jsonArray, String[][].class);
        assertTrue(Arrays.deepEquals(convertedStrArray, str2DArray));

        Point[][] pts2DArray = new Point[][] {
            { new Point(3, 43), new Point(-43, 342) },
            { new Point(899, 143), new Point(123, 456), new Point(-2333, 565) }
        };
        jsonArray = JSONArrays.from(pts2DArray);
        assertTrue(jsonArray.size() == pts2DArray.length);
        for (int i = 0; i < pts2DArray.length; i++) {
            JSONArray jpa = jsonArray.get(i).asArray();
            assertTrue(jpa.size() == pts2DArray[i].length);
            checkArrayElementTypes(jpa, JSONObject.class);
        }
        // round trip
        Point[][] convertedPtsArray = (Point[][]) JSONObjects.toObject(jsonArray, Point[][].class);
        assertTrue(Arrays.deepEquals(convertedPtsArray, pts2DArray));
    }

    @Test
    public void testIterable() {
        var strings = new String[] { "hello", "world", "is", "great!" };
        JSONArray jsonArray = JSONArrays.from(Arrays.asList(strings));
        checkArrayElementTypes(jsonArray, JSONString.class);
        checkStringArray(jsonArray, strings);

        Point[] points = new Point[] { new Point(24, 342), new Point(423, -233) };
        jsonArray = JSONArrays.from(Arrays.asList(points));
        checkArrayElementTypes(jsonArray, JSONObject.class);
        checkPointArray(jsonArray, points);

        Color[] colors = new Color[] { Color.RED, Color.BLUE, Color.GREEN, Color.RED };
        jsonArray = JSONArrays.from(Arrays.asList(colors));
        checkArrayElementTypes(jsonArray, JSONString.class);
        checkColorArray(jsonArray, colors);
    }

    @Test
    public void testIterator() {
        var strings = new String[] { "java", "Java", "JAVA" };
        JSONArray jsonArray = JSONArrays.from(Stream.of(strings).iterator());
        checkArrayElementTypes(jsonArray, JSONString.class);
        checkStringArray(jsonArray, strings);

        Point[] points = new Point[] { new Point(334, 98), new Point(-99, 679) };
        jsonArray = JSONArrays.from(Stream.of(points).iterator());
        checkArrayElementTypes(jsonArray, JSONObject.class);
        checkPointArray(jsonArray, points);

        Color[] colors = new Color[] { Color.RED, Color.BLUE, Color.GREEN, Color.RED };
        jsonArray = JSONArrays.from(Stream.of(colors).iterator());
        checkArrayElementTypes(jsonArray, JSONString.class);
        checkColorArray(jsonArray, colors);
    }

    @Test
    public void testToList() {
        List<String> strList = List.of("java", "Java", "JAVA");
        JSONArray jsonArray = JSONArrays.from(strList);
        List<String> convertedStrList = JSONArrays.toList(jsonArray, String.class);
        // round trip check
        assertEquals(convertedStrList, strList);

        List<Point> pointList = List.of(new Point(432, 23), new Point(-231, 22));
        jsonArray = JSONArrays.from(pointList);
        List<Point> convertedPointList = JSONArrays.toList(jsonArray, Point.class);
        // round trip check
        assertEquals(convertedPointList, pointList);
    }

    @Test
    public void testListOfLists() {
        List<List<String>> listOfLists = List.of(
            List.of("java", "is", "great!"),
            List.of("hello", "world"),
            List.of("abc", "xyz", "uvw", "ijk", "pqrs")
        );
        JSONArray jsonArray = JSONArrays.from(listOfLists);
        assertTrue(jsonArray.size() == listOfLists.size());
        for (int i = 0; i < listOfLists.size(); i++) {
            JSONArray jia = jsonArray.get(i).asArray();
            assertTrue(jia.size() == listOfLists.get(i).size());
            checkArrayElementTypes(jia, JSONString.class);
        }

        // round trip
        record DummyRecord(List<List<String>> abc) {};
        Type type = DummyRecord.class.getRecordComponents()[0].getGenericType();
        assertEquals(JSONObjects.toObject(jsonArray, type), listOfLists);
    }

    @Test
    public void testToSet() {
        Set<String> strSet = Set.of("java", "Java", "JAVA");
        JSONArray jsonArray = JSONArrays.from(strSet);
        Set<String> convertedStrSet = JSONArrays.toSet(jsonArray, String.class);
        // round trip check
        assertEquals(convertedStrSet, strSet);

        Set<Point> pointSet = Set.of(new Point(2, 3), new Point(13, 32));
        jsonArray = JSONArrays.from(pointSet);
        Set<Point> convertedPointSet = JSONArrays.toSet(jsonArray, Point.class);
        // round trip check
        assertEquals(convertedPointSet, pointSet);
    }

    @Test
    public void testToEnumSet() {
        EnumSet<Color> cs = EnumSet.noneOf(Color.class);
        JSONArray colors = JSONArrays.from(cs);
        // round trip check
        EnumSet<Color> converted = JSONArrays.toEnumSet(colors, Color.class);
        assertEquals(converted, cs);

        cs = EnumSet.of(Color.RED);
        colors = JSONArrays.from(cs);
        assertEquals(colors.size(), 1);
        assertEquals(colors.get(0).asString(), "RED");
        // round trip check
        converted = JSONArrays.toEnumSet(colors, Color.class);
        assertEquals(converted, cs);

        cs = EnumSet.of(Color.RED, Color.GREEN, Color.RED);
        colors = JSONArrays.from(cs);
        // set and so duplicate removed
        assertEquals(colors.size(), 2);
        assertEquals(colors.get(0).asString(), "RED");
        assertEquals(colors.get(1).asString(), "GREEN");
        // round trip check
        converted = JSONArrays.toEnumSet(colors, Color.class);
        assertEquals(converted, cs);

        cs = EnumSet.allOf(Color.class);
        colors = JSONArrays.from(cs);
        assertEquals(colors.size(), 3);
        assertEquals(colors.get(0).asString(), "RED");
        assertEquals(colors.get(1).asString(), "GREEN");
        assertEquals(colors.get(2).asString(), "BLUE");
        // round trip check
        converted = JSONArrays.toEnumSet(colors, Color.class);
        assertEquals(converted, cs);
    }

    private void checkConversionFails(JSONArray jsonArray, Function<JSONArray, Object> func) {
        boolean exceptionSeen = false;
        try {
            func.apply(jsonArray);
        } catch (RuntimeException re) {
            var msg = re.getMessage();
            assertTrue(msg.contains("cannot convert") ||
                msg.contains("Unsupported conversion"), msg);
            exceptionSeen = true;
        }
        assertTrue(exceptionSeen);
    }

    @Test
    public void testArrayConversionFailures() {
        // array with mixed type elements
        var array = new Object[] {
            true, 'Z', false, -34, 431246, Integer.MIN_VALUE,
            (long)Integer.MAX_VALUE + 1L, Long.MAX_VALUE,
            Color.GREEN.name(), "Java", null,
            new String[] { "Java", "is", "great!" },
            new int[] { 33, 4343, -23434, Integer.MAX_VALUE }
        };

        JSONArray jsonArray = JSONArrays.from(array);

        // all homogeneous array conversions other than Object[] should fail
        checkConversionFails(jsonArray, JSONArrays::toBooleanArray);
        checkConversionFails(jsonArray, JSONArrays::toCharArray);
        checkConversionFails(jsonArray, JSONArrays::toByteArray);
        checkConversionFails(jsonArray, JSONArrays::toShortArray);
        checkConversionFails(jsonArray, JSONArrays::toIntArray);
        checkConversionFails(jsonArray, JSONArrays::toLongArray);
        checkConversionFails(jsonArray, JSONArrays::toFloatArray);
        checkConversionFails(jsonArray, JSONArrays::toDoubleArray);
        checkConversionFails(jsonArray, ja -> JSONArrays.toArray(ja, new Point[0]));
        checkConversionFails(jsonArray, ja -> JSONArrays.toArray(ja, new Color[0]));
    }
}
