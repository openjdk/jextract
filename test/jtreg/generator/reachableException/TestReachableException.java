/*
 * Copyright (c) 2025, Oracle and/or its affiliates. All rights reserved.
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

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.util.concurrent.*;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

import test.jextract.reachableException.*;

/*
 * @test
 * @library /lib
 * @build testlib.TestUtils
 * @run main/othervm JtregJextract -t test.jextract.reachableException reachableException.h
 * @build TestReachableException
 * @run junit/othervm --enable-native-access=ALL-UNNAMED TestReachableException
 */

public class TestReachableException {

    private Arena confinedArena;
    private MemorySegment callbackSegment;

    @BeforeEach
    public void setup() {
        confinedArena = Arena.ofConfined();
        callbackSegment = simple_callback.allocate(value ->
                System.out.println("Value: " + value), confinedArena);
    }

    @AfterEach
    public void cleanup() {
        if (confinedArena != null && confinedArena.scope().isAlive()) {
            confinedArena.close();
        }
    }

    @Test
    void testWrongThreadAccess() throws Exception {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        try {
            Future<Throwable> future = executor.submit(() -> {
                try {
                    simple_callback.invoke(callbackSegment, 42);
                    return null;
                } catch (Throwable t) {
                    return t;
                }
            });

            Throwable thrown = future.get(5, TimeUnit.SECONDS);

            assertNotNull(thrown, "Expected an exception when accessing from wrong thread");
            assertInstanceOf(WrongThreadException.class, thrown,
                    "Expected WrongThreadException but got: " + thrown.getClass().getName());

        } finally {
            executor.shutdown();
        }
    }

    @Test
    void testSameThreadAccess() {
        assertDoesNotThrow(() -> {
            simple_callback.invoke(callbackSegment, 42);
        }, "No exception should be thrown when accessing from the same thread");
    }

    @Test
    void testExceptionType() throws Exception {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        try {
            Future<Class<?>> future = executor.submit(() -> {
                try {
                    simple_callback.invoke(callbackSegment, 42);
                    return null;
                } catch (Throwable t) {
                    return t.getClass();
                }
            });

            Class<?> exceptionClass = future.get(5, TimeUnit.SECONDS);
            assertEquals(WrongThreadException.class, exceptionClass,
                    "Exception should be WrongThreadException, not wrapped in AssertionError");

        } finally {
            executor.shutdown();
        }
    }
}
