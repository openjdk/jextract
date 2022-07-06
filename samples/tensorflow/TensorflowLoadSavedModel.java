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

import java.lang.foreign.*;
import static java.lang.foreign.MemorySegment.*;
import static org.tensorflow.c_api_h.*;
import org.tensorflow.*;

// simple program that loads saved model and prints basic info on operations in it

public class TensorflowLoadSavedModel {
    public static void main(String... args) throws Exception {
        System.out.println("TensorFlow C library version: " + TF_Version().getUtf8String(0));

        if (args.length == 0) {
            System.err.println("java TensorflowLoadSavedModel <saved model dir>");
            System.exit(1);
        }

        try (var session = NativeArena.openConfined()) {
            var allocator = SegmentAllocator.bumpAllocator(session);
            var graph = TF_NewGraph();
            var status = TF_NewStatus();
            var sessionOpts = TF_NewSessionOptions();

            var savedModelDir = allocator.allocateUtf8String(args[0]);
            var tags = allocator.allocate(C_POINTER, allocator.allocateUtf8String("serve"));
            var tf_session = TF_LoadSessionFromSavedModel(sessionOpts, NULL, savedModelDir, tags, 1, graph, NULL, status);

            if (TF_GetCode(status) != TF_OK()) {
                System.err.printf("cannot load session from saved model: %s\n",
                    TF_Message(status).getUtf8String(0));
            } else {
                System.err.println("load session from saved model works!");
            }

            // print operations
            var size = allocator.allocate(C_LONG_LONG);
            var operation = NULL;
            while (!(operation = TF_GraphNextOperation(graph, size)).equals(NULL)) {
                System.out.printf("%s : %s\n",
                    TF_OperationName(operation).getUtf8String(0),
                    TF_OperationOpType(operation).getUtf8String(0));
            }

            TF_DeleteGraph(graph);
            TF_DeleteSession(tf_session, status);
            TF_DeleteSessionOptions(sessionOpts);
            TF_DeleteStatus(status);
        }
    }
}
