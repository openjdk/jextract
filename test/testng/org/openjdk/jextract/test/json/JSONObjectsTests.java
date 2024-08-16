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
import java.net.URI;
import java.time.LocalDate;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.OptionalLong;
import java.util.OptionalDouble;
import java.util.Set;
import java.util.regex.Pattern;

import org.openjdk.jextract.json.JSONObjects;
import org.openjdk.jextract.json.parser.JSONArray;
import org.openjdk.jextract.json.parser.JSONBoolean;
import org.openjdk.jextract.json.parser.JSONDecimal;
import org.openjdk.jextract.json.parser.JSONNull;
import org.openjdk.jextract.json.parser.JSONNumber;
import org.openjdk.jextract.json.parser.JSONObject;
import org.openjdk.jextract.json.parser.JSONString;
import org.openjdk.jextract.json.parser.JSONValue;

import org.testng.annotations.Test;
import static org.testng.Assert.*;

public class JSONObjectsTests {
    // delta for floating point comparisons
    private static final double DELTA = 0.0001;

    // Tests for Java objects to JSONValue subtypes conversions

    @Test
    public void testFromPrimitive() {
        boolean b = true;
        JSONValue jv = JSONObjects.from(b);
        assertTrue(jv.isBoolean());
        assertEquals(jv.asBoolean(), b);

        b = false;
        jv = JSONObjects.from(b);
        assertTrue(jv.isBoolean());
        assertEquals(jv.asBoolean(), b);

        char c = 'U';
        jv = JSONObjects.from(c);
        assertTrue(jv.isString());
        assertEquals(jv.asChar(), c);

        byte bi = 33;
        jv = JSONObjects.from(bi);
        assertTrue(jv.isInt());
        assertEquals(jv.asByte(), bi);

        short s = 3324;
        jv = JSONObjects.from(s);
        assertTrue(jv.isInt());
        assertEquals(jv.asShort(), s);

        int i = 343233413;
        jv = JSONObjects.from(i);
        assertTrue(jv.isInt());
        assertEquals(jv.asInt(), i);

        long l = Long.MAX_VALUE;
        jv = JSONObjects.from(l);
        assertTrue(jv.isLong());
        assertEquals(jv.asLong(), l);

        float f = 3.14f;
        jv = JSONObjects.from(f);
        assertTrue(jv.isDouble());
        assertEquals(jv.asFloat(), f, (float)DELTA);

        double d = 3.14;
        jv = JSONObjects.from(d);
        assertTrue(jv.isDouble());
        assertEquals(jv.asDouble(), d, DELTA);
    }

    @Test
    public void testFromPrimitiveFailures() {
        JSONValue jv = JSONObjects.from("hello");
        assertTrue(jv.isString());
        try {
            // cannot convert a String that has
            // more than one character to char
            jv.asChar();
            throw new RuntimeException("should not reach here");
        } catch (RuntimeException re) {
            assertTrue(re.getMessage().contains("cannot convert"));
        }

        jv = JSONObjects.from("");
        assertTrue(jv.isString());
        try {
            // empty string has no character
            jv.asChar();
            throw new RuntimeException("should not reach here");
        } catch (RuntimeException re) {
            assertTrue(re.getMessage().contains("cannot convert"));
        }

        short s = Byte.MAX_VALUE + 1;
        jv = JSONObjects.from(s);
        assertTrue(jv.isInt());
        // overflow value cannot be converted to byte
        try {
            jv.asByte();
            throw new RuntimeException("should not reach here");
        } catch (ArithmeticException ae) {
            assertTrue(ae.getMessage().contains("byte overflow"));
        }

        int i = Short.MIN_VALUE - 1;
        jv = JSONObjects.from(i);
        assertTrue(jv.isInt());
        // overflow value cannot be converted to byte
        try {
            jv.asByte();
            throw new RuntimeException("should not reach here");
        } catch (ArithmeticException ae) {
            assertTrue(ae.getMessage().contains("byte overflow"));
        }
        // overflow value cannot be converted to short
        try {
            jv.asShort();
            throw new RuntimeException("should not reach here");
        } catch (ArithmeticException ae) {
            assertTrue(ae.getMessage().contains("short overflow"));
        }

        long l = Long.MAX_VALUE;
        jv = JSONObjects.from(l);
        assertTrue(jv.isLong());
        // overflow value cannot be converted to int
        try {
            jv.asInt();
            throw new RuntimeException("should not reach here");
        } catch (ArithmeticException ae) {
            assertTrue(ae.getMessage().contains("integer overflow"));
        }
    }

