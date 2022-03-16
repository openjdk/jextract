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

#include "constants_aux.h"

#define ZERO 0
#define ONE ZERO + 1 //backward ref
#define THREE ONE + TWO /* forward ref */
#define TWO ONE + ONE

#define FOUR (long long)0x1L + THREE //hack: force long carrier
#define FIVE (long long)0x1UL + FOUR //hack: force long carrier

#define SIX ONE +\
              TWO +\
              THREE

#define STR "Hello" // a string

#define ID(x) x //function-like
#define SUM(x,y) x + y //function-like

#define BLOCK_BEGIN { //not a constant
#define BLOCK_END } //not a constant

#define INTEGER_MAX_VALUE Integer.MAX_VALUE //constant in Java, not in C
#define QUOTE "QUOTE" //should be ok

#define FLOAT_VALUE 1.32F;
#define DOUBLE_VALUE 1.32;

#define CYCLIC_1 1 + CYCLIC_1 //cycle

#define CYCLIC_2 1 + TEMP //indirect cycle
#define TEMP 1 + CYCLIC_2

#define CHAR_VALUE 'h'
#define MULTICHAR_VALUE 'hh'

#define BOOL_VALUE (_Bool)1
//we should have tests for char and shorts, but these are likely to be platform dependent

#define SUB SUP + 2 //dependency

#define ZERO_PTR (void*)0;
#define F_PTR (void*) 0xFFFFFFFFFFFFFFFFLL; // all 1s

#define ARRAY { 0, 1, 2, 3, 4, 5 }
