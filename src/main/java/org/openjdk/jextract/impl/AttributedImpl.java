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

package org.openjdk.jextract.impl;

import org.openjdk.jextract.Attributed;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public abstract class AttributedImpl implements Attributed {

    private final Map<Class<?>, Record> attributes = new HashMap<>();

    @Override
    public Collection<Record> attributes() {
        return attributes.values();
    }

    @Override
    @SuppressWarnings("unchecked")
    public <R extends Record> Optional<R> getAttribute(Class<R> attributeClass) {
        return Optional.ofNullable((R)attributes.get(attributeClass));
    }

    @Override
    public <R extends Record> void addAttribute(R attribute) {
        if (attributes.containsKey(attribute.getClass())) {
            throw new IllegalStateException("Attribute already exists: " + attribute.getClass().getSimpleName());
        }
        attributes.put(attribute.getClass(), attribute);
    }
}
