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

// Macro of constant function pointer
#define INVALID_INT_CONSUMER         (void (*)(int))0

struct foo;
typedef struct foo *foo_t;
struct bar;

// Macro of constant struct pointer
#define NO_FOO ((foo_t)0)

// Cases where resolving introduce new type references
// Pointer to pointer in macro
#define INVALID_INT_ARRAY_PTR (int**) 0
// Function pointer with pointer type argument
void (*op)(int cnt, int* operands);
void func(struct bar *pBar, struct foo *pFoo);

// Cyclic struct pointer within struct definitions
struct foo {
    foo_t ptrFoo;
    struct bar *ptrBar;
};

struct bar {
    foo_t ptrFoo;
    foo_t *arFooPtr;
};

// Function with array to pointer
void withArray(foo_t ar[2]);
