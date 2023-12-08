/*
 *  Copyright (c) 2020, Oracle and/or its affiliates. All rights reserved.
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
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.lang.foreign.MemoryLayout;

import org.openjdk.jextract.impl.DeclarationImpl;

/**
 * Instances of this class are used to model declaration elements in the foreign language.
 * All declarations have a position (see {@link Position}) and a name. Instances of this class
 * support the <em>visitor</em> pattern (see {@link Declaration#accept(Visitor, Object)} and
 * {@link Visitor}).
 */
public interface Declaration {

    /**
     * The position associated with this declaration.
     * @return The position associated with this declaration.
     */
    Position pos();

    /**
     * The name associated with this declaration.
     * @return The name associated with this declaration.
     */
    String name();

    /**
     * Entry point for visiting declaration instances.
     * @param visitor the declaration visitor.
     * @param data optional data to be passed to the visitor.
     * @param <R> the visitor's return type.
     * @param <D> the visitor's argument type.
     * @return the result of visiting this declaration through the specified declaration visitor.
     */
    <R,D> R accept(Visitor<R, D> visitor, D data);

    /**
     * Compares the specified object with this Declaration for equality.  Returns
     * {@code true} if and only if the specified object is also a Declaration and both
     * the declarations are <i>equal</i>.
     *
     * @param o the object to be compared for equality with this Declaration
     * @return {@code true} if the specified object is equal to this Declaration
     */
    boolean equals(Object o);

    /**
     * Returns the hash code value for this Declaration.
     *
     * @return the hash code value for this Declaration.
     */
    int hashCode();

    /**
     * {@return the attributes associated with this declaration}
     */
    Collection<Record> attributes();

    /**
     * Obtains an attribute from this declaration.
     * @param attributeClass the class of the attribute to be obtained.
     * @param <R> the attribute's type.
     * @return the attribute (if any).
     */
    <R extends Record> Optional<R> getAttribute(Class<R> attributeClass);

    /**
     * Adds a new attribute to this declaration.
     * @param attribute the attribute to be added.
     * @param <R> the attribute's type.
     */
    <R extends Record> void addAttribute(R attribute);

    /**
     * A function declaration.
     */
    interface Function extends Declaration {
        /**
         * The parameters associated with this function declaration.
         * @return The parameters associated with this function declaration.
         */
        List<Variable> parameters();

        /**
         * The foreign type associated with this function declaration.
         * @return The foreign type associated with this function declaration.
         */
        Type.Function type();
    }

    /**
     * A scoped declaration is a container for one or more nested declarations. This declaration can be used to model
     * several constructs in the foreign languages, such as (but not limited to) structs, unions and structs (see also
     * {@link Scoped.Kind}).
     */
    interface Scoped extends Declaration {

        /**
         * The scoped declaration kind.
         */
        enum Kind {
            /**
             * Class declaration.
             */
            CLASS,
            /**
             * Enum declaration.
             */
            ENUM,
            /**
             * Struct declaration.
             */
            STRUCT,
            /**
             * Union declaration.
             */
            UNION,
            /**
             * Bitfields declaration.
             */
            BITFIELDS,
            /**
             * Toplevel declaration.
             */
            TOPLEVEL;
        }

        /**
         * The member declarations associated with this scoped declaration.
         * @return The member declarations associated with this scoped declaration.
         */
        List<Declaration> members();

        /**
         * The scoped declaration kind.
         * @return The scoped declaration kind.
         */
        Kind kind();
    }

    /**
     * A typedef declaration
     */
    interface Typedef extends Declaration {
        /**
         * The canonical type associated with this typedef declaration.
         * @return The canonical type associated with this typedef declaration.
         */
        Type type();
    }

    /**
     * A variable declaration.
     */
    interface Variable extends Declaration {
        /**
         * The variable declaration kind.
         */
        enum Kind {
            /**
             * Global variable declaration.
             */
            GLOBAL,
            /**
             * Field declaration.
             */
            FIELD,
            /**
             * Bitfield declaration.
             */
            BITFIELD,
            /**
             * Function parameter declaration.
             */
            PARAMETER;
        }

        /**
         * The type associated with this variable declaration.
         * @return The type associated with this variable declaration.
         */
        Type type();

        /**
         * The kind associated with this variable declaration.
         * @return The kind associated with this variable declaration.
         */
        Kind kind();
    }

    /**
     * A bitfield declaration. Same as a variable declaration, but doesn't have a layout. Instead, it has
     * an offset (relative to the enclosing container) and a width.
     */
    interface Bitfield extends Variable {

        /**
         * {@return The bitfield width (in bits)}
         */
        long width();
    }

    /**
     * A constant value declaration.
     */
    interface Constant extends Declaration {
        /**
         * The value associated with this constant declaration.
         * @return The value associated with this constant declaration.
         */
        Object value();