    @Test
    public void testFromString() {
        String s = "hello";
        JSONValue jv = JSONObjects.from(s);
        assertTrue(jv.isString());
        assertEquals(jv.asString(), s);
    }

    @Test
    public void testFromNull() {
        JSONValue jv = JSONObjects.from((Object)null);
        assertTrue(jv.isNull());
    }

    @Test
    public void testFromOptionals() {
        // Optional empty values are converted to JSONNull
        JSONValue jv = JSONObjects.from(Optional.empty());
        assertTrue(jv.isNull());
        jv = JSONObjects.from(OptionalInt.empty());
        assertTrue(jv.isNull());
        jv = JSONObjects.from(OptionalInt.empty());
        assertTrue(jv.isNull());
        jv = JSONObjects.from(OptionalLong.empty());
        assertTrue(jv.isNull());
        jv = JSONObjects.from(OptionalDouble.empty());
        assertTrue(jv.isNull());

        // non-empty Optional values are converted from underlying values
        Optional<String> os = Optional.of("Hello");
        jv = JSONObjects.from(os);
        assertTrue(jv.isString());
        assertEquals(jv.asString(), os.get());

        OptionalInt oi = OptionalInt.of(4234);
        jv = JSONObjects.from(oi);
        assertTrue(jv instanceof JSONNumber);
        assertEquals(jv.asInt(), oi.getAsInt());

        OptionalLong ol = OptionalLong.of(Long.MIN_VALUE);
        jv = JSONObjects.from(ol);
        assertTrue(jv instanceof JSONNumber);
        assertEquals(jv.asLong(), ol.getAsLong());

        OptionalDouble od = OptionalDouble.of(Math.PI);
        jv = JSONObjects.from(od);
        assertTrue(jv instanceof JSONDecimal);
        assertEquals(jv.asDouble(), od.getAsDouble(), DELTA);
    }

    public record Point(int x, int y) {}

    // more complex record types
    public record UserInfo(String name,
        List<String> address,
        String email,
        Optional<List<String>> alternateAddress,
        Optional<URI> homePage) {}

    public enum Color { RED, GREEN, BLUE }

    @Test
    public void testFromEnum() {
        Color c = Color.BLUE;
        JSONValue jv = JSONObjects.from(c);
        assertTrue(jv.isString());
        assertEquals(jv.asString(), c.name());

        c = Color.RED;
        jv = JSONObjects.from(c);
        assertTrue(jv.isString());
        assertEquals(jv.asString(), c.name());
    }

    @Test
    public void testFromRecord() {
        Point pt = new Point(44, -289);
        JSONObject jo = JSONObjects.from(pt);
        assertEquals(jo.size(), Point.class.getRecordComponents().length);
        assertEquals(jo.get("x").asInt(), pt.x());
        assertEquals(jo.get("y").asInt(), pt.y());

        // try a complex record
        UserInfo ui = new UserInfo("Batman",
            List.of("224", "Park Drive", "Gotham City"),
            "bruce.wayne@batcave.com",
            Optional.empty(),
            Optional.of(URI.create("http://www.thebatman.com")));
        jo = JSONObjects.from(ui);
        assertEquals(jo.size(), UserInfo.class.getRecordComponents().length);
        assertEquals(jo.get("name").asString(), ui.name());
        assertEquals(jo.get("email").asString(), ui.email());
        assertTrue(jo.get("address").isArray());
        JSONArray address = jo.get("address").asArray();
        assertEquals(address.size(), ui.address().size());
        assertEquals(address.get(0).asString(), ui.address().get(0));
        assertEquals(address.get(1).asString(), ui.address().get(1));
        assertEquals(address.get(2).asString(), ui.address().get(2));
        // empty Optional value
        assertTrue(jo.get("alternateAddress").isNull());
        assertEquals(URI.create(jo.get("homePage").asString()), ui.homePage().get());
    }

    @Test
    public void testFromMap() {
        Map<String, Object> map = Map.of("x", 44243, "y", 5455);

        JSONObject jo = JSONObjects.from(map);
        assertEquals(jo.size(), map.size());
        assertEquals(jo.get("x").asInt(), (int)map.get("x"));
        assertEquals(jo.get("y").asInt(), (int)map.get("y"));
    }

