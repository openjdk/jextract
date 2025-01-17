/*
 * Copyright (c) 2024, 2025, Oracle and/or its affiliates. All rights reserved.
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
 * @build testlib.TestUtils
 * @run main/othervm JtregJextract -t test.jextract.nestedtypes.names nested_types_names.h
 * @build TestNestedTypesNames
 * @run testng/othervm --enable-native-access=ALL-UNNAMED TestNestedTypesNames
 */
public class TestNestedTypesNames {

    static MemoryLayout ARG_STRUCT = MemoryLayout.structLayout(
            LayoutUtils.C_INT.withName("y")
    );

    static MemoryLayout RET_STRUCT = MemoryLayout.structLayout(
            LayoutUtils.C_INT.withName("x")
    );

    static FunctionDescriptor FUNC_DESC = FunctionDescriptor.of(
            RET_STRUCT,
            ARG_STRUCT
    );

    @Test
    public void testTypeNamesAndLayouts() {
        // function, anonymous
        checkLayout(f1$return.layout(), RET_STRUCT);
        checkLayout(f1$x0.layout(), ARG_STRUCT);
        // function, explicit names
        checkLayout(SR_FUN.layout(), RET_STRUCT);
        checkLayout(SA_FUN.layout(), ARG_STRUCT);
        // global, anonymous
        checkLayout(fp1$return.layout(), RET_STRUCT);
        checkLayout(fp1$x0.layout(), ARG_STRUCT);
        checkDescriptor(fp1.descriptor(), FUNC_DESC);
        // global, explicit names
        checkLayout(SR_VAR.layout(), RET_STRUCT);
        checkLayout(SA_VAR.layout(), ARG_STRUCT);
        checkDescriptor(fp2.descriptor(), FUNC_DESC);
        // typedef, anonymous
        checkLayout(td1$return.layout(), RET_STRUCT);
        checkLayout(td1$x0.layout(), ARG_STRUCT);
        checkDescriptor(td1.descriptor(), FUNC_DESC);
        // typedef, explicit names
        checkLayout(SR_DEF.layout(), RET_STRUCT);
        checkLayout(SA_DEF.layout(), ARG_STRUCT);
        checkDescriptor(td2.descriptor(), FUNC_DESC);
        // struct, anonymous
        checkLayout(Outer.fp1$return.layout(), RET_STRUCT);
        checkLayout(Outer.fp1$x0.layout(), ARG_STRUCT);
        checkDescriptor(Outer.fp1.descriptor(), FUNC_DESC);
        // struct, explicit names
        checkLayout(Outer.SR_FLD.layout(), RET_STRUCT);
        checkLayout(Outer.SA_FLD.layout(), ARG_STRUCT);
        checkDescriptor(Outer.fp2.descriptor(), FUNC_DESC);
        // struct + global, anonymous
        checkLayout(outer_var.fp1$return.layout(), RET_STRUCT);
        checkLayout(outer_var.fp1$x0.layout(), ARG_STRUCT);
        checkDescriptor(outer_var.fp1.descriptor(), FUNC_DESC);
        // struct + global, explicit names
        checkLayout(outer_var.SR_FLD_VAR.layout(), RET_STRUCT);
        checkLayout(outer_var.SA_FLD_VAR.layout(), ARG_STRUCT);
        checkDescriptor(outer_var.fp2.descriptor(), FUNC_DESC);
        // struct + typedef, anonymous
        checkLayout(outer_td.fp1$return.layout(), RET_STRUCT);
        checkLayout(outer_td.fp1$x0.layout(), ARG_STRUCT);
        checkDescriptor(outer_td.fp1.descriptor(), FUNC_DESC);
        // struct + typedef, explicit names
        checkLayout(outer_td.SR_FLD_DEF.layout(), RET_STRUCT);
        checkLayout(outer_td.SA_FLD_DEF.layout(), ARG_STRUCT);
        checkDescriptor(outer_td.fp2.descriptor(), FUNC_DESC);
        // struct + func return, anonymous
        checkLayout(f3$return.fp1$return.layout(), RET_STRUCT);
        checkLayout(f3$return.fp1$x0.layout(), ARG_STRUCT);
        checkDescriptor(f3$return.fp1.descriptor(), FUNC_DESC);
        // struct + func return, explicit names
        checkLayout(f3$return.SR_FLD_FUN_RET.layout(), RET_STRUCT);
        checkLayout(f3$return.SA_FLD_FUN_RET.layout(), ARG_STRUCT);
        checkDescriptor(f3$return.fp2.descriptor(), FUNC_DESC);
        // struct + func arg, anonymous
        checkLayout(f4$x0.fp1$return.layout(), RET_STRUCT);
        checkLayout(f4$x0.fp1$x0.layout(), ARG_STRUCT);
        checkDescriptor(f4$x0.fp1.descriptor(), FUNC_DESC);
        // struct + func arg, explicit names
        checkLayout(f4$x0.SR_FLD_FUN_ARG.layout(), RET_STRUCT);
        checkLayout(f4$x0.SA_FLD_FUN_ARG.layout(), ARG_STRUCT);
        checkDescriptor(f4$x0.fp2.descriptor(), FUNC_DESC);
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
