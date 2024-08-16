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
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.RecordComponent;
import java.lang.reflect.Type;
import java.io.File;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.OptionalLong;
import java.util.OptionalDouble;
import java.util.Set;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.openjdk.jextract.json.parser.JSONArray;
import org.openjdk.jextract.json.parser.JSONBoolean;
import org.openjdk.jextract.json.parser.JSONDecimal;
import org.openjdk.jextract.json.parser.JSONNull;
import org.openjdk.jextract.json.parser.JSONNumber;
import org.openjdk.jextract.json.parser.JSONObject;
import org.openjdk.jextract.json.parser.JSONString;
import org.openjdk.jextract.json.parser.JSONValue;

/**
 * Helper class that has static utilities to convert between JSONValue
 * and Java records/maps/enummaps. RuntimeException is thrown for
 * conversion failures.
 */
public final class JSONObjects {
    private JSONObjects() {}

    /**
     * Converts the given object as a JSONValue.
     *
     * @param value the object being converted.
     * @return converted JSONValue.
     */
    public static JSONValue from(Object value) {
        return switch (value) {
            case JSONValue jv -> jv;
            case Boolean b -> JSONValue.from((boolean)b);
            case Character c -> JSONValue.from(c.toString());
            case Byte bi -> JSONValue.from((int)bi);
            case Short s -> JSONValue.from((int)s);
            case Integer i -> JSONValue.from((int)i);
            case Long l -> JSONValue.from((long)l);
            case Float f -> JSONValue.from((double)f);
            case Double d -> JSONValue.from((double)d);
            case String s -> JSONValue.from(s);
            case OptionalInt oi -> from(oi.isEmpty() ? null : oi.getAsInt());
            case OptionalLong ol -> from(ol.isEmpty() ? null : ol.getAsLong());
            case OptionalDouble od -> from(od.isEmpty() ? null : od.getAsDouble());
            case Optional opt -> from(opt.isEmpty() ? null : opt.get());
            case Enum<?> en -> JSONValue.from(en.name());
            case Record r -> from(r);
            case Map<?,?> m -> from(m);
            case Iterable<?> iterable -> JSONArrays.from(iterable);
            case Iterator<?> iterator -> JSONArrays.from(iterator);
            case boolean[] boa -> JSONArrays.from(boa);
            case char[] ca -> JSONArrays.from(ca);
            case byte[] bia -> JSONArrays.from(bia);
            case short[] sa -> JSONArrays.from(sa);
            case int[] ia -> JSONArrays.from(ia);
            case long[] la -> JSONArrays.from(la);
            case float[] fa -> JSONArrays.from(fa);
            case double[] da -> JSONArrays.from(da);
            case String[] sa -> JSONArrays.from(sa);
            case Object[] oa -> JSONArrays.from(oa);
            case null -> JSONValue.fromNull();
            default -> JSONValue.from(Objects.toString(value));
        };
    }

    /**
     * Converts the given record as a JSONObject.
     *
     * @param record the record being converted.
     * @return converted JSONObject.
     */
    public static JSONObject from(Record record) {
        Objects.requireNonNull(record);

        Class<? extends Record> cls = record.getClass();
        Map<String, JSONValue> props = new LinkedHashMap<>();
        for (RecordComponent rc : cls.getRecordComponents()) {
            props.put(rc.getName(), getRecordComponentValue(record, rc));
        }

        return new JSONObject(props);
    }

    /**
     * Converts the given Map as a JSONObject.
     *
     * @param map the Map being converted.
     * @return converted JSONObject.
     */
    public static JSONObject from(Map<?,?> map) {
        Objects.requireNonNull(map);

        JSONObject jo = new JSONObject();
        if (map instanceof EnumMap) {
            for (Map.Entry<?,?> e : map.entrySet()) {
                jo.put(((Enum)e.getKey()).name(),
                       from(e.getValue()));
            }
        } else {
            for (Map.Entry<?,?> e : map.entrySet()) {
                jo.put(Objects.toString(e.getKey()),
                       from(e.getValue()));
            }
        }

        return jo;
    }