        /**
         * The type associated with this constant declaration.
         * @return The type associated with this constant declaration.
         */
        Type type();
    }

    /**
     * Declaration visitor interface.
     * @param <R> the visitor's return type.
     * @param <P> the visitor's parameter type.
     */
    interface Visitor<R,P> {
        /**
         * Visit a scoped declaration.
         * @param d the scoped declaration.
         * @param p the visitor parameter.
         * @return the result of visiting the given scoped declaration through this visitor object.
         */
        default R visitScoped(Scoped d, P p) { return visitDeclaration(d, p); }

        /**
         * Visit a function declaration.
         * @param d the function declaration.
         * @param p the visitor parameter.
         * @return the result of visiting the given function declaration through this visitor object.
         */
        default R visitFunction(Function d, P p) { return visitDeclaration(d, p); }

        /**
         * Visit a variable declaration.
         * @param d the variable declaration.
         * @param p the visitor parameter.
         * @return the result of visiting the given variable declaration through this visitor object.
         */
        default R visitVariable(Variable d, P p) { return visitDeclaration(d, p); }

        /**
         * Visit a constant declaration.
         * @param d the constant declaration.
         * @param p the visitor parameter.
         * @return the result of visiting the given constant declaration through this visitor object.
         */
        default R visitConstant(Constant d, P p) { return visitDeclaration(d, p); }

        /**
         * Visit a typedef declaration.
         * @param d the typedef declaration.
         * @param p the visitor parameter.
         * @return the result of visiting the given typedef declaration through this visitor object.
         */
        default R visitTypedef(Typedef d, P p) { return visitDeclaration(d, p); }

        /**
         * Visit a declaration.
         * @param d the declaration.
         * @param p the visitor parameter.
         * @return the result of visiting the given declaration through this visitor object.
         */
        default R visitDeclaration(Declaration d, P p) { throw new UnsupportedOperationException(); }
    }

    /**
     * Creates a new constant declaration with given name and type.
     * @param pos the constant declaration position.
     * @param name the constant declaration name.
     * @param value the constant declaration value.
     * @param type the constant declaration type.
     * @return a new constant declaration with given name and type.
     */
    static Declaration.Constant constant(Position pos, String name, Object value, Type type) {
        return new DeclarationImpl.ConstantImpl(type, value, name, pos);
    }

    /**
     * Creates a new global variable declaration with given name and type.
     * @param pos the global variable declaration position.
     * @param name the global variable declaration name.
     * @param type the global variable declaration type.
     * @return a new global variable declaration with given name and type.
     */
    static Declaration.Variable globalVariable(Position pos, String name, Type type) {
        return new DeclarationImpl.VariableImpl(type, Declaration.Variable.Kind.GLOBAL, name, pos);
    }

    /**
     * Creates a new field declaration with given name and type.
     * @param pos the field declaration position.
     * @param name the field declaration name.
     * @param type the field declaration type.
     * @return a new field declaration with given name and type.
     */
    static Declaration.Variable field(Position pos, String name, Type type) {
        return new DeclarationImpl.VariableImpl(type, Declaration.Variable.Kind.FIELD, name, pos);
    }

    /**
     * Creates a new bitfield declaration with given name, type, offset and width.
     * @param pos the bitfield declaration position.
     * @param name the bitfield declaration name.
     * @param type the bitfield declaration type.
     * @return a new bitfield declaration with given name, type and layout.
     */
    static Declaration.Variable bitfield(Position pos, String name, long width, Type type) {
        return new DeclarationImpl.BitfieldImpl(type, width, name, pos);
    }

    /**
     * Creates a new parameter declaration with given name and type.
     * @param pos the parameter declaration position.
     * @param name the parameter declaration name.
     * @param type the parameter declaration type.
     * @return a new parameter declaration with given name and type.
     */
    static Declaration.Variable parameter(Position pos, String name, Type type) {
        return new DeclarationImpl.VariableImpl(type, Declaration.Variable.Kind.PARAMETER, name, pos);
    }

    /**
     * Creates a new variable declaration with given kind, name and type.
     * @param kind the variable declaration kind.
     * @param pos the variable declaration position.
     * @param name the variable declaration name.
     * @param type the variable declaration type.
     * @return a new variable declaration with given kind, name and type.
     */
    static Declaration.Variable var(Variable.Kind kind, Position pos, String name, Type type) {
        return new DeclarationImpl.VariableImpl(type, kind, name, pos);
    }

