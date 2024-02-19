/* Copyright (c) 2023, 2024, Oracle and/or its affiliates. All rights reserved.
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
import java.lang.foreign.MemoryLayout.PathElement;

import org.testng.annotations.Test;

import static org.testng.Assert.*;

import test.jextract.packedstructs.*;

/*
 * @test
 * @library /lib
 * @run main/othervm JtregJextract -t test.jextract.packedstructs packedstructs.h
 * @build TestPackedStructs
 * @run testng/othervm --enable-native-access=ALL-UNNAMED TestPackedStructs
 */
public class TestPackedStructs {

    @Test
    public void testPackedStructs() {
        checkLayout(S1.layout());
        checkLayout(S2.layout());
        checkLayout(S3.layout());
        checkLayout(S4.layout());
        checkLayout(S5.layout());
        checkLayout(S6.layout());
        checkLayout(S7.layout());
        checkLayout(S8.layout());
        checkLayout(S9.layout());
        checkLayout(S10.layout());
        checkLayout(S11.layout());
        checkLayout(S12.layout());
    }

    private void checkLayout(MemoryLayout layout) {
        layout.select(PathElement.groupElement("first"));
        layout.select(PathElement.groupElement("second"));
        assertEquals(((GroupLayout)layout).memberLayouts().get(1).byteAlignment(), 1);
    }
}
