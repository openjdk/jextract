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
import test.jextract.nestedtypes.names.*;

import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.MemoryLayout;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

/*
 * @test
 * @library /lib
 * @run main/othervm JtregJextract -t test.jextract.nestedtypes.names nested_types_names.h
 * @build TestNestedTypesNames
 * @run testng/othervm --enable-native-access=ALL-UNNAMED TestNestedTypesNames
 */
public class TestNestedTypesNames {

    static MemoryLayout ARG_STRUCT = MemoryLayout.structLayout(
        nested_types_names_h.C_INT.withName("y")
    );

    static MemoryLayout RET_STRUCT = MemoryLayout.structLayout(
        nested_types_names_h.C_INT.withName("x")
    );

    static FunctionDescriptor FUNC_DESC = FunctionDescriptor.of(
            RET_STRUCT,
            ARG_STRUCT
    );

    @Test
    public void testTypeNamesAndLayouts() {
        // function, anonymous
        checkLayout(f1$return.$LAYOUT(), RET_STRUCT);
        checkLayout(f1$x0.$LAYOUT(), ARG_STRUCT);
        // function, explicit names
        checkLayout(SR_FUN.$LAYOUT(), RET_STRUCT);
        checkLayout(SA_FUN.$LAYOUT(), ARG_STRUCT);
        // global, anonymous
        checkLayout(fp1$return.$LAYOUT(), RET_STRUCT);
        checkLayout(fp1$x0.$LAYOUT(), ARG_STRUCT);
        checkDescriptor(fp1.$DESC, FUNC_DESC);
        // global, explicit names
        checkLayout(SR_VAR.$LAYOUT(), RET_STRUCT);
        checkLayout(SA_VAR.$LAYOUT(), ARG_STRUCT);
        checkDescriptor(fp2.$DESC, FUNC_DESC);
        // typedef, anonymous
        checkLayout(td1$return.$LAYOUT(), RET_STRUCT);
        checkLayout(td1$x0.$LAYOUT(), ARG_STRUCT);
        checkDescriptor(td1.$DESC, FUNC_DESC);
        // typedef, explicit names
        checkLayout(SR_DEF.$LAYOUT(), RET_STRUCT);
        checkLayout(SA_DEF.$LAYOUT(), ARG_STRUCT);
        checkDescriptor(td2.$DESC, FUNC_DESC);
        // struct, anonymous
        checkLayout(Outer.fp1$return.$LAYOUT(), RET_STRUCT);
        checkLayout(Outer.fp1$x0.$LAYOUT(), ARG_STRUCT);
        checkDescriptor(Outer.fp1.$DESC, FUNC_DESC);
        // struct, explicit names
        checkLayout(Outer.SR_FLD.$LAYOUT(), RET_STRUCT);
        checkLayout(Outer.SA_FLD.$LAYOUT(), ARG_STRUCT);
        checkDescriptor(Outer.fp2.$DESC, FUNC_DESC);
        // struct + global, anonymous
        checkLayout(outer_var.fp1$return.$LAYOUT(), RET_STRUCT);
        checkLayout(outer_var.fp1$x0.$LAYOUT(), ARG_STRUCT);
        checkDescriptor(outer_var.fp1.$DESC, FUNC_DESC);
        // struct + global, explicit names
        checkLayout(outer_var.SR_FLD_VAR.$LAYOUT(), RET_STRUCT);
        checkLayout(outer_var.SA_FLD_VAR.$LAYOUT(), ARG_STRUCT);
        checkDescriptor(outer_var.fp2.$DESC, FUNC_DESC);
        // struct + typedef, anonymous
        checkLayout(outer_td.fp1$return.$LAYOUT(), RET_STRUCT);
        checkLayout(outer_td.fp1$x0.$LAYOUT(), ARG_STRUCT);
        checkDescriptor(outer_td.fp1.$DESC, FUNC_DESC);
        // struct + typedef, explicit names
        checkLayout(outer_td.SR_FLD_DEF.$LAYOUT(), RET_STRUCT);
        checkLayout(outer_td.SA_FLD_DEF.$LAYOUT(), ARG_STRUCT);
        checkDescriptor(outer_td.fp2.$DESC, FUNC_DESC);
        // struct + func return, anonymous
        checkLayout(f3$return.fp1$return.$LAYOUT(), RET_STRUCT);
        checkLayout(f3$return.fp1$x0.$LAYOUT(), ARG_STRUCT);
        checkDescriptor(f3$return.fp1.$DESC, FUNC_DESC);
        // struct + func return, explicit names
        checkLayout(f3$return.SR_FLD_FUN_RET.$LAYOUT(), RET_STRUCT);
        checkLayout(f3$return.SA_FLD_FUN_RET.$LAYOUT(), ARG_STRUCT);
        checkDescriptor(f3$return.fp2.$DESC, FUNC_DESC);
        // struct + func arg, anonymous
        checkLayout(f4$x0.fp1$return.$LAYOUT(), RET_STRUCT);
        checkLayout(f4$x0.fp1$x0.$LAYOUT(), ARG_STRUCT);
        checkDescriptor(f4$x0.fp1.$DESC, FUNC_DESC);
        // struct + func arg, explicit names
        checkLayout(f4$x0.SR_FLD_FUN_ARG.$LAYOUT(), RET_STRUCT);
        checkLayout(f4$x0.SA_FLD_FUN_ARG.$LAYOUT(), ARG_STRUCT);
        checkDescriptor(f4$x0.fp2.$DESC, FUNC_DESC);
    }

    void checkLayout(MemoryLayout found, MemoryLayout expected) {
        assertEquals(found.withoutName(), expected);
    }

    void checkDescriptor(FunctionDescriptor found, FunctionDescriptor expected) {
        assertEquals(
                FunctionDescriptor.of(
                        found.returnLayout().get().withoutName(),
                        found.argumentLayouts().get(0).withoutName()),
                expected);
    }
}
