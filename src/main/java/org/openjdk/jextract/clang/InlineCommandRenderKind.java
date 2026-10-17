/*
 *  Copyright (c) 2026, Oracle and/or its affiliates. All rights reserved.
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
 *  Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 *  or visit www.oracle.com if you need additional information or have any
 *  questions.
 *
 */

package org.openjdk.jextract.clang;

import org.openjdk.jextract.clang.libclang.Index_h;

import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;

public enum InlineCommandRenderKind {

    Anchor(Index_h.CXCommentInlineCommandRenderKind_Anchor()),
    Bold(Index_h.CXCommentInlineCommandRenderKind_Bold()),
    Emphasized(Index_h.CXCommentInlineCommandRenderKind_Emphasized()),
    Monospaced(Index_h.CXCommentInlineCommandRenderKind_Monospaced()),
    Normal(Index_h.CXCommentInlineCommandRenderKind_Normal());

    private final int value;

    InlineCommandRenderKind(int value) {
        this.value = value;
    }

    public int value() {
        return value;
    }

    private final static Map<Integer, InlineCommandRenderKind> lookup;

    static {
        lookup = new HashMap<>();
        for (InlineCommandRenderKind e: InlineCommandRenderKind.values()) {
            lookup.put(e.value(), e);
        }
    }

    public static InlineCommandRenderKind valueOf(int value) {
        InlineCommandRenderKind x = lookup.get(value);
        if (null == x) {
            throw new NoSuchElementException("Invalid inline command render kind value: " + value);
        }
        return x;
    }
}
