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

import java.lang.reflect.Array;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import org.openjdk.jextract.json.parser.JSONArray;
import org.openjdk.jextract.json.parser.JSONValue;

/**
 * Helper class that has static utilities to convert between JSONArrays
 * and Java arrays, iterables, lists, sets and enum sets. RuntimeException
 * is thrown for conversion failures.
 */
public final class JSONArrays {
    private JSONArrays() {}

    /**
     * Converts the given boolean array as a JSONArray.
     *
     * @param ba the boolean array being converted.
     * @return converted JSONArray.
     */
    public static JSONArray from(boolean[] ba) {
        Objects.requireNonNull(ba);

        JSONArray ja = new JSONArray();
        for (boolean b : ba) {
            ja.add(JSONValue.from(b));
        }
        return ja;
    }

    /**
     * Converts the JSONArray as a boolean array.
     *
     * @param ja the JSONArray being converted.
     * @return converted boolean array.
     */
    public static boolean[] toBooleanArray(JSONArray ja) {
        Objects.requireNonNull(ja);

        boolean[] arr = new boolean[ja.size()];
        for (int i = 0; i < arr.length; i++) {
            arr[i] = ja.get(i).asBoolean();
        }
        return arr;
    }

    /**
     * Converts the given char array as a JSONArray.
     *
     * @param ca the char array being converted.
     * @return converted JSONArray.
     */
    public static JSONArray from(char[] ca) {
        Objects.requireNonNull(ca);

        JSONArray ja = new JSONArray();
        for (char c : ca) {
            ja.add(JSONValue.from(String.valueOf(c)));
        }
        return ja;
    }

    /**
     * Converts the JSONArray as a char array.
     *
     * @param ja the JSONArray being converted.
     * @return converted char array.
     */
    public static char[] toCharArray(JSONArray ja) {
        Objects.requireNonNull(ja);

        char[] arr = new char[ja.size()];
        for (int i = 0; i < arr.length; i++) {
            arr[i] = ja.get(i).asChar();
        }
        return arr;
    }

    /**
     * Converts the given byte array as a JSONArray.
     *
     * @param ba the byte array being converted.
     * @return converted JSONArray.
     */
    public static JSONArray from(byte[] ba) {
        Objects.requireNonNull(ba);

        JSONArray ja = new JSONArray();
        for (byte b : ba) {
            ja.add(JSONValue.from((int)b));
        }
        return ja;
    }

    /**
     * Converts the JSONArray as a byte array.
     *
     * @param ja the JSONArray being converted.
     * @return converted byte array.
     */
    public static byte[] toByteArray(JSONArray ja) {
        Objects.requireNonNull(ja);

        byte[] arr = new byte[ja.size()];
        for (int i = 0; i < arr.length; i++) {
            arr[i] = ja.get(i).asByte();
        }
        return arr;
    }

    /**
     * Converts the given short array as a JSONArray.
     *
     * @param sa the short array being converted.
     * @return converted JSONArray.
     */
    public static JSONArray from(short[] sa) {
        Objects.requireNonNull(sa);

        JSONArray ja = new JSONArray();
        for (short s : sa) {
            ja.add(JSONValue.from((int)s));
        }
        return ja;
    }

    /**
     * Converts the JSONArray as a short array.
     *
     * @param ja the JSONArray being converted.
     * @return converted short array.
     */
    public static short[] toShortArray(JSONArray ja) {
        Objects.requireNonNull(ja);

        short[] arr = new short[ja.size()];
        for (int i = 0; i < arr.length; i++) {
            arr[i] = ja.get(i).asShort();
        }
        return arr;
    }

    /**
     * Converts the given int array as a JSONArray.
     *
     * @param ia the int array being converted.
     * @return converted JSONArray.
     */
    public static JSONArray from(int[] ia) {
        Objects.requireNonNull(ia);

        JSONArray ja = new JSONArray();
        for (int i : ia) {
            ja.add(JSONValue.from(i));
        }
        return ja;
    }