    /**
     * Converts the given JSONValue as a convenient object.
     *
     * @param jsonValue the JSONValue being converted.
     * @param targetTyoe the target type for conversion.
     * @return converted convenient object.
     */
    public static Object toObject(JSONValue jsonValue, Type targetType) {
        Objects.requireNonNull(jsonValue);
        Objects.requireNonNull(targetType);

        if (jsonValue.getClass() == targetType) {
            return jsonValue;
        }

        boolean isOptional = false;
        if (targetType instanceof ParameterizedType pt &&
            pt.getRawType() == Optional.class) {
            // Change the targetType to the underlying Optional value type.
            // We'll wrap the value to be Optional later.
            targetType = getFirstTypeArgument(pt);
            isOptional = true;
        } else if (targetType == Optional.class) {
            // raw Optional type!
            targetType = Object.class;
            isOptional = true;
        }

        Object result = switch (jsonValue) {
            case JSONObject jo -> convertObject(jo, targetType);
            case JSONArray ja -> convertArray(ja, targetType);
            case JSONNull jn -> convertNull(jn, targetType);
            case JSONBoolean jb -> convertBoolean(jb, targetType);
            case JSONNumber jn -> convertNumber(jn, targetType);
            case JSONDecimal jd -> convertDecimal(jd, targetType);
            case JSONString js -> convertString(js, targetType);
        };

        // wrap the return value if the target is an Optional type
        return isOptional ? Optional.ofNullable(result) : result;
    }

    /**
     * Converts the given JSONValue as a convenient object.
     *
     * @param jsonValue the JSONValue being converted.
     * @return converted convenient object.
     */
    public static Object toObject(JSONValue jsonValue) {
        return toObject(jsonValue, Object.class);
    }

    /**
     * Converts the given JSONObject as a record.
     *
     * @param jsonObj the JSONValue being converted.
     * @param cls the target record Class type.
     * @return converted record.
     */
    public static <T extends Record> T toRecord(JSONObject jsonObj, Class<T> cls) {
        Objects.requireNonNull(jsonObj);
        Objects.requireNonNull(cls);

        Constructor<? extends Record> cons = getRecordConstructor(cls);
        Object[] params = Arrays.stream(cls.getRecordComponents()).
              map(rc -> {
                  String rcName = rc.getName();
                  Type rcType = rc.getGenericType();
                  if (jsonObj.contains(rcName)) {
                     return toObject(jsonObj.get(rcName), rcType);
                  } else {
                     // It is okay to miss an optional record component.
                     // Return the corresponding Optional empty value
                     // for the missing properties.
                     if (rcType == OptionalInt.class) {
                         return OptionalInt.empty();
                     } else if (rcType == OptionalLong.class) {
                         return OptionalLong.empty();
                     } else if (rcType == OptionalDouble.class) {
                         return OptionalDouble.empty();
                     } else if (isOptionalType(rcType)) {
                         return Optional.empty();
                     } else {
                         // non-Optional property. report missing property value error.
                         throw new RuntimeException("cannot find value for property: " + rcName);
                     }
                  }
              }).
              toArray();
        try {
            return cls.cast(cons.newInstance(params));
        } catch (ReflectiveOperationException roe) {
            throw new RuntimeException(roe);
        }
    }

    /**
     * Converts the given JSONObject as a Map.
     *
     * @param jsonObj the JSONValue being converted.
     * @return converted Map.
     */
    public static Map<String, Object> toMap(JSONObject jsonObj) {
        return toMap(jsonObj, String.class, Object.class);
    }

    /**
     * Converts the given JSONObject as a Map.
     *
     * @param jsonObj the JSONValue being converted.
     * @param keyType the target key type for the Map.
     * @param valueType the value key type for the Map.
     * @return converted Map.
     */
    @SuppressWarnings("unchecked")
    public static <K,V> Map<K, V> toMap(JSONObject jsonObj, Class<K> keyType, Class<V> valueType) {
        Objects.requireNonNull(jsonObj);
        Objects.requireNonNull(keyType);
        Objects.requireNonNull(valueType);
        return (Map<K, V>) convertToMap(jsonObj, keyType, valueType);
    }

    /**
     * Converts the given JSONObject as a EnumMap.
     *
     * @param jsonObj the JSONValue being converted.
     * @param enumCls the target enum key Class for the Map.
     * @param valueType the value key type for the map.
     */
    @SuppressWarnings("unchecked")
    public static <K extends Enum<K>, V> EnumMap<K, V> toEnumMap(JSONObject jsonObj,
                Class<K> enumCls, Class<V> valueType) {
        Objects.requireNonNull(jsonObj);
        Objects.requireNonNull(enumCls);
        Objects.requireNonNull(valueType);

        return (EnumMap<K, V>) convertToEnumMap(jsonObj, enumCls, valueType);
    }

