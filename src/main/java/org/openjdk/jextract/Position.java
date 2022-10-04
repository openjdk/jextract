/*
 *  Copyright (c) 2020, Oracle and/or its affiliates. All rights reserved.
 *  DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 *  This code is free software; you can redistribute it and/or modify it
 *  under the terms of the GNU General Public License version 2 only, as
 *  published by the Free Software Foundation.  Oracle designates this
 *  particular file as subject to the "Classpath" exception as provided
 *  by Oracle in the LICENSE file that accompanied this code.
 *
 *  This code is distributed in the hope that it will be useful, but WITHOUT
 *  ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 *  FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 *  version 2 for more details (a copy is included in the LICENSE file that
 *  accompanied this code).
 *
 *  You should have received a copy of the GNU General Public License version
 *  2 along with this work; if not, write to the Free Software Foundation,
 *  Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 *   Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 *  or visit www.oracle.com if you need additional information or have any
 *  questions.
 *
 */

package org.openjdk.jextract;

import java.nio.file.Path;
import java.util.Objects;

/**
 * Instances of this class model are used to model source code positions.
 */
public interface Position {

    /**
     * The input file to which this position refers to.
     * @return The input file to which this position refers to.
     */
    Path path();

    /**
     * The line number associated with this position.
     * @return The line number associated with this position.
     */
    int line();

    /**
     * The column number associated with this position.
     * @return The column number associated with this position.
     */
    int col();

    /**
     * An empty position instance; this can be used to model <em>synthetic</em> program elements which are not
     * defined in any input file.
     */
    Position NO_POSITION = new Position() {
        @Override
        public Path path() {
            return null;
        }

        @Override
        public int line() {
            return 0;
        }

        @Override
        public int col() {
            return 0;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (obj instanceof Position pos) {
                return Objects.equals(path(), pos.path()) &&
                    Objects.equals(line(), pos.line()) &&
                    Objects.equals(col(), pos.col());
            }
            return false;
        }

        @Override
        public int hashCode() {
            return 0;
        }

        @Override
        public String toString() {
            return "NO_POSITION";
        }
    };
}
