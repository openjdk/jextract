// Generated by jextract

package org.openjdk;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.VarHandle;
import java.nio.ByteOrder;
import java.lang.foreign.*;
import static java.lang.foreign.ValueLayout.*;
public interface JImageGetResource_t {

    long apply(java.lang.foreign.MemoryAddress jimage, long location, java.lang.foreign.MemoryAddress buffer, long size);
    static MemorySegment allocate(JImageGetResource_t fi, MemorySession session) {
        return RuntimeHelper.upcallStub(JImageGetResource_t.class, fi, constants$2.JImageGetResource_t$FUNC, "(Ljava/lang/foreign/MemoryAddress;JLjava/lang/foreign/MemoryAddress;J)J", session);
    }
    static JImageGetResource_t ofAddress(MemoryAddress addr, MemorySession session) {
        MemorySegment symbol = MemorySegment.ofAddress(addr, 0, session);
        return (java.lang.foreign.MemoryAddress _jimage, long _location, java.lang.foreign.MemoryAddress _buffer, long _size) -> {
            try {
                return (long)constants$2.JImageGetResource_t$MH.invokeExact((Addressable)symbol, (java.lang.foreign.Addressable)_jimage, _location, (java.lang.foreign.Addressable)_buffer, _size);
            } catch (Throwable ex$) {
                throw new AssertionError("should not reach here", ex$);
            }
        };
    }
}