    // package private helpers for JSONArrays
    @SuppressWarnings("unchecked")
    static <T> List<T> toList(JSONArray ja, Class<T> elemType) {
        return (List<T>) convertToList(ja, elemType);
    }

    @SuppressWarnings("unchecked")
    static <T> Set<T> toSet(JSONArray ja, Class<T> elemType) {
        return (Set<T>) convertToSet(ja, elemType);
    }

    @SuppressWarnings("unchecked")
    static <T extends Enum<T>> EnumSet<T> toEnumSet(JSONArray ja, Class<T> enumCls) {
        return (EnumSet<T>) convertToEnumSet(ja, enumCls);
    }

    // Internals only below this point

    // Type helpers

    // is the given Type Optional raw type or Optional<T>?
    private static boolean isOptionalType(Type type) {
        return type == Optional.class ||
            (type instanceof ParameterizedType pt &&
            pt.getRawType() == Optional.class);
    }

    // is this a List/ArrayList/Iterable?
    private static boolean isListOrIterable(Class<?> cls) {
        return cls == List.class || cls == ArrayList.class || cls == Iterable.class;
    }

    // is this a Set/HashSet/LinkedHashSet?
    private static boolean isSet(Class<?> cls) {
        return cls == Set.class || cls == HashSet.class || cls == LinkedHashSet.class;
    }

    // is this a Map/HashMap/LinkedHashMap?
    private static boolean isMap(Class<?> cls) {
        return cls == Map.class || cls == HashMap.class || cls == LinkedHashMap.class;
    }

    // is this String or CharSequence class?
    private static boolean isStringOrCharSequence(Type type) {
        return type == String.class || type == CharSequence.class;
    }

    // The ParamerizedType is expected to have only one type argument at most
    private static Type getFirstTypeArgument(ParameterizedType pt) {
        return getNthTypeArgument(pt, 0);
    }

    private static Type getSecondTypeArgument(ParameterizedType pt) {
        return getNthTypeArgument(pt, 1);
    }

    private static Type getNthTypeArgument(ParameterizedType pt, int n) {
        Type[] typeArgs = pt.getActualTypeArguments();
        return typeArgs.length > n ? typeArgs[n] : Object.class;
    }

    // record helpers

    // get the record's canonical constructor
    private static Constructor<? extends Record> getRecordConstructor(Class<? extends Record> cls) {
        Class<?>[] paramTypes = Arrays.stream(cls.getRecordComponents()).
            map(RecordComponent::getType).
            toArray(Class<?>[]::new);

        try {
            return cls.getConstructor(paramTypes);
        } catch (ReflectiveOperationException roe) {
            throw new RuntimeException("cannot find canonical constructor", roe);
        }
    }

    // get the Record component value as a JSONValue
    private static JSONValue getRecordComponentValue(Record record, RecordComponent rc) {
        Object value = null;
        try {
            value = rc.getAccessor().invoke(record);
        } catch (ReflectiveOperationException roe) {
            throw new RuntimeException(roe);
        }
        return from(value);
    }

    // JSONValue to convenient Java objects conversion helpers

    // create cannot convert exception
    private static RuntimeException cannotConvert(JSONValue jv, Type targetType) {
        return new RuntimeException("cannot convert " + jv + " to " + targetType.getTypeName());
    }

    // JSONObject converter helpers

    // exists only localized SuppressWarnings
    @SuppressWarnings("unchecked")
    private static Record convertToRecord(JSONObject jo, Class<?> cls) {
        assert cls.isRecord();
        return toRecord(jo, (Class<? extends Record>)cls);
    }

    private static Map<?,?> convertToMap(JSONObject jo, Type keyType, Type valueType) {
        if (isStringOrCharSequence(keyType)) {
            return jo.fields().stream().
                collect(Collectors.toMap(JSONObject.Field::name,
                    field -> toObject(field.value(), valueType),
                    (e1, e2) -> e1, LinkedHashMap::new));
        } else {
            return jo.fields().stream().
                collect(Collectors.toMap(
                    field -> toObject(JSONValue.from(field.name()), keyType),
                    field -> toObject(field.value(), valueType),
                    (e1, e2) -> e1, LinkedHashMap::new));
        }
    }

    private static EnumMap convertToEnumMap(JSONObject jo, ParameterizedType pt) {
        assert pt.getRawType() == EnumMap.class;
        Type keyType = getFirstTypeArgument(pt);
        if (keyType instanceof Class enumCls && enumCls.isEnum()) {
            Type valueType = getSecondTypeArgument(pt);
            return convertToEnumMap(jo, enumCls, valueType);
        }

        throw cannotConvert(jo, pt);
    }

