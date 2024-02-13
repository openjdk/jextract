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
package org.openjdk.jextract.test.toolprovider;

/*
 * MSVC: (/d1reportSingleClassLayoutFoo)
 * class Foo    size(24):
 *      +---
 *  0.    | a (bitstart=0,nbits=45)
 *  8.    | b (bitstart=0,nbits=24)
 *  8.    | c (bitstart=24,nbits=1)
 * 16.    | d (bitstart=0,nbits=58)
 *      +---
 *
 * SysV: (PAHole)
 * struct Foo {
 *     long long int a:45;                0:19   8
 *     long long int b:24;                0:251  8
 *     XXX 251 bits hole, try to pack
 *     long long int c:1;                 8:58   8
 *     long long int d:58;                8: 0   8
 *
 *     size: 16, cachelines: 1, members: 4
 *     bit holes: 1, sum bit holes: 251 bits
 *     bit_padding: 5 bits
 *     last cacheline: 16 bytes
 * };
 *
 */

import org.testng.annotations.Test;
import testlib.JextractToolRunner;

public class BadBitfieldTest extends JextractToolRunner {
    @Test
    public void testBadBitfield() {
        runAndCompile(getOutputFilePath("badBitfieldsGen"),
                getInputFilePath("badBitfields.h").toString());
    }
}
