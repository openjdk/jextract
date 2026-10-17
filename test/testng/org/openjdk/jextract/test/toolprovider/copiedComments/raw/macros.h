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

// line 01
// line 02
// line 03
// line 04
// line 05
// line 06
// line 07
// line 08
// line 09
// line 10
// line 11
// line 12
#define CROSS_DEFAULT_BATCH_SIZE 12


#\
\
\
\
\
\
\
\
\
\
\
\
define CROSS_DEFAULT_BATCH_SIZE_LINE_SPLICING 1


// FOO
#define FOO 42


// MSG
#define MSG "Hello"


// MSG_COMMENT
#define MSG_COMMENT "HelloWithComment" /* Some comment */


/* block comment */
#define BASIC_BLOCK 1


        // indented comment
#define INDENTED_COMMENT 1

// indented macro
        #define INDENTED_MACRO 1


// too far away

#define EMPTY_LINE 1


int x; // will be associated with AFTER_DECLARATION
#define AFTER_DECLARATION 1


int y; /* will be associated with AFTER_DECLARATION_2 */
#define AFTER_DECLARATION_2 1


// first
// second
// third
#define MULTIPLE_LINE_COMMENTS 1


// line comment
/* block comment */
#define MIXED_COMMENT_TYPES 1


/*
 * multi-line
 * block comment
 */
#define MULTILINE_BLOCK 1


/*
 * multi-line
 * block comment
 * gap
 */

#define MULTILINE_BLOCK_GAP 1


// ignored line comment

// kept line comment
#define GAP_IN_BETWEEN_COMMENTS 1


/* ignored block comment */

/* kept block comment */
#define GAP_IN_BETWEEN_BLOCK_COMMENTS 1


// 1. line comment that is kept
/* 2. block comment that is kept */
// 3. line comment is that kept
/* 4. block comment that is kept */
#define ALTERNATING_COMMENT_TYPES 1
