/*
 * Copyright (c) 2019, Oracle and/or its affiliates. All rights reserved.
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
  #ifdef IMPL
    #define EXPORT __declspec(dllexport)
  #else
    #define EXPORT __declspec(dllimport)
  #endif // IMPL
#else
#define EXPORT
#endif //_WIN64

#ifdef _WIN32
// Windows doesn't really support asm symbol, this is similar approach for C code to
// achieve similar, but this won't work with Panama until we support such Macro
#ifdef ADD
#define foo fooA
#define func funcA
#else
#define foo fooB
#define func funcB
#endif //ADD
#define ALIAS(sym)

#elif __APPLE__
#define ALIAS(sym) __asm("_" #sym)
#else
#define ALIAS(sym) __asm__(#sym)
#endif // _WIN32

// We do 3 declarations to make sure we will pick up alias no matter the sequence of encounter
// Without alias
EXPORT extern int foo;
EXPORT int func (int x, int y);

// With alias
#ifdef ADD

EXPORT extern int foo ALIAS(fooA);
EXPORT int func (int x, int y) ALIAS(funcA);

#else

EXPORT extern int foo ALIAS(fooB);
EXPORT int func (int x, int y) ALIAS(funcB);

#endif // ADD

// Without alias again
EXPORT extern int foo;
EXPORT int func (int x, int y);

