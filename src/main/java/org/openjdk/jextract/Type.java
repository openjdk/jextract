/*
 *  Copyright (c) 2020, 2023, Oracle and/or its affiliates. All rights reserved.
 *  DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 *  This code is free software; you can redistribute it and/or modify it
 *  under the terms of the GNU General Public License version 2 only, as
 *  published by the Free Software Foundation.  Oracle designates this
 *  particular file as subject to the "Classpath" exception as provided
 *  by Oracle in the LICENSE file that accompanied this code.
 *
 *  This code is distributed in the hope that it will be useful, but WITHOUT
 *  ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 *  FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 *  version 2 for more details (a copy is included in the LICENSE file that
 *  accompanied this code).
 *
 *  You should have received a copy of the GNU General Public License version
 *  2 along with this work; if not, write to the Free Software Foundation,
 *  Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 *   Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 *  or visit www.oracle.com if you need additional information or have any
 *  questions.
 *
 */

package org.openjdk.jextract;

import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.MemoryLayout;
import org.openjdk.jextract.impl.TypeImpl;
import org.openjdk.jextract.impl.TypeImpl.ErronrousTypeImpl;

import java.util.List;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Instances of this class are used to model types in the foreign language.
 * Instances of this class support the <em>visitor</em> pattern (see {@link Type#accept(Type.Visitor, Object)} and
 * {@link Type.Visitor}).
 */
public interface Type {

    /**
     * Is this type the erroneous type?
     * @return true, if this type is the erroneous type.
     */
    boolean isErroneous();

    /**
     * Entry point for visiting type instances.
     * @param visitor the type visitor.
     * @param data optional data to be passed to the visitor.
     * @param <R> the visitor's return type.
     * @param <D> the visitor's argument type.
     * @return the result of visiting this type through the specified type visitor.
     */
    <R,D> R accept(Visitor<R, D> visitor, D data);

    /**
     * Compares the specified object with this Type for equality.  Returns
     * {@code true} if and only if the specified object is also a Type and both
     * the Types are <i>equal</i>.
     *
     * @param o the object to be compared for equality with this Type
     * @return {@code true} if the specified object is equal to this Type
     */
    boolean equals(Object o);

    /**
     * Returns the hash code value for this Type.
     *
     * @return the hash code value for this Type.
     */
    int hashCode();

    /**
     * A primitive type.
     */
    interface Primitive extends Type {

        /**
         * The primitive type kind.
         */
        enum Kind {
            /**
             * {@code void} type.
             */
            Void("void"),
            /**
             * {@code Bool} type.
             */
            Bool("_Bool"),
            /**
             * {@code char} type.
             */
            Char("char"),
            /**
             * {@code char16} type.
             */
            Char16("char16"),
            /**
             * {@code short} type.
             */
            Short("short"),
            /**
             * {@code int} type.
             */
            Int("int"),
            /**
             * {@code long} type.
             */
            Long("long"),
            /**
             * {@code long long} type.
             */
            LongLong("long long"),
            /**
             * {@code int128} type.
             */
            Int128("__int128"),
            /**
             * {@code float} type.
             */
            Float("float"),
            /**
             * {@code double} type.
             */
            Double("double"),
            /**
              * {@code long double} type.
              */
            LongDouble("long double"),
            /**
             * {@code float128} type.
             */
            Float128("float128"),
            /**
             * {@code float16} type.
             */
            HalfFloat("__fp16"),
            /**
             * {@code wchar} type.
             */
            WChar("wchar_t");

            private final String typeName;

            Kind(String typeName) {
                this.typeName = typeName;
            }

            public String typeName() {
                return typeName;
            }
        }

        /**
         * The primitive type kind.
         * @return The primitive type kind.
         */
        Kind kind();
    }

    /**
     * Instances of this class are used to model types which are associated to a declaration in the foreign language
     * (see {@link Declaration}).
     */
    interface Declared extends Type {
        /**
         * The declaration to this type refers to.
         * @return The declaration to this type refers to.
         */
        Declaration.Scoped tree();
    }

    /**
     * A function type.
     */
    interface Function extends Type {
        /**
         * Is this function type a variable-arity?
         * @return true, if this function type is a variable-arity.
         */
        boolean varargs();

        /**
         * The function formal parameter types.
         * @return The function formal parameter types.
         */
        List<Type> argumentTypes();

        /**
         * The function return type.
         * @return The function return type.
         */
        Type returnType();

        /**
         * Names of function parameters (from typedef), if any
         * @return The optional list of function parameter names.
         */
        Optional<List<String>> parameterNames();

        /**
         * Returns a Function type that has the given parameter names.
         *
         * @param paramNames parameter names for this function type.
         * @return new Function type with the given parameter names.
         */
        Function withParameterNames(List<String> paramNames);
    }

    /**
     * An array type. Array types feature an element type and an optional size. As such they can also be used to
     * model array types.
     */
    interface Array extends Type {

        /**
         * The array type kind.
         */
        enum Kind {
            /**
             * Vector kind.
             */
            VECTOR,
            /**
             * Array kind.
             */
            ARRAY,
            /**
             * Incomplete array kind.
             */
            INCOMPLETE_ARRAY;
        }

        /**
         * The array type kind.
         * @return The array type kind.
         */
        Kind kind();

        /**
         * The (optional) array element count.
         * @return The (optional) array element count.
         *
         * @implSpec an element count is present if the array type kind is one of {@link Kind#VECTOR}, {@link Kind#ARRAY}.
         */
        OptionalLong elementCount();

        /**
         * The array type element type.
         * @return The array type element type.
         */
        Type elementType();
    }

    /**
     * A delegated type is used to model a type which contains an indirection to some other underlying type. For instance,
     * a delegated type can be used to model foreign pointers, where the indirection is used to model the pointee type.
     */
    interface Delegated extends Type {

        /**
         * The delegated type kind.
         */
        enum Kind {
            /**
             * Type-defined type.
             */
            TYPEDEF,
            /**
             * Pointer type.
             */
            POINTER,
            /**
             * Signed type.
             */
            SIGNED,
            /**
             * Unsigned type.
             */
            UNSIGNED,
            /**
             * Atomic type.
             */
            ATOMIC,
            /**
             * Volatile type.
             */
            VOLATILE,
            /**
             * Complex type.
             */
            COMPLEX;
        }

        /**
         * The delegated type kind.
         * @return The delegated type kind.
         */
        Kind kind();

        /**
         * The delegated type (optional) name.
         * @return The delegated type (optional) name.
         *
         * @implSpec an element count is present if the array type kind is one of {@link Kind#TYPEDEF}.
         */
        Optional<String> name();

        /**
         * The delegated type underlying type.
         * @return The delegated type underlying type.
         */
        Type type();
    }

    /**
     * Type visitor interface.
     * @param <R> the visitor's return type.
     * @param <P> the visitor's parameter type.
     */
    interface Visitor<R,P> {
        /**
         * Visit a primitive type.
         * @param t the primitive type.
         * @param p the visitor parameter.
         * @return the result of visiting the given primitive type through this visitor object.
         */
        default R visitPrimitive(Primitive t, P p) { return visitType(t, p); }

        /**
         * Visit a function type.
         * @param t the function type.
         * @param p the visitor parameter.
         * @return the result of visiting the given function type through this visitor object.
         */
        default R visitFunction(Function t, P p) { return visitType(t, p); }

        /**
         * Visit a declared type.
         * @param t the declared type.
         * @param p the visitor parameter.
         * @return the result of visiting the given declared type through this visitor object.
         */
        default R visitDeclared(Declared t, P p) { return visitType(t, p); }

        /**
         * Visit a delegated type.
         * @param t the delegated type.
         * @param p the visitor parameter.
         * @return the result of visiting the given delegated type through this visitor object.
         */
        default R visitDelegated(Delegated t, P p) { return visitType(t, p); }

        /**
         * Visit an array type.
         * @param t the array type.
         * @param p the visitor parameter.
         * @return the result of visiting the given array type through this visitor object.
         */
        default R visitArray(Array t, P p) { return visitType(t, p); }

        /**
         * Visit a type.
         * @param t the type.
         * @param p the visitor parameter.
         * @return the result of visiting the given type through this visitor object.
         */
        default R visitType(Type t, P p) { throw new UnsupportedOperationException(); }
    }

    /**
     * Compute the layout for a given type.
     * @param t the type.
     * @return the layout for given type.
     */
    static Optional<MemoryLayout> layoutFor(Type t) {
        return TypeImpl.getLayout(t);
    }

    /**
     * Compute the function descriptor for a given function type.
     * @param function the function type.
     * @return the function descriptor for given function type.
     */
    static Optional<FunctionDescriptor> descriptorFor(Function function) {
        return TypeImpl.getDescriptor(function);
    }

    /**
     * Create the {@code void} type.
     * @return the {@code void} type.
     */
    static Type.Primitive void_() {
        return new TypeImpl.PrimitiveImpl(Type.Primitive.Kind.Void);
    }

    /**
     * Creates a new primitive type given kind.
     * @param kind the primitive type kind.
     * @return a new primitive type with given kind.
     */
    static Type.Primitive primitive(Type.Primitive.Kind kind) {
        return new TypeImpl.PrimitiveImpl(kind);
    }

    /**
     * Creates a new qualified type given kind and underlying type.
     * @param kind the qualified type kind.
     * @param type the qualified type underlying type.
     * @return a new qualified type with given name and underlying type.
     */
    static Type.Delegated qualified(Type.Delegated.Kind kind, Type type) {
        return new TypeImpl.QualifiedImpl(kind, type);
    }

    /**
     * Creates a new typedef type given name and underlying type.
     * @param name the typedef type name.
     * @param aliased the typeef type underlying type.
     * @return a new typedef type with given name and underlying type.
     */
    static Type.Delegated typedef(String name, Type aliased) {
        return new TypeImpl.QualifiedImpl(Delegated.Kind.TYPEDEF, name, aliased);
    }

    /**
     * Creates a new pointer type with no associated pointee information.
     * @return a new pointer type with no associated pointee information.
     */
    static Type.Delegated pointer() {
        return new TypeImpl.PointerImpl(() -> new TypeImpl.PrimitiveImpl(Type.Primitive.Kind.Void));
    }

    /**
     * Creates a new pointer type with given pointee type.
     * @param pointee the pointee type.
     * @return a new pointer type with given pointee type.
     */
    static Type.Delegated pointer(Type pointee) {
        return new TypeImpl.PointerImpl(() -> pointee);
    }

    /**
     * Creates a new pointer type with given pointee type.
     * @param pointee factory to (lazily) build the pointee type.
     * @return a new pointer type with given pointee type (lazily built from factory).
     */
    static Type.Delegated pointer(Supplier<Type> pointee) {
        return new TypeImpl.PointerImpl(pointee);
    }

    /**
     * Creates a new function type with given parameter types and return type.
     * @param varargs is this function type variable-arity?
     * @param returnType the function type return type.
     * @param arguments the function type formal parameter types.
     * @return a new function type with given parameter types and return type.
     */
    static Type.Function function(boolean varargs, Type returnType, Type... arguments) {
        return new TypeImpl.FunctionImpl(varargs, Stream.of(arguments).collect(Collectors.toList()), returnType, null);
    }

    /**
     * Creates a new declared type with given foreign declaration.
     * @param tree the foreign declaration the type refers to.
     * @return  a new declared type with given foreign declaration.
     */
    static Type.Declared declared(Declaration.Scoped tree) {
        return new TypeImpl.DeclaredImpl(tree);
    }

    /**
     * Creates a new vector type with given element count and element type.
     * @param elementCount the vector type element count.
     * @param elementType the vector type element type.
     * @return a new vector type with given element count and element type.
     */
    static Type.Array vector(long elementCount, Type elementType) {
        return new TypeImpl.ArrayImpl(Array.Kind.VECTOR, elementCount, elementType);
    }

    /**
     * Creates a new array type with given element count and element type.
     * @param elementCount the array type element count.
     * @param elementType the array type element type.
     * @return a new array type with given element count and element type.
     */
    static Type.Array array(long elementCount, Type elementType) {
        return new TypeImpl.ArrayImpl(Array.Kind.ARRAY, elementCount, elementType);
    }

    /**
     * Creates a new array type with given element type.
     * @param elementType the array type element type.
     * @return a new array type with given element type.
     */
    static Type.Array array(Type elementType) {
        return new TypeImpl.ArrayImpl(Array.Kind.INCOMPLETE_ARRAY, elementType);
    }

    /**
     * Creates an erroneous type with the given name.
     * @return an erroneous type with the given name.
     */
    static Type error(String erroneousName) {
        return new ErronrousTypeImpl(erroneousName);
    }
}
