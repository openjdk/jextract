/*
 * Copyright (c) 2020, Oracle and/or its affiliates. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 *   - Redistributions of source code must retain the above copyright
 *     notice, this list of conditions and the following disclaimer.
 *
 *   - Redistributions in binary form must reproduce the above copyright
 *     notice, this list of conditions and the following disclaimer in the
 *     documentation and/or other materials provided with the distribution.
 *
 *   - Neither the name of Oracle nor the names of its
 *     contributors may be used to endorse or promote products derived
 *     from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS
 * IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO,
 * THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED.  IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

import java.lang.foreign.MemorySegment;
import java.lang.foreign.NativeArena;
import java.lang.foreign.SegmentAllocator;
import lapack.*;
import static lapack.lapacke_h.*;

public class TestLapack {
    public static void main(String[] args) {

        /* Locals */
        try (var session = NativeArena.openConfined()) {
            var A = session.allocateArray(C_DOUBLE,
                    1, 2, 3, 4, 5, 1, 3, 5, 2, 4, 1, 4, 2, 5, 3
            );
            var b = session.allocateArray(C_DOUBLE,
                    -10, 12, 14, 16, 18, -3, 14, 12, 16, 16
            );
            int info, m, n, lda, ldb, nrhs;

            /* Initialization */
            m = 5;
            n = 3;
            nrhs = 2;
            lda = 5;
            ldb = 5;

            /* Print Entry Matrix */
            print_matrix_colmajor("Entry Matrix A", m, n, A, lda );
            /* Print Right Rand Side */
            print_matrix_colmajor("Right Hand Side b", n, nrhs, b, ldb );
            System.out.println();

            /* Executable statements */
            //            printf( "LAPACKE_dgels (col-major, high-level) Example Program Results\n" );
            /* Solve least squares problem*/
            info = LAPACKE_dgels(LAPACK_COL_MAJOR(), (byte)'N', m, n, nrhs, A, lda, b, ldb);

            /* Print Solution */
            print_matrix_colmajor("Solution", n, nrhs, b, ldb );
            System.out.println();
            System.exit(info);
        }
    }

    static void print_matrix_colmajor(String msg, int m, int n, MemorySegment mat, int ldm) {
        int i, j;
        System.out.printf("\n %s\n", msg);

        for( i = 0; i < m; i++ ) {
            for( j = 0; j < n; j++ ) System.out.printf(" %6.2f", mat.getAtIndex(C_DOUBLE, i+j*ldm));
            System.out.printf( "\n" );
        }
    }
}