    // tests for JSONValues to Java object conversions

    @Test
    public void testJSONBooleanConversions() {
        JSONBoolean jb = new JSONBoolean(false);
        assertEquals(JSONObjects.toObject(jb, boolean.class), jb.asBoolean());
        assertEquals(JSONObjects.toObject(jb, Boolean.class), jb.asBoolean());
        assertEquals(JSONObjects.toObject(jb, Object.class), jb.asBoolean());

        jb = new JSONBoolean(true);
        assertEquals(JSONObjects.toObject(jb, boolean.class), jb.asBoolean());
        assertEquals(JSONObjects.toObject(jb, Boolean.class), jb.asBoolean());
        assertEquals(JSONObjects.toObject(jb, Object.class), jb.asBoolean());
    }

    @Test
    public void testJSONNumberConversions() {
        JSONNumber jn = new JSONNumber(345673);
        assertEquals(JSONObjects.toObject(jn, int.class), jn.asInt());
        assertEquals(JSONObjects.toObject(jn, long.class), jn.asLong());
        assertEquals(JSONObjects.toObject(jn, Object.class), jn.asInt());
        assertEquals(JSONObjects.toObject(jn, Number.class), jn.asInt());
        assertEquals(JSONObjects.toObject(jn, OptionalInt.class), OptionalInt.of(jn.asInt()));

        jn = new JSONNumber(Integer.MAX_VALUE + 1L);
        assertEquals(JSONObjects.toObject(jn, long.class), jn.asLong());
        assertEquals(JSONObjects.toObject(jn, Object.class), jn.asLong());
        assertEquals(JSONObjects.toObject(jn, Number.class), jn.asLong());
        assertEquals(JSONObjects.toObject(jn, OptionalLong.class), OptionalLong.of(jn.asLong()));
    }

    @Test
    public void testJSONDecimalConversions() {
        JSONDecimal jd = new JSONDecimal(Math.E);

        Object obj = JSONObjects.toObject(jd, double.class);
        assertEquals((double)obj, jd.asDouble(), DELTA);
        obj = JSONObjects.toObject(jd, Double.class);
        assertEquals((double)obj, jd.asDouble(), DELTA);
        obj = JSONObjects.toObject(jd, float.class);
        assertEquals((float)obj, jd.asFloat(), (float) DELTA);
        obj = JSONObjects.toObject(jd, Float.class);
        assertEquals((float)obj, jd.asFloat(), (float) DELTA);
        obj = JSONObjects.toObject(jd, OptionalDouble.class);
        assertEquals(((OptionalDouble)obj).getAsDouble(), jd.asDouble(), DELTA);
    }

