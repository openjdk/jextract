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

__int128 returns_unsupported(void);
void accepts_unsupported(__int128);

typedef __int128 (*unsupported_func_t)(void);

// this insane syntax indicates a function 'returns_unsupported_func(void)'
// that returns a '__int128 (*)(void)' function pointer
__int128 (*returns_unsupported_func(void))(void);
void accepts_unsupported_func(__int128 (*)(void));
void accepts_unsupported_func_varargs(void (*)(int, ...));

extern __int128 GLOBAL_UNSUPPORTED;
extern __int128 (*GLOBAL_UNSUPPORTED_FUNC)(void);

struct undeclared;

struct undeclared returns_undeclared(void);
void accepts_undeclared(struct undeclared);
void accepts_undeclared_func(void (*)(struct undeclared));

extern struct undeclared GLOBAL_UNDECLARED;

struct HasUnsupported {
    __int128 x;
};

extern struct HasUnsupported GLOBAL_HAS_UNSUPPORTED;