    @SuppressWarnings("unchecked")
    private static EnumMap convertToEnumMap(JSONObject jo, Class<?> enumCls, Type valueType) {
        EnumMap enumMap = new EnumMap(enumCls);
        jo.fields().forEach(field -> {
            enumMap.put(convertToEnum(field.name(), enumCls),
                toObject(field.value(), valueType));
        });
        return enumMap;
    }

    private static Object convertObject(JSONObject jo, Type targetType) {
        if (targetType instanceof Class cls) {
            if (cls.isRecord()) {
                return convertToRecord(jo, cls);
            } else if (cls == Object.class || isMap(cls)) {
                // convert as Map<String, Object>
                return convertToMap(jo, String.class, Object.class);
            }
        } else if (targetType instanceof ParameterizedType pt) {
            if (pt.getRawType() instanceof Class cls) {
                if (cls == EnumMap.class) {
                    // EnumMap<K, V>
                    return convertToEnumMap(jo, pt);
                } else if (isMap(cls)) {
                    // Map<K, V>
                    Type keyType = getFirstTypeArgument(pt);
                    Type valueType = getSecondTypeArgument(pt);
                    return convertToMap(jo, keyType, valueType);
                }
            }
        }

        throw cannotConvert(jo, targetType);
    }

    // JSONArray converter helpers
    private static final Map<Class<?>, Function<JSONArray, Object>> arrayConverters =
        new IdentityHashMap<>();

    private static void addArrayConverter(Class<?> cls, Function<JSONArray, Object> func) {
        arrayConverters.put(cls, func);
    }

    static {
        addArrayConverter(boolean[].class, JSONArrays::toBooleanArray);
        addArrayConverter(char[].class, JSONArrays::toCharArray);
        addArrayConverter(byte[].class, JSONArrays::toByteArray);
        addArrayConverter(int[].class, JSONArrays::toIntArray);
        addArrayConverter(long[].class, JSONArrays::toLongArray);
        addArrayConverter(float[].class, JSONArrays::toFloatArray);
        addArrayConverter(double[].class, JSONArrays::toDoubleArray);
        addArrayConverter(Object[].class, JSONArrays::toObjectArray);
        addArrayConverter(String[].class, JSONArrays::toStringArray);

        // for Object.class too, we return an Object[]
        addArrayConverter(Object.class, JSONArrays::toObjectArray);

        // convert to List cases
        Function<JSONArray, Object> listFunc = jo -> convertToList(jo, Object.class);
        addArrayConverter(List.class, listFunc);
        addArrayConverter(ArrayList.class, listFunc);
        addArrayConverter(Iterable.class, listFunc);

        // convert to Map cases
        Function<JSONArray, Object> setFunc = jo -> convertToSet(jo, Object.class);
        addArrayConverter(Set.class, setFunc);
        addArrayConverter(HashSet.class, setFunc);
        addArrayConverter(LinkedHashSet.class, setFunc);
    }

    // convert the given JSONArray to a Java object of the given Type

    private static EnumSet<?> convertToEnumSet(JSONArray ja, ParameterizedType pt) {
        assert pt.getRawType() == EnumSet.class;
        Type compType = getFirstTypeArgument(pt);
        if (compType instanceof Class enumCls && enumCls.isEnum()) {
            return convertToEnumSet(ja, enumCls);
        } else {
            throw cannotConvert(ja, pt);
        }
    }

    @SuppressWarnings("unchecked")
    private static EnumSet convertToEnumSet(JSONArray ja, Class enumCls) {
        final int length = ja.size();
        return switch (length) {
            case 0 -> EnumSet.noneOf(enumCls);
            case 1 -> EnumSet.of(convertToEnum(ja.get(0).asString(), enumCls));
            default -> {
                Enum first = convertToEnum(ja.get(0).asString(), enumCls);
                Enum[] rest = new Enum[length - 1];
                for (int i = 1; i < length; i++) {
                    rest[i - 1] = convertToEnum(ja.get(i).asString(), enumCls);
                }
                yield EnumSet.of(first, rest);
            }
        };
    }

    private static List<?> convertToList(JSONArray ja, Type compType) {
        List<Object> list = new ArrayList<>();
        final int length = ja.size();
        for (int i = 0; i < length; i++) {
             list.add(toObject(ja.get(i), compType));
        }
        return list;
    }

