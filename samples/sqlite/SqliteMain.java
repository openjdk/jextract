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

import jdk.incubator.foreign.MemoryAddress;
import jdk.incubator.foreign.MemorySegment;
import jdk.incubator.foreign.ResourceScope;
import jdk.incubator.foreign.SegmentAllocator;
import org.sqlite.*;
import static jdk.incubator.foreign.MemoryAddress.NULL;
import static org.sqlite.sqlite3_h.*;

public class SqliteMain {
   public static void main(String[] args) throws Exception {
        try (var scope = ResourceScope.newConfinedScope()) {
            var allocator = SegmentAllocator.newNativeArena(scope);
            // char** errMsgPtrPtr;
            var errMsgPtrPtr = allocator.allocate(C_POINTER);

            // sqlite3** dbPtrPtr;
            var dbPtrPtr = allocator.allocate(C_POINTER);

            int rc = sqlite3_open(allocator.allocateUtf8String("employee.db"), dbPtrPtr);
            if (rc != 0) {
                System.err.println("sqlite3_open failed: " + rc);
                return;
            } else {
                System.out.println("employee db opened");
            }

            // sqlite3* dbPtr;
            var dbPtr = dbPtrPtr.get(C_POINTER, 0);

            // create a new table
            var sql = allocator.allocateUtf8String(
                "CREATE TABLE EMPLOYEE ("  +
                "  ID INT PRIMARY KEY NOT NULL," +
                "  NAME TEXT NOT NULL,"    +
                "  SALARY REAL NOT NULL )");

            rc = sqlite3_exec(dbPtr, sql, NULL, NULL, errMsgPtrPtr);

            if (rc != 0) {
                System.err.println("sqlite3_exec failed: " + rc);
                System.err.println("SQL error: " + errMsgPtrPtr.get(C_POINTER, 0).getUtf8String(0));
                sqlite3_free(errMsgPtrPtr.get(C_POINTER, 0));
            } else {
                System.out.println("employee table created");
            }

            // insert two rows
            sql = allocator.allocateUtf8String(
                "INSERT INTO EMPLOYEE (ID,NAME,SALARY) " +
                    "VALUES (134, 'Xyz', 200000.0); " +
                "INSERT INTO EMPLOYEE (ID,NAME,SALARY) " +
                    "VALUES (333, 'Abc', 100000.0);"
            );
            rc = sqlite3_exec(dbPtr, sql, NULL, NULL, errMsgPtrPtr);

            if (rc != 0) {
                System.err.println("sqlite3_exec failed: " + rc);
                System.err.println("SQL error: " + errMsgPtrPtr.get(C_POINTER, 0).getUtf8String(0));
                sqlite3_free(errMsgPtrPtr.get(C_POINTER, 0));
            } else {
                System.out.println("rows inserted");
            }

            int[] rowNum = new int[1];
            // callback to print rows from SELECT query
            var callback = sqlite3_exec$callback.allocate((a, argc, argv, columnNames) -> {
                System.out.println("Row num: " + rowNum[0]++);
                System.out.println("numColumns = " + argc);
                var argv_seg = MemorySegment.ofAddress(argv, C_POINTER.byteSize() * argc, scope);
                var columnNames_seg = MemorySegment.ofAddress(columnNames, C_POINTER.byteSize() * argc, scope);
                for (int i = 0; i < argc; i++) {
                     String name = columnNames_seg.getAtIndex(C_POINTER, i).getUtf8String(0);
                     String value = argv_seg.getAtIndex(C_POINTER, i).getUtf8String(0);

                     System.out.printf("%s = %s\n", name, value);
                }
                return 0;
            }, scope);

            // select query
            sql = allocator.allocateUtf8String("SELECT * FROM EMPLOYEE");
            rc = sqlite3_exec(dbPtr, sql, callback, NULL, errMsgPtrPtr);

            if (rc != 0) {
                System.err.println("sqlite3_exec failed: " + rc);
                System.err.println("SQL error: " + errMsgPtrPtr.get(C_POINTER, 0).getUtf8String(0));
                sqlite3_free(errMsgPtrPtr.get(C_POINTER, 0));
            } else {
                System.out.println("done");
            }

            sqlite3_close(dbPtr);
        }
    }
}

