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
package org.openjdk.jextract.json;

import java.util.Objects;

import org.openjdk.jextract.json.parser.JSONObject;
import org.openjdk.jextract.json.parser.JSONParser;
import org.openjdk.jextract.json.parser.JSONValue;
import org.openjdk.jextract.json.parser.JWCC;

/**
 * Simple interface to parse a JSON string to a record and
 * to convert a record to a JSON string.
 */
public final class JSON {
    private JSON() {}

    /**
     * Parse a given String to a Record object of given type.
     *
     * @param str JSON string to parse.
     * @param cls the target record class type.
     * @param extension parse extended JSON format.
     *                  Allows comments and trailing commas.
     * @return parsed and mapped record.
     */
    public static <T extends Record> T parse(String str, Class<T> cls, boolean extension) {
        Objects.requireNonNull(str);
        Objects.requireNonNull(cls);

        JSONValue jsonValue = extension ? JWCC.parse(str) : new JSONParser().parse(str);
        return JSONObjects.toRecord(jsonValue.asObject(), cls);
    }

    /**
     * Parse a given String to a Record object of given type.
     *
     * @param str JSON string to parse.
     * @param cls the target record class type.
     * @return parsed and mapped record.
     */
    public static <T extends Record> T parse(String str, Class<T> cls) {
        return JSON.parse(str, cls, false);
    }

    /**
     * Convert a given record as a JSON String.
     *
     * @param record the record to be converted as JSON String.
     * @param pretty whether to indentation the JSON String or not.
     * @rerurn converted JSON String.
     */
    public static String toString(Record record, boolean pretty) {
        Objects.requireNonNull(record);

        JSONObject jsonObj = JSONObjects.from(record);
        return pretty ? JSONValue.toPrettyString(jsonObj) : jsonObj.toString();
    }

    /**
     * Convert a given record as a JSON String.
     *
     * @param record the record to be converted as JSON String.
     * @rerurn converted JSON String.
     */
    public static String toString(Record record) {
        return toString(record, true);
    }
}
