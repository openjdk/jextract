/*
 * Copyright (c) 2021, Oracle and/or its affiliates. All rights reserved.
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

import java.lang.invoke.*;
import java.util.Optional;
import jdk.incubator.foreign.*;
import org.unix.dlfcn_h.*;
import static org.unix.dlfcn_h.*;

public class Dlopen {
    // implementation of SymbolLookup that loads a given shared object using dlopen
    // and looks up symbols using dlsym
    private static SymbolLookup lookup(String libraryName, ResourceScope scope) {
        try (ResourceScope openScope = ResourceScope.newConfinedScope()) {
            var openScopeAllocator = SegmentAllocator.nativeAllocator(openScope);
            var handle = dlopen(openScopeAllocator.allocateUtf8String(libraryName), RTLD_LOCAL());
            if (handle == MemoryAddress.NULL) {
                throw new IllegalArgumentException("Cannot find library: " + libraryName);
            }
            scope.addCloseAction(() -> dlclose(handle));
            return name -> {
                var allocator = SegmentAllocator.nativeAllocator(scope);
                var addr = dlsym(handle, allocator.allocateUtf8String(name));
                return addr == MemoryAddress.NULL ?
                            Optional.empty() : Optional.of(NativeSymbol.ofAddress(name, addr, scope));
            };
        }
    }

    public static void main(String[] args) throws Throwable {
        var arg = args.length > 0? args[0] : "Java";
        var libName = "libhello.dylib";
        try (var scope = ResourceScope.newConfinedScope()) {
            var allocator = SegmentAllocator.nativeAllocator(scope);
            var symLookup = lookup(libName, scope);

            var linker = CLinker.systemCLinker();
            // get method handle for a function from helloLIb
            var greetingMH = linker.downcallHandle(
                symLookup.lookup("greeting").get(),
                FunctionDescriptor.ofVoid(ValueLayout.ADDRESS));

            // invoke a function from helloLib
            greetingMH.invoke(allocator.allocateUtf8String(arg));
        }
    }
}
