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

#ifdef _WIN64
#define EXPORT __declspec(dllexport)
#else
#define EXPORT
#endif

// function declarations
EXPORT struct UNSUPPORTED f1(struct UNSUPPORTED p);

// global variable declarations

EXPORT struct UNSUPPORTED (*fp)(struct UNSUPPORTED p);

// typedef declarations

typedef struct UNSUPPORTED (*td)(struct UNSUPPORTED p);

// field declarations

struct Outer {
    struct UNSUPPORTED (*fp)(struct UNSUPPORTED p);
};

// field and global

EXPORT struct {
    struct UNSUPPORTED (*fp)(struct UNSUPPORTED p);
} outer_var;

// field and typedef

typedef struct {
    struct UNSUPPORTED (*fp)(struct UNSUPPORTED p);
} outer_td;

// field and function return

EXPORT struct {
    struct UNSUPPORTED (*fp)(struct UNSUPPORTED p);
} f2(void);

// field and function arg
EXPORT void f3(struct {
    struct UNSUPPORTED (*fp)(struct UNSUPPORTED p);
} p);
