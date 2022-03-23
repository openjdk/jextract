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

struct Point;
struct Point;

int i;
int i;

void func(int);
void func(int abc);
void func(int xyz);
void func2(int);
void func2(int abc);
void func2(int xyz);

typedef int INT;
void func(INT);
void func(INT abc);
void func(INT xyz);
void func2(INT);
void func2(INT abc);
void func2(INT xyz);

typedef int* INTPTR;
void func3(INTPTR x);
void func3(int* x);
void func4(INTPTR x);
void func4(int* x);

typedef int Integer;
void func(Integer x);
void func5(int x);
void func5(Integer x);
void func5(INT x);

struct Point;
struct Point {
   int i;
   int j;
};

typedef struct Point POINT;
typedef struct Point Point_t;

double distance(struct Point p);
double distance(POINT p);

typedef struct Point3D {
    int i;
    int j;
    int k;
} Point3D_t;
struct Point3D;

enum RGBColor;
enum RGBColor {
   R, G, B
};

enum CMYColor {
  C, M, Y
};
enum CMYColor;
