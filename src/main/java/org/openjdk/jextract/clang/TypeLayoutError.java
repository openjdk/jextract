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

package org.openjdk.jextract.clang;

import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;

public class TypeLayoutError extends IllegalStateException {

    private static final long serialVersionUID = 0L;

    private final Kind kind;

    public TypeLayoutError(long value, String message) {
        super(Kind.valueOf(value) + ". " + message);
        this.kind = Kind.valueOf(value);
    }

    public Kind kind() {
        return kind;
    }

    public static boolean isError(long value) {
        return Kind.isError(value);
    }

    public enum Kind {
        Invalid(-1),
        Incomplete(-2),
        Dependent(-3),
        NotConstantSize(-4),
        InvalidFieldName(-5);

        private final long value;

        Kind(long value) {
            this.value = value;
        }

        private final static Map<Long, Kind> lookup;

        static {
            lookup = new HashMap<>();
            for (Kind e: Kind.values()) {
                lookup.put(e.value, e);
            }
        }

        public final static Kind valueOf(long value) {
            Kind x = lookup.get(value);
            if (null == x) {
                throw new NoSuchElementException("TypeLayoutError = " + value);
            }
            return x;
        }

        public static boolean isError(long value) {
            return lookup.containsKey(value);
        }
    }
}
