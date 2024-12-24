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
EXPORT struct { int x; } f1(struct { int y; } p);

EXPORT struct SR_FUN { int x; } f2(struct SA_FUN { int y; } p);

// global variable declarations

EXPORT struct { int x; } (*fp1)(struct { int y; } p);

EXPORT struct SR_VAR { int x; } (*fp2)(struct SA_VAR { int y; } p);

// typedef declarations

typedef struct { int x; } (*td1)(struct { int y; } p);

typedef struct SR_DEF { int x; } (*td2)(struct SA_DEF { int y; } p);

// field declarations

struct Outer {
    struct { int x; } (*fp1)(struct { int y; } p);
    struct SR_FLD { int x; } (*fp2)(struct SA_FLD { int y; } p);
};

// field and global

EXPORT struct {
    struct { int x; } (*fp1)(struct { int y; } p);
    struct SR_FLD_VAR { int x; } (*fp2)(struct SA_FLD_VAR { int y; } p);
} outer_var;

// field and typedef

typedef struct {
    struct { int x; } (*fp1)(struct { int y; } p);
    struct SR_FLD_DEF { int x; } (*fp2)(struct SA_FLD_DEF { int y; } p);
} outer_td;

// field and function return

EXPORT struct {
    struct { int x; } (*fp1)(struct { int y; } p);
    struct SR_FLD_FUN_RET { int x; } (*fp2)(struct SA_FLD_FUN_RET { int y; } p);
} f3(void);

// field and function arg
EXPORT void f4(struct {
    struct { int x; } (*fp1)(struct { int y; } p);
    struct SR_FLD_FUN_ARG { int x; } (*fp2)(struct SA_FLD_FUN_ARG { int y; } p);
} p);
