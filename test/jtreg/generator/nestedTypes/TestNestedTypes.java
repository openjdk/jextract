/*
 * Copyright (c) 2023, 2024, Oracle and/or its affiliates. All rights reserved.
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

import java.lang.foreign.GroupLayout;
import java.lang.foreign.MemoryLayout;
import java.lang.foreign.SequenceLayout;

import org.testng.annotations.Test;

import static org.testng.Assert.*;

import test.jextract.nestedtypes.*;

/*
 * @test id
 * @library /lib
 * @run main/othervm JtregJextract -t test.jextract.nestedtypes nested_types.h
 * @build TestNestedTypes
 * @run testng/othervm --enable-native-access=ALL-UNNAMED TestNestedTypes
 */
public class TestNestedTypes {

    @Test
    public void testNestedTypes() {
        checkNestedLayout(NestedStructArray.$LAYOUT());
        checkNestedLayout(NestedStructArrayTypedef.$LAYOUT());
        checkNestedLayout(NestedStructArrayTypedefTypedef.$LAYOUT());
    }

    void checkNestedLayout(MemoryLayout layout) {
        MemoryLayout nestedLayout = ((GroupLayout)layout).memberLayouts().get(0);
        assertEquals(nestedLayout.name().get(), "nested");
        assertTrue(nestedLayout instanceof SequenceLayout);
        assertEquals(((SequenceLayout)nestedLayout).elementCount(), 1);
        assertEquals(((SequenceLayout)nestedLayout).elementLayout().withoutName(), ELEM_NESTED_LAYOUT);
    }

    static final MemoryLayout ELEM_NESTED_LAYOUT = MemoryLayout.structLayout(nested_types_h.C_INT.withName("x"));
}
