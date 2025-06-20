/*
 * Copyright (c) 2020, 2024, Oracle and/or its affiliates. All rights reserved.
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
import static org.testng.Assert.assertEquals;
import static test.jextract.testEnums.testEnums_h.CodetypeT;
import static test.jextract.testEnums.testEnums_h.Size;
import static test.jextract.testEnums.testEnums_h.Temp;
import static test.jextract.testEnums.testEnums_h.TempT;

/*
 * @test
 * @library /lib
 * @run main/othervm JtregJextract --generate-java-enums -l libTestEnums -t test.jextract.testEnums testEnums.h
 * @build LibTestEnums
 * @run testng/othervm LibTestEnums
 */
public class LibTestEnums {
    @Test
    public void test() {
        assertEquals(CodetypeT.JAVA.getValue(), 0);
        assertEquals(CodetypeT.C.getValue(), 1);
        assertEquals(CodetypeT.CPP.getValue(), 2);
        assertEquals(CodetypeT.PYTHON.getValue(), 3);
        assertEquals(CodetypeT.RUBY.getValue(), 4);

        assertEquals(Size.XS.getValue(), 0);
        assertEquals(Size.S.getValue(), 1);
        assertEquals(Size.M.getValue(), 2);
        assertEquals(Size.L.getValue(), 3);
        assertEquals(Size.XL.getValue(), 4);
        assertEquals(Size.XXL.getValue(), 5);

        assertEquals(Temp.ONE.getValue(), 1);
        assertEquals(Temp.TWO.getValue(), 2);

        assertEquals(TempT.ONE.getValue(), 1);
        assertEquals(TempT.TWO.getValue(), 2);
        assertEquals(TempT.THREE.getValue(), 3);
    }
}