    /**
     * Converts the JSONArray as an int array.
     *
     * @param ja the JSONArray being converted.
     * @return converted int array.
     */
    public static int[] toIntArray(JSONArray ja) {
        Objects.requireNonNull(ja);

        int[] arr = new int[ja.size()];
        for (int i = 0; i < arr.length; i++) {
            arr[i] = ja.get(i).asInt();
        }
        return arr;
    }

    /**
     * Converts the given long array as a JSONArray.
     *
     * @param la the long array being converted.
     * @return converted JSONArray.
     */
    public static JSONArray from(long[] la) {
        Objects.requireNonNull(la);

        JSONArray ja = new JSONArray();
        for (long l : la) {
            ja.add(JSONValue.from(l));
        }
        return ja;
    }

    /**
     * Converts the JSONArray as a long array.
     *
     * @param ja the JSONArray being converted.
     * @return converted long array.
     */
    public static long[] toLongArray(JSONArray ja) {
        Objects.requireNonNull(ja);

        long[] arr = new long[ja.size()];
        for (int i = 0; i < arr.length; i++) {
            arr[i] = ja.get(i).asLong();
        }
        return arr;
    }

    /**
     * Converts the given float array as a JSONArray.
     *
     * @param fa the float array being converted.
     * @return converted JSONArray.
     */
    public static JSONArray from(float[] fa) {
        Objects.requireNonNull(fa);

        JSONArray ja = new JSONArray();
        for (float f : fa) {
            ja.add(JSONValue.from((double)f));
        }
        return ja;
    }

    /**
     * Converts the JSONArray as a float array.
     *
     * @param ja the JSONArray being converted.
     * @return converted float array.
     */
    public static float[] toFloatArray(JSONArray ja) {
        Objects.requireNonNull(ja);

        float[] arr = new float[ja.size()];
        for (int i = 0; i < arr.length; i++) {
            arr[i] = ja.get(i).asFloat();
        }
        return arr;
    }

    /**
     * Converts the given double array as a JSONArray.
     *
     * @param da the double array being converted.
     * @return converted JSONArray.
     */
    public static JSONArray from(double[] da) {
        Objects.requireNonNull(da);

        JSONArray ja = new JSONArray();
        for (double d : da) {
            ja.add(JSONValue.from(d));
        }
        return ja;
    }

    /**
     * Converts the JSONArray as a double array.
     *
     * @param ja the JSONArray being converted.
     * @return converted double array.
     */
    public static double[] toDoubleArray(JSONArray ja) {
        Objects.requireNonNull(ja);

        double[] arr = new double[ja.size()];
        for (int i = 0; i < arr.length; i++) {
            arr[i] = ja.get(i).asDouble();
        }
        return arr;
    }

    /**
     * Converts the given String array as a JSONArray.
     *
     * @param sa the String array being converted.
     * @return converted JSONArray.
     */
    public static JSONArray from(String[] sa) {
        Objects.requireNonNull(sa);

        JSONArray ja = new JSONArray();
        for (String s : sa) {
            ja.add(JSONValue.from(s));
        }
        return ja;
    }

    /**
     * Converts the JSONArray as a String array.
     *
     * @param ja the JSONArray being converted.
     * @return converted String array.
     */
    public static String[] toStringArray(JSONArray ja) {
        Objects.requireNonNull(ja);

        String[] arr = new String[ja.size()];
        for (int i = 0; i < arr.length; i++) {
            arr[i] = ja.get(i).asString();
        }
        return arr;
    }

    /**
     * Converts the given Object array as a JSONArray.
     *
     * @param sa the Object array being converted.
     * @return converted JSONArray.
     */
    public static JSONArray from(Object[] array) {
        Objects.requireNonNull(array);

        JSONArray ja = new JSONArray();
        for (Object elem : array) {
            ja.add(JSONObjects.from(elem));
        }
        return ja;
    }