    /**
     * Creates a new toplevel declaration with given member declarations.
     * @param pos the toplevel declaration position.
     * @param decls the toplevel declaration member declarations.
     * @return a new toplevel declaration with given member declarations.
     */
    static Declaration.Scoped toplevel(Position pos, Declaration... decls) {
        List<Declaration> declList = List.of(decls);
        return new DeclarationImpl.ScopedImpl(Declaration.Scoped.Kind.TOPLEVEL, declList, "<toplevel>", pos);
    }

    /**
     * Creates a new bitfields group declaration with given name and layout.
     * @param pos the bitfields group declaration position.
     * @param bitfields the bitfields group member declarations.
     * @return a new bitfields group declaration with given name and layout.
     */
    static Declaration.Scoped bitfields(Position pos, Declaration.Variable... bitfields) {
        List<Declaration> declList = List.of(bitfields);
        return new DeclarationImpl.ScopedImpl(Declaration.Scoped.Kind.BITFIELDS, declList, "", pos);
    }

    /**
     * Creates a new struct declaration with given name and member declarations.
     * @param pos the struct declaration position.
     * @param name the struct declaration name.
     * @param decls the struct declaration member declarations.
     * @return a new struct declaration with given name, layout and member declarations.
     */
    static Declaration.Scoped struct(Position pos, String name, Declaration... decls) {
        List<Declaration> declList = List.of(decls);
        return new DeclarationImpl.ScopedImpl(Declaration.Scoped.Kind.STRUCT, declList, name, pos);
    }

    /**
     * Creates a new union declaration with given name and member declarations.
     * @param pos the union declaration position.
     * @param name the union declaration name.
     * @param decls the union declaration member declarations.
     * @return a new union declaration with given name and member declarations.
     */
    static Declaration.Scoped union(Position pos, String name, Declaration... decls) {
        List<Declaration> declList = List.of(decls);
        return new DeclarationImpl.ScopedImpl(Scoped.Kind.UNION, declList, name, pos);
    }

    /**
     * Creates a new enum declaration with given name and member declarations.
     * @param pos the enum declaration position.
     * @param name the enum declaration name.
     * @param decls the enum declaration member declarations.
     * @return a new enum declaration with given name, layout and member declarations.
     */
    static Declaration.Scoped enum_(Position pos, String name, Declaration... decls) {
        List<Declaration> declList = List.of(decls);
        return new DeclarationImpl.ScopedImpl(Declaration.Scoped.Kind.ENUM, declList, name, pos);
    }

    /**
     * Creates a new scoped declaration with given kind, name and member declarations.
     * @param kind the kind of the scoped declaration.
     * @param pos the scoped declaration position.
     * @param name the scoped declaration name.
     * @param decls the scoped declaration member declarations.
     * @return a new scoped declaration with given kind, name, layout and member declarations.
     */
    static Declaration.Scoped scoped(Scoped.Kind kind, Position pos, String name, Declaration... decls) {
        List<Declaration> declList = List.of(decls);
        return new DeclarationImpl.ScopedImpl(kind, declList, name, pos);
    }

    /**
     * Creates a new function declaration with given name, type and parameter declarations.
     * @param pos the function declaration position.
     * @param name the function declaration name.
     * @param type the function declaration type.
     * @param params the function declaration parameter declarations.
     * @return a new function declaration with given name, type and parameter declarations.
     */
    static Declaration.Function function(Position pos, String name, Type.Function type, Declaration.Variable... params) {
        List<Variable> paramList = List.of(params);
        return new DeclarationImpl.FunctionImpl(type, paramList, name, pos);
    }

    /**
     * Creates a new typedef declaration with given name and declared type.
     * @param pos the typedef declaration position.
     * @param name the typedef declaration name.
     * @param type the typedef type
     * @return a new type declaration with given name and declared type.
     */
    static Declaration.Typedef typedef(Position pos, String name, Type type) {
        return new DeclarationImpl.TypedefImpl(type, name, pos);
    }

    /**
     * Compute the layout for a given declaration.
     * @param d the declaration.
     * @return the layout for given declaration.
     */
    static Optional<MemoryLayout> layoutFor(Declaration d) {
        return switch (d) {
            case Scoped scoped -> DeclarationImpl.layoutFor(scoped);
            case Variable var -> Type.layoutFor(var.type());
            case Typedef typedef -> Type.layoutFor(typedef.type());
            case Constant constant -> Type.layoutFor(constant.type());
            default -> Optional.empty();
        };
    }

    /**
     * Compute the function descriptor for a given function declaration.
     * @param function the function declaration.
     * @return the function descriptor for given function declaration.
     */
    static Optional<FunctionDescriptor> descriptorFor(Function function) {
        return Type.descriptorFor(function.type());
    }

    /**
     * A record used to capture clang attributes attached to a declaration.
     * @param attributes a map from attribute name to attribute values.
     */
    record ClangAttributes(Map<String, List<String>> attributes) { }
}
