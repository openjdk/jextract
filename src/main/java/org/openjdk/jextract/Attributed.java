/*
 *  Copyright (c) 2023, Oracle and/or its affiliates. All rights reserved.
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

import java.util.Collection;
import java.util.Optional;

/**
 * Subtypes of this interface can be customized with a variable number of attributes. Each attribute
 * is modelled as an instance of a record class. The record class is used to lookup attributes.
 * There can be at most one attribute associated with a given record class. Moreover, the set of
 * attributes associated with an entity implementing this interface can only monotonically increase over time
 * (that is, removing or replacing attributes is not supported).
 */
public interface Attributed {

    /**
     * {@return the attributes associated with this entity}
     */
    Collection<Record> attributes();

    /**
     * Obtains an attribute from this entity.
     * @param attributeClass the class of the attribute to be obtained.
     * @param <R> the attribute's type.
     * @return the attribute (if any).
     */
    <R extends Record> Optional<R> getAttribute(Class<R> attributeClass);

    /**
     * Adds a new attribute to this entity. This method is idempotent, that is, it allows adding an attribute
     * that is identical to the one already stored.
     * @param attribute the attribute to be added.
     * @param <R> the attribute's type.
     */
    <R extends Record> void addAttribute(R attribute);
}
