// Generated by jextract

package org.openjdk;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.VarHandle;
import java.nio.ByteOrder;
import java.lang.foreign.*;
import static java.lang.foreign.ValueLayout.*;
public interface JImagePackageToModule_t {

    java.lang.foreign.Addressable apply(java.lang.foreign.MemoryAddress jimage, java.lang.foreign.MemoryAddress package_name);
    static MemorySegment allocate(JImagePackageToModule_t fi, MemorySession session) {
        return RuntimeHelper.upcallStub(JImagePackageToModule_t.class, fi, constants$1.JImagePackageToModule_t$FUNC, "(Ljava/lang/foreign/MemoryAddress;Ljava/lang/foreign/MemoryAddress;)Ljava/lang/foreign/Addressable;", session);
    }
    static JImagePackageToModule_t ofAddress(MemoryAddress addr, MemorySession session) {
        MemorySegment symbol = MemorySegment.ofAddress(addr, 0, session);
        return (java.lang.foreign.MemoryAddress _jimage, java.lang.foreign.MemoryAddress _package_name) -> {
            try {
                return (java.lang.foreign.Addressable)(java.lang.foreign.MemoryAddress)constants$1.JImagePackageToModule_t$MH.invokeExact((Addressable)symbol, (java.lang.foreign.Addressable)_jimage, (java.lang.foreign.Addressable)_package_name);
            } catch (Throwable ex$) {
                throw new AssertionError("should not reach here", ex$);
            }
        };
    }
}


