/*
 *  Copyright (c) 2022, Oracle and/or its affiliates. All rights reserved.
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

package org.openjdk.jextract.clang;

import java.lang.foreign.MemorySegment;
import java.lang.foreign.MemorySession;
import java.lang.foreign.SegmentAllocator;

/**
 * This class models a libclang entity that has an explicit lifecycle (e.g. TranslationUnit, Index).
 * This class starts a new confined session and an arena allocator; this arena allocator is used by all
 * the abstractions "owned" by this disposable. For instance, as a CXCursor's lifetime is the same as that of
 * the CXTranslationUnit's lifetime, cursors are allocated inside the translation unit's lifetime.
 */
public abstract class ClangDisposable implements SegmentAllocator, AutoCloseable {
    protected final MemorySegment ptr;
    protected final MemorySession session;
    protected final SegmentAllocator arena;

    public ClangDisposable(MemorySegment ptr, long size, Runnable cleanup) {
        this.session = MemorySession.openConfined();
        this.ptr = MemorySegment.ofAddress(ptr.address(), size, session).asReadOnly();
        session.addCloseAction(cleanup);
        this.arena = SegmentAllocator.newNativeArena(session);
    }

    public ClangDisposable(MemorySegment ptr, Runnable cleanup) {
        this(ptr, 0, cleanup);
    }

    @Override
    public void close() {
        session.close();
    }

    @Override
    public MemorySegment allocate(long bytesSize, long bytesAlignment) {
        return arena.allocate(bytesSize, bytesAlignment);
    }

    /**
     * A libclang entity owned by some libclang disposable entity. Entities modelled by this class
     * do not have their own session; instead, they piggyback on the session of their owner.
     */
    static class Owned {
        final MemorySegment segment;
        final ClangDisposable owner;

        protected Owned(MemorySegment segment, ClangDisposable owner) {
            this.segment = segment;
            this.owner = owner;
        }
    }
}
