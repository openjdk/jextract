// Generated by jextract

package org.openjdk;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.nio.ByteOrder;
import java.lang.foreign.*;
import static java.lang.foreign.ValueLayout.*;

/**
 * {@snippet lang=c :
 * JImageResourceVisitor_t visitor
 * }
 */
public interface JIMAGE_ResourceIterator$visitor {

    int apply(MemorySegment _x0, MemorySegment _x1, MemorySegment _x2, MemorySegment _x3, MemorySegment _x4, MemorySegment _x5, MemorySegment _x6);

    FunctionDescriptor $DESC = FunctionDescriptor.of(
        jimage_h.C_INT,
        jimage_h.C_POINTER,
        jimage_h.C_POINTER,
        jimage_h.C_POINTER,
        jimage_h.C_POINTER,
        jimage_h.C_POINTER,
        jimage_h.C_POINTER,
        jimage_h.C_POINTER
    );

    MethodHandle UP$MH = jimage_h.upcallHandle(JIMAGE_ResourceIterator$visitor.class, "apply", $DESC);

    static MemorySegment allocate(JIMAGE_ResourceIterator$visitor fi, Arena scope) {
        return Linker.nativeLinker().upcallStub(UP$MH.bindTo(fi), $DESC, scope);
    }

    MethodHandle DOWN$MH = Linker.nativeLinker().downcallHandle($DESC);

    static JIMAGE_ResourceIterator$visitor ofAddress(MemorySegment addr, Arena arena) {
        MemorySegment symbol = addr.reinterpret(arena, null);
        return (MemorySegment __x0, MemorySegment __x1, MemorySegment __x2, MemorySegment __x3, MemorySegment __x4, MemorySegment __x5, MemorySegment __x6) -> {
            try {
                return (int) DOWN$MH.invokeExact(symbol, __x0, __x1, __x2, __x3, __x4, __x5, __x6);
            } catch (Throwable ex$) {
                throw new AssertionError("should not reach here", ex$);
            }
        };
    }
}

