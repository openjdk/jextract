/*
 * Copyright (c) 2026, Oracle and/or its affiliates. All rights reserved.
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

// block commands supported by clang 13.0.0 with a JavaDoc equivalent

/// \brief mdfkR
/// \param a DMppz
/// \throw PBHox
/// \throws gQggq
/// \exception JlLlB
/// \see rldXW
/// \sa KBJtl
/// \deprecated jUNVH
/// \since guUpf
/// \author ZJYmn
int func_kOfvP(int a);

/// \short wjPaq
/// \returns nOZxm
/// \authors vHYBD
int func_nIwyf();

/// \result vdHVK
int func_PbMtd();

/// \par hlMuF
/// \arg jmvOH
/// \li DVDvK
int var_QJzCL;


// block commands without a JavaDoc equivalent

/// \attention zIVELv
/// \bug DoGkH
/// \copyright RfboO
/// \date bkWkZ
/// \details rsOFX
/// \invariant tnJBI
/// \note tYiZb
/// \post HiYkH
/// \pre KBAio
/// \remark JCPgR
/// \remarks mZwCi
/// \retval xVwZQ
/// \todo JOBgs
/// \version KIQOC
/// \warning NiKEQ
int var_wXjml;


// verbatim block commands

/// \verbatim
/// GsgCz
/// \endverbatim
/// \code
/// sQgMZ
/// \endcode
int var_UGxIb;


// inline commands supported by clang 13.0.0

/// \b XIrls \a XSXDD \e tziIu \em oBYQL \c ThZXB \p UpCVA \ref PYFvb \anchor oiqLb
int var_MIoKP;


// HTML

/// <a kDFlc="cWJBx">KgNnj</a>
/// <br />
/// <tt>MaHPQ</tt> <strike>JNDRQ</strike>
int var_TgdcO;


// Parameters

/// @param a xXqcs
/// @param[in] b fEpuI
/// @param[out] c mPVjJ
/// @param[in,out] d MaUPO
/// @param[out,in] e blCUS
void func_ajwqE(int *a, int *b, int *c, int *d, int *e);

/// \brief YqFta
/// \param a vQxbR
/// \return fKPsJ
typedef int (*typedef_KdWnL)(int a);


// Declaration commands
// - except \enum, as enum tag is not retained
// - except \def, as macros are not supported by clang

/// \var var_Vqube
/// var_Vqube
int var_Vqube;

/// \fn func_pIERk
/// func_pIERk
void func_pIERk();

/// \typedef typedef_wSJnT
/// typedef_wSJnT
typedef int typedef_wSJnT;

/// \struct struct_hcHgy
/// struct_hcHgy
struct struct_hcHgy {};

/// \union union_IiFeM
/// union_IiFeM
union union_IiFeM {};