    /**
     * Converts the JSONArray as an Object array.
     *
     * @param ja the JSONArray being converted.
     * @return converted Object array.
     */
    public static Object[] toObjectArray(JSONArray ja) {
        Objects.requireNonNull(ja);

        final int length = ja.size();
        Object[] arr = new Object[length];
        for (int i = 0; i < length; i++) {
             arr[i] = JSONObjects.toObject(ja.get(i));
        }
        return arr;
    }

    /**
     * Converts the given Iterable as a JSONArray.
     *
     * @param iterable the Iterable being converted.
     * @return converted JSONArray.
     */
    public static JSONArray from(Iterable<?> iterable) {
        Objects.requireNonNull(iterable);

        return from(iterable.iterator());
    }

    /**
     * Converts the given Iterator as a JSONArray.
     *
     * @param iterator the Iterator being converted.
     * @return converted JSONArray.
     */
    public static JSONArray from(Iterator<?> iterator) {
        Objects.requireNonNull(iterator);

        JSONArray ja = new JSONArray();
        while (iterator.hasNext()) {
            ja.add(JSONObjects.from(iterator.next()));
        }
        return ja;
    }

    /**
     * Returns an array containing all of the elements in the given JSONArray in
     * proper sequence (from first to last element) after conversion; the runtime
     * type of the returned array is that of the specified array.  If the converted
     * JSONArray fits in the specified array, it is returned therein.  Otherwise, a new
     * array is allocated with the runtime type of the specified array and
     * the size of the given JSONArray.
     *
     * @param ja the JSONArray being converted.
     * @param arr the array into which the elements of the given JSONArray are to
     *          be stored after conversion, if it is big enough; otherwise, a new
     *          array of the same runtime type is allocated for this purpose.
     * @return an array containing the elements of the JSONArray after conversion.
     */
    @SuppressWarnings("unchecked")
    public static <T> T[] toArray(JSONArray ja, T[] arr) {
        Objects.requireNonNull(ja);
        Objects.requireNonNull(arr);

        final int length = ja.size();
        Class<?> compType = arr.getClass().getComponentType();
        if (arr.length < length) {
            // Make a new array of arr's runtime type
            arr = (T[]) Array.newInstance(compType, length);
        }
        for (int i = 0; i < length; i++) {
             arr[i] = (T) JSONObjects.toObject(ja.get(i), compType);
        }
        if (arr.length > length) {
            arr[length] = null;
        }
        return arr;
    }

    /**
     * Returns a list containing all of the elements in the given JSONArray in
     * proper sequence (from first to last element) after conversion.
     *
     * @param ja the JSONArray being converted.
     * @param elemType the target list element type for conversion.
     * @return a list containing the elements of the JSONArray after conversion.
     */
    public static <T> List<T> toList(JSONArray ja, Class<T> elemType) {
        Objects.requireNonNull(ja);
        Objects.requireNonNull(elemType);

        return JSONObjects.toList(ja, elemType);
    }

    /**
     * Returns a set containing all of the elements in the given JSONArray in
     * after conversion with the duplicates removed.
     *
     * @param ja the JSONArray being converted.
     * @param elemType the target set element type for conversion.
     * @return a set containing the elements of the JSONArray after conversion.
     */
    public static <T> Set<T> toSet(JSONArray ja, Class<T> elemType) {
        Objects.requireNonNull(ja);
        Objects.requireNonNull(elemType);

        return JSONObjects.toSet(ja, elemType);
    }

    /**
     * Returns a set containing all of the elements in the given JSONArray in
     * after conversion with the duplicates removed.
     *
     * @param ja the JSONArray being converted.
     * @param enumCls the target EnumSet element enum type for conversion.
     * @return a EnumSet containing the elements of the JSONArray after conversion.
     */
    public static <T extends Enum<T>> EnumSet<T> toEnumSet(JSONArray ja, Class<T> enumCls) {
        Objects.requireNonNull(ja);
        Objects.requireNonNull(enumCls);

        return JSONObjects.toEnumSet(ja, enumCls);
    }
}
