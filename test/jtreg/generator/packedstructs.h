/*
 * Copyright (c) 2023, Oracle and/or its affiliates. All rights reserved.
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

#pragma pack(1)
struct S1 {
   char first;
   int second;
};

#pragma pack(1)
struct S2 {
   char first;
   struct { int i } second;
};

#pragma pack(1)
struct S3 {
   char first;
   int second[2];
};

#pragma pack(1)
struct S4 {
   char first;
   union { int x; int y; } second;
};

#pragma pack(1)
struct S5 {
   char first;
   union { struct { int i } x; struct { int i } y; } second;
};

#pragma pack(1)
struct S6 {
   char first;
   union { int x[2]; int y[2]; } second;
};

#pragma pack(1)
struct S7 {
   long long first;
   int second;
};

#pragma pack(1)
struct S8 {
   struct S7 first[1];
   struct S7 second[1];
};
