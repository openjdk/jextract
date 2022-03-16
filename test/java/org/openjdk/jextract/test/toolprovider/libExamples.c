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

#include "examples.h"

#ifdef _WIN64
#define EXPORT __declspec(dllexport)
#else
#define EXPORT
#endif

EXPORT char func_byte(void) { return 1; }
EXPORT short func_short(void) { return 2; }
EXPORT int func_int(void) { return 3; }
EXPORT long long func_long(void) { return 4; }
EXPORT float func_float(void) { return 5; }
EXPORT double func_double(void) { return 6; }

EXPORT char global_byte = 1;
EXPORT short global_short = 2;
EXPORT int global_int = 3;
EXPORT long long global_long = 4;
EXPORT float global_float = 5;
EXPORT double global_double = 6;

EXPORT void func_cb(CB cb) {
    cb(1);
}