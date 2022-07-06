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
import org.unix.*;
import static java.lang.foreign.MemorySegment.NULL;
import static org.unix.libproc_h.*;

public class LibprocMain {
    private static final int NAME_BUF_MAX = 256;

    public static void main(String[] args) {
        try (var session = NativeArena.openConfined()) {
            // get the number of processes
            int numPids = proc_listallpids(NULL, 0);
            // allocate an array
            var pids = session.allocateArray(C_INT, numPids);
            // list all the pids into the native array
            proc_listallpids(pids, numPids);
            // convert native array to java array
            int[] jpids = pids.toArray(C_INT);
            // buffer for process name
            var nameBuf = session.allocateArray(C_CHAR, NAME_BUF_MAX);
            for (int i = 0; i < jpids.length; i++) {
                int pid = jpids[i];
                // get the process name
                proc_name(pid, nameBuf, NAME_BUF_MAX);
                String procName = nameBuf.getUtf8String(0);
                // print pid and process name
                System.out.printf("%d %s\n", pid, procName);
            }
        }
    }
}