    private static Set<?> convertToSet(JSONArray ja, Type compType) {
        Set<Object> set = new LinkedHashSet<>();
        final int length = ja.size();
        for (int i = 0; i < length; i++) {
             set.add(toObject(ja.get(i), compType));
        }
        return set;
    }

    private static Object convertArray(JSONArray ja, Type targetType) {
        if (targetType instanceof Class cls) {
            var func = arrayConverters.get(cls);
            if (func != null) {
                return func.apply(ja);
            } else if (cls.isArray()) {
                // T[] for some type T
                Class<?> elemCls = cls.getComponentType();
                final int length = ja.size();
                Object arr = Array.newInstance(elemCls, length);
                for (int i = 0; i < length; i++) {
                    Array.set(arr, i, JSONObjects.toObject(ja.get(i), elemCls));
                }
                return arr;
            }
        } else if (targetType instanceof ParameterizedType pt) {
            if (pt.getRawType() instanceof Class cls) {
                // List<T>, ArrayList<T> or Iterable<T>
                if (isListOrIterable(cls)) {
                    Type compType = getFirstTypeArgument(pt);
                    return convertToList(ja, compType);
                // Set<T>, HashMap<T> or LinkedHashMap<T>
                } else if (isSet(cls)) {
                    Type compType = getFirstTypeArgument(pt);
                    return convertToSet(ja, compType);
                // EnumSet<T>
                } else if (cls == EnumSet.class) {
                    return convertToEnumSet(ja, pt);
                }
            }
        }

        throw cannotConvert(ja, targetType);
    }

    // convert JSONNull value to a Java oject of the given Type
    private static Object convertNull(JSONNull jn, Type targetType) {
        if (targetType instanceof Class cls && cls.isPrimitive()) {
            throw cannotConvert(jn, targetType);
        } else if (targetType == OptionalInt.class) {
            return OptionalInt.empty();
        } else if (targetType == OptionalLong.class) {
            return OptionalLong.empty();
        } else if (targetType == OptionalDouble.class) {
            return OptionalDouble.empty();
        } else {
            return null;
        }
    }

    // convert the JSONBoolean value to a Java object of the given Type
    private static Object convertBoolean(JSONBoolean jb, Type targetType) {
        if (targetType == boolean.class ||
            targetType == Boolean.class ||
            targetType == Object.class) {
            return jb.asBoolean();
        } else {
            throw cannotConvert(jb, targetType);
        }
    }

    private static final Map<Class<?>, Function<JSONNumber, Object>> numberConverters =
        new IdentityHashMap<>();

    private static void addNumberConverter(Class<?> cls, Function<JSONNumber, Object> func) {
        numberConverters.put(cls, func);
    }

    static {
        addNumberConverter(byte.class, JSONNumber::asByte);
        addNumberConverter(Byte.class, JSONNumber::asByte);
        addNumberConverter(short.class, JSONNumber::asShort);
        addNumberConverter(Short.class, JSONNumber::asShort);
        addNumberConverter(int.class, JSONNumber::asInt);
        addNumberConverter(Integer.class, JSONNumber::asInt);
        addNumberConverter(long.class, JSONNumber::asLong);
        addNumberConverter(Long.class, JSONNumber::asLong);
        addNumberConverter(OptionalInt.class, jn -> OptionalInt.of(jn.asInt()));
        addNumberConverter(OptionalLong.class, jn -> OptionalLong.of(jn.asLong()));
        Function<JSONNumber, Object> intOrLong = jn -> {
            try {
                 return jn.asInt();
            } catch (ArithmeticException ae) {
                 return jn.asLong();
            }
        };
        addNumberConverter(Number.class, intOrLong);
        addNumberConverter(Object.class, intOrLong);
    }

    // convert the JSONNumber value to a Java object of the given Type
    private static Object convertNumber(JSONNumber jn, Type targetType) {
        var func = numberConverters.get(targetType);
        if (func != null) {
            return func.apply(jn);
        } else {
            throw cannotConvert(jn, targetType);
        }
    }

    private static final Map<Class<?>, Function<JSONDecimal, Object>> decimalConverters =
        new IdentityHashMap<>();

    private static void addDecimalConverter(Class<?> cls, Function<JSONDecimal, Object> func) {
        decimalConverters.put(cls, func);
    }

