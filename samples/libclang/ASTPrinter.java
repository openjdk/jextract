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
import static java.lang.foreign.MemorySegment.NULL;
import static org.llvm.clang.Index_h.*;
import org.llvm.clang.*;

public class ASTPrinter {
    private static String asJavaString(MemorySegment clangStr) {
        String str = clang_getCString(clangStr).getUtf8String(0);
        clang_disposeString(clangStr);
        return str;
    }

    public static void main(String[] args) {
        if (args.length == 0) {
            System.err.println("java ASTPrinter <C source or header>");
            System.exit(1);
        }

        try (var session = NativeArena.openConfined()) {
            // parse the C header/source passed from the command line
            var index = clang_createIndex(0, 0);
            var tu = clang_parseTranslationUnit(index, session.allocateUtf8String(args[0]),
                    NULL, 0, NULL, 0, CXTranslationUnit_None());
            // array trick to update within lambda
            var level = new int[1];
            var visitor = new MemorySegment[1];

            // clang Cursor visitor callback
            visitor[0] = CXCursorVisitor.allocate((cursor, parent, data) -> {
                var kind = clang_getCursorKind(cursor);
                var name = asJavaString(clang_getCursorSpelling(session, cursor));
                var kindName = asJavaString(clang_getCursorKindSpelling(session, kind));
                System.out.printf("%s %s %s", " ".repeat(level[0]), kindName, name);
                var type = clang_getCursorType(session, cursor);
                if (CXType.kind$get(type) != CXType_Invalid()) {
                    var typeName = asJavaString(clang_getTypeSpelling(session, type));
                    System.out.printf(" <%s>", typeName);
                }
                System.out.println();

                // visit children
                level[0]++;
                clang_visitChildren(cursor, visitor[0], NULL);
                level[0]--;

                return CXChildVisit_Continue();
            }, session);

            // get the AST root and visit it
            var root = clang_getTranslationUnitCursor(session, tu);
            clang_visitChildren(root, visitor[0], NULL);

            clang_disposeTranslationUnit(tu);
            clang_disposeIndex(index);
        }
    }
}
