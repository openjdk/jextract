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

/*!
 * \brief Array of character pointers.
 *
 * Holds \a five pointers to character data.
 *
 * \var msg
 * \details The array contains exactly \c 5 pointer elements.
 * \warning The pointed-to strings are not owned by the array itself.
 *
 * Each entry may point to a <code>null</code>-terminated string.
 * \see signal
 */
char* msg[5];


enum Suit {
    club = 1,      ///< \a Clubs.
    diamonds = 2,  //!< \b Diamonds.
    hearts = 3,    /**< \brief Hearts. */
    spades = 4,    /*!< \p Spades. \anchor suit_spades */
};


/*!
 * \brief Sets a signal handler.
 * \param[in] sig Signal number.
 * \param func Pointer to the new handler.
 * \return Pointer to the previous signal handler.
 * \retval NULL No previous handler.
 * \see process
 */
void (*signal(int sig, void (*func)(int)))(int);


//! \brief Performs <i>processing</i> operation.
//! \param[in] a Value to process.
//! \warning Negative values may be unsupported.
void process(int a);


// FOO
#define FOO 42


/// @brief Contains an anonymous nested structure.
/// @verbatim
/// NestedAnon value = {
///     .u = {
///         .l = 10,
///         .h = 100LL
///     }
/// };
/// @endverbatim
struct NestedAnon {
    struct {
        int l;        ///< Lower value.
        long long h;  ///< Higher value.
    } u;              /**< Nested data. */
};


/*!
 * \short Opaque pointer to a \ref Foo structure.
 * \li The structure's implementation may be hidden.
 * \arg The structure's implementation may not be hidden.
 */
typedef struct Foo* OPAQUE_PTR;


/// \brief A callback description.
/// \param x Callback input.
/// \return Callback output.
typedef int (*example)(int x);


//! \brief Pointer to character data.
//!
//! Data may be \e null.
//!
//! Data has a static lifetime.
//! \attention May be \c NULL.
char* data;
