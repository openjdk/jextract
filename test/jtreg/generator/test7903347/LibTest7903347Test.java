/*
 * Copyright (c) 2020, Oracle and/or its affiliates. All rights reserved.
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
import static test.jextract.test7903347.test7903347_h.*;

/*
 * @test id=classes
 * @bug 7903347
 * @summary add long name option for all single letter options and expand help on default values for various options
 * @library /lib
 * @run main/othervm JtregJextract --library Test7903347 -t test.jextract.test7903347 test7903347.h
 * @run testng/othervm --enable-native-access=ALL-UNNAMED LibTest7903347Test
 */
/*
 * @test id=sources
 * @bug 7903347
 * @summary add long name option for all single letter options and expand help on default values for various options
 * @library /lib
 * @run main/othervm JtregJextractSources --library Test7903347 -t test.jextract.test7903347 test7903347.h
 * @run testng/othervm --enable-native-access=ALL-UNNAMED LibTest7903347Test
 */
public class LibTest7903347Test {
    @Test
    public void test() {
        print_point(34, 56);
    }
}
