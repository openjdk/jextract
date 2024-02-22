/*
 * Copyright (c) 2024, Oracle and/or its affiliates. All rights reserved.
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

struct A { };

// nested

typedef struct { struct A a; } t_str;

void f_str_arg(struct { struct A a; });

struct { struct A a; } f_str_ret();

struct { struct A a; } v_str;

// function pointers

typedef void (*t_fp_arg)(struct A);

typedef struct A (*t_fp_ret)(int);

void f_fp_arg(void (*p)(struct A));

void f_fp_ret(struct A (*p)(int));

void (*v_fp_arg)(struct A);

struct A (*v_fp_ret)(int);
