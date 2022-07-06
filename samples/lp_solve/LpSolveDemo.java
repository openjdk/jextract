/*
 * Copyright (c) 2022, Oracle and/or its affiliates. All rights reserved.
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

import java.lang.foreign.*;
import static net.sourceforge.lpsolve.lp_lib_h.*;
import static java.lang.foreign.MemorySegment.NULL;

// This is port of C example from http://web.mit.edu/lpsolve/doc/

class LpSolveDemo {
    private static final byte TRUE = 1;
    private static final byte FALSE = 0;

    public static void main(String[] args) {
        int Ncol = 2; // two variables in the model
        var lp = make_lp(0, Ncol);
        if (lp == NULL) {
             System.err.println("make_lp returns NULL");
             return;
        }

        try (var session = NativeArena.openConfined()) {
            var colno = session.allocateArray(C_INT, Ncol);
            var row = session.allocateArray(C_DOUBLE, Ncol);

            // makes building the model faster if it is done rows by row
            set_add_rowmode(lp, TRUE);

            // construct first row (120 x + 210 y <= 15000)
            int j = 0;
            colno.setAtIndex(C_INT, j, 1);
            row.setAtIndex(C_DOUBLE, j, 120);
            j++;
            colno.setAtIndex(C_INT, j, 2);
            row.setAtIndex(C_DOUBLE, j, 210);
            j++;
            add_constraintex(lp, j, row, colno, LE(), 15000);

            // construct second row (110 x + 30 y <= 4000)
            j = 0;
            colno.setAtIndex(C_INT, j, 1);
            row.setAtIndex(C_DOUBLE, j, 110);
            j++;
            colno.setAtIndex(C_INT, j, 2);
            row.setAtIndex(C_DOUBLE, j, 30);
            j++;
            add_constraintex(lp, j, row, colno, LE(), 4000);

            // construct third row (x + y <= 75)
            j = 0;
            colno.setAtIndex(C_INT, j, 1);
            row.setAtIndex(C_DOUBLE, j, 1);
            j++;
            colno.setAtIndex(C_INT, j, 2);
            row.setAtIndex(C_DOUBLE, j, 1);
            j++;
            add_constraintex(lp, j, row, colno, LE(), 75);

            // rowmode should be turned off again when done building the model
            set_add_rowmode(lp, FALSE);

            // set the objective function (143 x + 60 y)
            j = 0;
            colno.setAtIndex(C_INT, j, 1);
            row.setAtIndex(C_DOUBLE, j, 143);
            j++;
            colno.setAtIndex(C_INT, j, 2);
            row.setAtIndex(C_DOUBLE, j, 60);
            j++;

            // set the objective in lpsolve
            set_obj_fnex(lp, j, row, colno);

            // set the object direction to maximize
            set_maxim(lp);

            // I only want to see important messages on screen while solving
            set_verbose(lp, IMPORTANT());

            // Now let lpsolve calculate a solution
            int ret = solve(lp);
            if (ret != OPTIMAL()) {
                System.err.println("cannot find optimal solution!");
                return;
            }

            /*
             * Objective value: 6315.625000
             * x0 = 21.875000
             * x1 = 53.125000
             */
            System.out.println("Objective value: " + get_objective(lp));

            // variable values
            get_variables(lp, row);
            for(j = 0; j < Ncol; j++) {
                System.out.println("x" + j + " = " + row.getAtIndex(C_DOUBLE, j));
            }
        } finally {
            delete_lp(lp);
        }
    }
}
