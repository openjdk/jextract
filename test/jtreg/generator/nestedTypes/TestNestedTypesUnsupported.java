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

import org.testng.annotations.Test;
import test.jextract.nestedtypes.unsupported.*;

import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.MemoryLayout;
import java.lang.foreign.MemorySegment;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

/*
 * @test
 * @library /lib
 * @run main/othervm JtregJextract -t test.jextract.nestedtypes.unsupported nested_types_unsupported.h
 * @build TestNestedTypesUnsupported
 * @run testng/othervm --enable-native-access=ALL-UNNAMED TestNestedTypesUnsupported
 */
public class TestNestedTypesUnsupported {

    static MemoryLayout UNDEFINED_STRUCT = MemoryLayout.structLayout(
            MemoryLayout.paddingLayout(nested_types_unsupported_h.C_POINTER.byteSize())
    );

    @Test
    public void testTypeNamesAndLayouts() {
        checkLayout(Outer.layout(), UNDEFINED_STRUCT);
        checkLayout(outer_var.layout(), UNDEFINED_STRUCT);
        checkLayout(outer_td.layout(), UNDEFINED_STRUCT);
        checkLayout(outer_td$0.layout(), UNDEFINED_STRUCT);
        checkLayout(f2$return.layout(), UNDEFINED_STRUCT);
        checkLayout(f3$x0.layout(), UNDEFINED_STRUCT);
    }

    void checkLayout(MemoryLayout found, MemoryLayout expected) {
        assertEquals(found.withoutName(), expected);
    }
}