    static {
        addDecimalConverter(double.class, JSONDecimal::asDouble);
        addDecimalConverter(Double.class, JSONDecimal::asDouble);
        addDecimalConverter(Number.class, JSONDecimal::asDouble);
        addDecimalConverter(Object.class, JSONDecimal::asDouble);
        addDecimalConverter(float.class, JSONDecimal::asFloat);
        addDecimalConverter(Float.class, JSONDecimal::asFloat);
        addDecimalConverter(OptionalDouble.class, jd -> OptionalDouble.of(jd.asDouble()));
    }

    // convert the JSONDecimal value to a Java object of the given Type
    private static Object convertDecimal(JSONDecimal jd, Type targetType) {
        var func = decimalConverters.get(targetType);
        if (func != null) {
            return func.apply(jd);
        } else {
            throw cannotConvert(jd, targetType);
        }
    }

    // exists only localized SuppressWarnings
    @SuppressWarnings("unchecked")
    private static Enum convertToEnum(String str, Class<?> cls) {
        assert cls.isEnum();
        return Enum.valueOf((Class<? extends Enum>)cls, str);
    }

    // special case String to object parse helpers

    private static File parseFile(String str) {
        return new File(str);
    }

    private static Path parsePath(String str) {
        return Path.of(str);
    }

    private static URL parseURL(String str) {
        try {
            return URI.create(str).toURL();
        } catch (MalformedURLException mue) {
            throw new RuntimeException(mue);
        }
    }

    // String -> Java type parse helper functions cache
    private static final Map<Class<?>, Function<String, Object>> parseFunctions =
        new IdentityHashMap<>();

    private static void addParseFuncToCache(Class<?> cls, Function<String, Object> func) {
        parseFunctions.put(cls, func);
    }

    private static void addParseFuncToCache(Class<?> cls, Method m) {
        parseFunctions.put(cls, methodToFunction(m));
    }

    // wrap a static Method as a Function
    private static Function<String, Object> methodToFunction(Method m) {
        Function<String, Object> func = str -> {
            try {
                return m.invoke(null, str);
            } catch (ReflectiveOperationException roe) {
                throw new RuntimeException(roe);
            }
        };
        return func;
    }

    static {
        // special case parse methods
        addParseFuncToCache(File.class, JSONObjects::parseFile);
        addParseFuncToCache(Path.class, JSONObjects::parsePath);
        addParseFuncToCache(URL.class, JSONObjects::parseURL);

        // specific method wrapping cases
        try {
            addParseFuncToCache(Pattern.class, Pattern.class.getMethod("compile", String.class));
            addParseFuncToCache(URI.class, URI.class.getMethod("create", String.class));
        } catch (ReflectiveOperationException roe) {
            throw new RuntimeException(roe);
        }
    }

    private static Optional<Function<String, Object>> findParseFunction(Class<?> cls) {
        // check the cache
        if (parseFunctions.containsKey(cls)) {
            return Optional.of(parseFunctions.get(cls));
        }

        var optMethod = Arrays.stream(cls.getMethods()).
            filter(mth -> Modifier.isStatic(mth.getModifiers()) &&
                   mth.getParameterCount() == 1 &&
                   mth.getName().equals("parse") &&
                   mth.getReturnType() == cls &&
                   isStringOrCharSequence(mth.getParameterTypes()[0])).
            findFirst();

        if (optMethod.isPresent()) {
            var func = methodToFunction(optMethod.get());
            // cache for future - only for bootstrap classes
            if (cls.getClassLoader() == null) {
                addParseFuncToCache(cls, func);
            }
            return Optional.of(func);
        }

        return Optional.empty();
    }

    // convert the JSONString value to a Java object of the given Type
    private static Object convertString(JSONString js, Type targetType) {
        if (isStringOrCharSequence(targetType)) {
            return js.asString();
        } else if (targetType == char.class ||
                   targetType == Character.class) {
            return js.asChar();
        } else if (targetType == Object.class) {
            // return Character or String object depending on the length
            String str = js.asString();
            return str.length() == 1 ? Character.valueOf(str.charAt(0)) : str;
        } else if (targetType instanceof Class cls) {
            if (cls.isEnum()) {
                return convertToEnum(js.asString(), cls);
            } else {
                // check if we can convert the JSON String value to an object
                // of the given target type by a type specific "parse" function.
                var optFunc = findParseFunction(cls);
                if (optFunc.isPresent()) {
                    return optFunc.get().apply(js.asString());
                }
            }
        }

        throw cannotConvert(js, targetType);
    }
}
