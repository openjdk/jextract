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

#ifdef _WIN64
#define EXPORT __declspec(dllexport)
#else
#define EXPORT
#endif

#define macro_byte (char) 1
#define macro_short (short) 1
#define macro_int 1
#define macro_long 1LL
#define macro_float 1.0F
#define macro_double 1.0
#define macro_address_NULL (void*) 0
#define macro_address_123 (void*) 123
#define macro_string "abc"
#define macro_string_noident "123.asdf"

EXPORT char func_byte(void);
EXPORT short func_short(void);
EXPORT int func_int(void);
EXPORT long long func_long(void);
EXPORT float func_float(void);
EXPORT double func_double(void);

EXPORT extern char global_byte;
EXPORT extern short global_short;
EXPORT extern int global_int;
EXPORT extern long long global_long;
EXPORT extern float global_float;
EXPORT extern double global_double;

typedef void(*CB)(int);
EXPORT void func_cb(CB cb);

struct Foo {
    char c;
    short s;
    int i;
    long long ll;
    float f;
    double d;
};

typedef struct {
    int a;
    int b;
} Bar;

enum Enum {
    enum_0,
    enum_1,
    enum_2,
};

enum {
    enum_anon_0,
    enum_anon_1,
    enum_anon_2,
};