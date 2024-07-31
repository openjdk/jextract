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
package org.openjdk.jextract.json.parser;

import java.util.stream.Stream;
import java.util.List;

public sealed interface JSONValue permits
        JSONBoolean, JSONNumber, JSONDecimal, JSONString,
        JSONObject, JSONArray, JSONNull  {
    default byte asByte() {
        throw new IllegalStateException("Unsupported conversion to byte");
    }

    default short asShort() {
        throw new IllegalStateException("Unsupported conversion to short");
    }

    default int asInt() {
        throw new IllegalStateException("Unsupported conversion to int");
    }

    default long asLong() {
        throw new IllegalStateException("Unsupported conversion to long");
    }

    default float asFloat() {
        throw new IllegalStateException("Unsupported conversion to float");
    }

    default double asDouble() {
        throw new IllegalStateException("Unsupported conversion to double");
    }

    default String asString() {
        throw new IllegalStateException("Unsupported conversion to String");
    }

    default char asChar() {
        throw new IllegalStateException("Unsupported conversion to char");
    }

    default boolean asBoolean() {
        throw new IllegalStateException("Unsupported conversion to boolean");
    }

    default JSONArray asArray() {
        throw new IllegalStateException("Unsupported conversion to array");
    }

    default JSONObject asObject() {
        throw new IllegalStateException("Unsupported conversion to object");
    }

    default boolean isInt() {
        return false;
    }

    default boolean isLong() {
        return false;
    }

    default boolean isDouble() {
        return false;
    }

    default boolean isString() {
        return false;
    }

    default boolean isBoolean() {
        return false;
    }

    default boolean isArray() {
        return false;
    }

    default boolean isObject() {
        return false;
    }

    default boolean isNull() {
        return false;
    }

    default List<JSONObject.Field> fields() {
        return asObject().fields();
    }

    default boolean contains(String field) {
        return asObject().contains(field);
    }

    default JSONValue get(String field) {
        return asObject().get(field);
    }

    default JSONValue getOrDefault(String field, JSONValue fallback) {
        return asObject().getOrDefault(field, fallback);
    }

    default JSONValue get(int i) {
        return asArray().get(i);
    }

    default Stream<JSONValue> stream() {
        return Stream.of(this);
    }

    static JSONValue from(int i) {
        return new JSONNumber(i);
    }

    static JSONValue from(long l) {
        return new JSONNumber(l);
    }

    static JSONValue from(double d) {
        return new JSONDecimal(d);
    }

    static JSONValue from(boolean b) {
        return new JSONBoolean(b);
    }

    static JSONValue from(String s) {
        return new JSONString(s);
    }

    static JSONValue fromNull() {
        return JSONNull.instance;
    }

    static String toPrettyString(JSONValue value) {
        StringBuilder builder = new StringBuilder();
        toPrettyString(builder, 0, false, value);
        return builder.toString();
    }

    private static void toPrettyString(StringBuilder builder, int level, boolean asField, JSONValue value) {
        if (!asField) {
            indent(builder, level);
        }
        switch (value) {
            case JSONObject obj -> {
                builder.append("{\n");
                List<JSONObject.Field> fields = obj.fields();
                for (int i = 0; i < fields.size(); i++) {
                    JSONObject.Field field = fields.get(i);
                    indent(builder, level + 1).append("\"" + field.name() + "\": ");
                    toPrettyString(builder, level + 1, true, field.value());
                    if (i < fields.size() - 1) {
                        builder.append(",\n");
                    } else {
                        builder.append("\n");
                    }
                }
                indent(builder, level).append("}");
            }
            case JSONArray array -> {
                builder.append("[\n");
                for (int i = 0; i < array.size(); i++) {
                    toPrettyString(builder, level + 1, false, array.get(i));
                    if (i < array.size() - 1) {
                        builder.append(",\n");
                    } else {
                        builder.append("\n");
                    }
                }
                indent(builder, level).append("]");
            }
            case JSONValue v -> builder.append(v);
        }
    }

    private static StringBuilder indent(StringBuilder builder, int level) {
        builder.append("  ".repeat(level));
        return builder;
    }
}
