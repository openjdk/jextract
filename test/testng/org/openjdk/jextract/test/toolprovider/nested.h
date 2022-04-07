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

struct Foo {
    struct Bar {
        int x, y;
    } bar;

    enum Color {
        red, green, blue
    } color;
};

union U {
    struct Point {
        short x, y;
    } point;

    enum RGB {
        r, g, b
    } rgb;

    int i;
};

struct MyStruct {
    char a;
    struct {
        int b;
        union {
            int c;
        };
        char d;
        struct MyStruct_Z {
            char e;
        } f;
    };
    union {
        int g;
        long long h;
    };
    enum {
        X, Y, Z
    };
    struct {
        int i;
        int j;
    } k;
};

union MyUnion {
    char a;
    struct {
        int b;
        union {
            int c;
        };
        char d;
        struct MyUnion_Z {
            char e;
        } f;
    };
    struct {
        int g;
        int h;
    };
    enum {
        A, B, C
    };
    union {
        int i;
        long long j;
    } k;
};

struct X {
    struct {
        union {
            int y;
        } Z;
    };
};

struct X2 {
    struct {
        union {
            int y;
        }; // no name this time
    };
};

struct NestedUnion {
    int x;
    union {
        int y;
        int z;
    };
};
