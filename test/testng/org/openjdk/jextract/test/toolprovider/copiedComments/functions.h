/*
 * Copyright (c) 2026, Oracle and/or its affiliates. All rights reserved.
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

void consumeCopyrightHeader(void);


// one line
void func1(int a);


// two
// lines
void func2(int a);


// these are four
// comments

// with whitespace
// in between
void func3(int a);


/// Doxygen
/// @param a parameter comment
void func4(int a);


/* a single line, multi-line comment */
void func5(int a);


/* a multi line,
   multi-line comment */
void func6(int a);


/*
 * internal
 *
 * comment
 */
void func7(int a);


/**
 * Doxygen
 * @param a par1
 * @param b par2
 * @return ret
 */
int func8(int a, int b);
