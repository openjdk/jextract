package org.openjdk.jextract.clang;

import java.lang.foreign.MemoryAddress;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.MemorySession;
import java.lang.foreign.SegmentAllocator;

/**
 * This class models a libclang entity that has an explicit lifecycle (e.g. TranslationUnit, Index).
 * This class starts a new confined session and an arena allocator; this arena allocator is used by all
 * the abstractions "owned" by this disposable. For instance, as a CXCursor's lifetime is the same as that of
 * the CXTranslationUnit's lifetime, cursors are allocated inside the translation unit's lifetime.
 */
public abstract class ClangDisposable implements AutoCloseable {
    protected final MemorySegment ptr;
    protected final MemorySession session;
    protected final SegmentAllocator arena;

    public ClangDisposable(MemoryAddress ptr, long size, Runnable cleanup) {
        this.session = MemorySession.openConfined();
        this.ptr = MemorySegment.ofAddress(ptr, size, session).asReadOnly();
        session.addCloseAction(cleanup);
        this.arena = SegmentAllocator.newNativeArena(session);
    }

    public ClangDisposable(MemoryAddress ptr, Runnable cleanup) {
        this(ptr, 0, cleanup);
    }

    @Override
    public void close() {
        session.close();
    }

    MemorySession session() {
        return session;
    }

    SegmentAllocator arena() {
        return arena;
    }

    /**
     * An libclang entity owned by some libclang disposable entity. Entities modelled by this class
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