    @Test
    public void testJSONStringConversions() {
        JSONString js = new JSONString("JAVA");
        Object obj = JSONObjects.toObject(js, String.class);
        assertEquals(obj, js.asString());

        obj = JSONObjects.toObject(js, CharSequence.class);
        assertEquals(obj, js.asString());

        // when target type is Object.class, we still get a String
        obj = JSONObjects.toObject(js, Object.class);
        assertEquals(obj, js.asString());

        // conversion from String length of 1 to char/Character
        js = new JSONString("X");
        obj = JSONObjects.toObject(js, char.class);
        assertEquals(obj, (Character) js.asString().charAt(0));
        obj = JSONObjects.toObject(js, Character.class);
        assertEquals(obj, (Character) js.asString().charAt(0));
        obj = JSONObjects.toObject(js, Object.class);
        assertEquals(obj, (Character) js.asString().charAt(0));

        // to enum constant
        js = new JSONString("RED");
        obj = JSONObjects.toObject(js, Color.class);
        assertEquals(obj, Color.RED);

        // use special parsing functions to convert JSON strings

        js = new JSONString("2024-01-01");
        obj = JSONObjects.toObject(js, LocalDate.class);
        assertEquals(obj, LocalDate.parse(js.asString()));

        js = new JSONString("https://docs.oracle.com");
        obj = JSONObjects.toObject(js, URI.class);
        assertEquals(obj, URI.create(js.asString()));

        js = new JSONString("[_a-zA-Z][_a-zA-Z0-9]*");
        obj = JSONObjects.toObject(js, Pattern.class);
        assertTrue(obj instanceof Pattern);
        try {
            // Can't do equality check on Pattern as Pattern.equals is identity comparison.
            // We convert to string and compare string representation.
            assertEquals(obj.toString(), Pattern.compile(js.asString()).toString());
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    @Test
    public void testJSONNullToOptionals() {
        JSONNull jn = (JSONNull) JSONObjects.from((Object)null);
        assertEquals(JSONObjects.toObject(jn, OptionalInt.class),
            OptionalInt.empty());
        assertEquals(JSONObjects.toObject(jn, OptionalLong.class),
            OptionalLong.empty());
        assertEquals(JSONObjects.toObject(jn, OptionalDouble.class),
            OptionalDouble.empty());
        assertEquals(JSONObjects.toObject(jn, Optional.class),
            Optional.empty());
    }

    @Test
    public void testToRecord() {
        Point pt = new Point(434, -4423);
        JSONObject jo = JSONObjects.from(pt);
        assertEquals(jo.size(), Point.class.getRecordComponents().length);
        assertTrue(jo.get("x").isInt());
        assertTrue(jo.get("y").isInt());
        assertEquals(jo.get("x").asInt(), pt.x());
        assertEquals(jo.get("y").asInt(), pt.y());
        // round trip check
        assertEquals(JSONObjects.toRecord(jo, Point.class), pt);

        // try a complex record
        UserInfo ui = new UserInfo("Batman",
            List.of("224", "Park Drive", "Gotham City"),
            "bruce.wayne@batcave.com",
            Optional.of(List.of("1007", "Mountain Drive", "Gotham")),
            Optional.empty());
        jo = JSONObjects.from(ui);
        assertEquals(jo.size(), UserInfo.class.getRecordComponents().length);
        // round trip check
        assertEquals(JSONObjects.toRecord(jo, UserInfo.class), ui);
    }

    private Type getFirstComponentType(Class<? extends Record> cls) {
        return cls.getRecordComponents()[0].getGenericType();
    }

    @Test
    public void testToMap() {
        Point pt = new Point(4299, 1456);
        JSONObject jo = JSONObjects.from(pt);
        Map<String, Object> expectedMap = Map.of("x", pt.x(), "y", pt.y());

        assertEquals(JSONObjects.toMap(jo), expectedMap);

        // toObject also returns a Map
        assertEquals(JSONObjects.toObject(jo), expectedMap);

        // toObject with Object.class as target also returns a Map
        assertEquals(JSONObjects.toObject(jo, Object.class), expectedMap);

        // Just used only to construct Type for Map<String, Point>
        record DummyRecord(Map<String, Point> namedPoints) {};
        Type pointMapType = getFirstComponentType(DummyRecord.class);

        // test Map with specific value type other than Object
        Map<String, Point> pointMap = Map.of(
             "x", new Point(23, 234),
             "y", new Point(13, -23));
        jo = JSONObjects.from(pointMap);
        // round trip check
        assertEquals(JSONObjects.toObject(jo, pointMapType), pointMap);

        // use proper typed map converter method
        Map<String, Point> convertedPointMap = JSONObjects.toMap(jo, String.class, Point.class);
        assertTrue(convertedPointMap.get("x") instanceof Point);
        assertTrue(convertedPointMap.get("y") instanceof Point);
        assertEquals(convertedPointMap.get("x"), pointMap.get("x"));
        assertEquals(convertedPointMap.get("y"), pointMap.get("y"));
        // round trip check
        assertEquals(convertedPointMap, pointMap);

        // Just used only to construct Type for Map<URI, String>
        record DummyRecord2(Map<URI, String> urls) {};
        Type uriMapType = getFirstComponentType(DummyRecord2.class);
        // test Map with specific value type other than Object
        Map<URI, String> uriMap = Map.of(
             URI.create("https://docs.oracle.com"), "Oracle Help Center",
             URI.create("https://jdk.java.net"), "OpenJDK home");
        jo = JSONObjects.from(uriMap);
        assertEquals(JSONObjects.toObject(jo, uriMapType), uriMap);

        // use proper typed map converter method
        Map<URI, String> convertedURIMap = JSONObjects.toMap(jo, URI.class, String.class);
        for (URI key : uriMap.keySet()) {
            assertTrue(convertedURIMap.get(key) instanceof String);
            assertEquals(convertedURIMap.get(key), uriMap.get(key));
        }
        // round trip check
        assertEquals(convertedURIMap, uriMap);
    }

    @Test
    public void testToEnumMap() {
        EnumMap<Color, List<String>> em = new EnumMap<>(Color.class);
        em.put(Color.RED, List.of("red", "RED", "#ff0000"));
        em.put(Color.GREEN, List.of("green","GREEN", "#00ff00"));
        em.put(Color.BLUE, List.of("blue", "BLUE", "#0000ff"));

        // Just used to construct Type for EnumMap<String, List<String>>
        record DummyRecord(EnumMap<Color, List<String>> colors) {};
        Type enumMapType = getFirstComponentType(DummyRecord.class);

        JSONObject colors = JSONObjects.from(em);
        assertTrue(colors.get("RED").isArray());
        assertEquals(colors.get("RED").asArray().size(),
            em.get(Color.RED).size());
        assertTrue(colors.get("GREEN").isArray());
        assertEquals(colors.get("GREEN").asArray().size(),
            em.get(Color.GREEN).size());
        assertTrue(colors.get("BLUE").isArray());
        assertEquals(colors.get("BLUE").asArray().size(),
            em.get(Color.BLUE).size());

        // round trip check
        assertEquals(JSONObjects.toObject(colors, enumMapType), em);

        EnumMap<Color, String[]> colorMap = new EnumMap<>(Color.class);
        colorMap.put(Color.RED, em.get(Color.RED).toArray(new String[0]));
        colorMap.put(Color.GREEN, em.get(Color.GREEN).toArray(new String[0]));
        colorMap.put(Color.BLUE, em.get(Color.BLUE).toArray(new String[0]));

        colors = JSONObjects.from(colorMap);
        // use proper typed EnumMap converter method
        EnumMap<Color, String[]> convertedMap = JSONObjects.
                toEnumMap(colors, Color.class, String[].class);

        // round trip check
        assertEqualsDeep(convertedMap, colorMap);
    }

    @Test
    public void testSet() {
        Set<Point> ps = Set.of(
            new Point(24, 13), new Point(124, 81), new Point(2, 132));

        // Just used only to construct Type for Set<Point>
        record DummyRecord(Set<Point> points) {};
        Type pointSetType = getFirstComponentType(DummyRecord.class);

        JSONValue jv = JSONObjects.from(ps);
        assertTrue(jv.isArray());
        assertEquals(JSONObjects.toObject(jv, pointSetType), ps);
    }

    @Test
    public void testEnumSet() {
        // Just used to construct Type for EnumSet<Color>
        record DummyRecord(EnumSet<Color> colors) {};
        Type colorSetType = getFirstComponentType(DummyRecord.class);

        EnumSet<Color> cs = EnumSet.noneOf(Color.class);
        JSONValue colors = JSONObjects.from(cs);
        assertTrue(colors.isArray());
        assertEquals(colors.asArray().size(), 0);
        // round trip check
        assertEquals(JSONObjects.toObject(colors, colorSetType), cs);

        cs = EnumSet.of(Color.RED);
        colors = JSONObjects.from(cs);
        assertTrue(colors.isArray());
        assertEquals(colors.asArray().size(), 1);
        assertEquals(colors.get(0).asString(), "RED");
        // round trip check
        assertEquals(JSONObjects.toObject(colors, colorSetType), cs);

        cs = EnumSet.of(Color.RED, Color.GREEN, Color.RED);
        colors = JSONObjects.from(cs);
        assertTrue(colors.isArray());
        // set and so duplicate removed
        assertEquals(colors.asArray().size(), 2);
        assertEquals(colors.get(0).asString(), "RED");
        assertEquals(colors.get(1).asString(), "GREEN");
        // round trip check
        assertEquals(JSONObjects.toObject(colors, colorSetType), cs);

        cs = EnumSet.allOf(Color.class);
        colors = JSONObjects.from(cs);
        assertTrue(colors.isArray());
        assertEquals(colors.asArray().size(), 3);
        assertEquals(colors.get(0).asString(), "RED");
        assertEquals(colors.get(1).asString(), "GREEN");
        assertEquals(colors.get(2).asString(), "BLUE");
        // round trip check
        assertEquals(JSONObjects.toObject(colors, colorSetType), cs);
    }
}
