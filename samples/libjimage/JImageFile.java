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

import java.lang.foreign.NativeArena;
import java.lang.foreign.SegmentAllocator;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.openjdk.*;
import static java.lang.foreign.MemorySegment.NULL;
import static org.openjdk.jimage_h.*;

public class JImageFile {
    public static void main(String[] args) {
        String javaHome = System.getProperty("java.home");
        Path modPath = Paths.get(javaHome + "/lib/modules");
        if (!Files.exists(modPath)) {
           System.err.println(modPath + " not found, please check if your java.home");
           return;
        }
        try (var session = NativeArena.openConfined()) {
            var jintResPtr = session.allocate(jint);
            var moduleFilePath = session.allocateUtf8String(javaHome + "/lib/modules");
            var jimageFile = JIMAGE_Open(moduleFilePath, jintResPtr);

            if (jimageFile == NULL) {
                System.err.println("JIMAGE_Open failed to open " + modPath);
                return;
            }
            var mod = JIMAGE_PackageToModule(jimageFile,
                session.allocateUtf8String("java/util"));
            System.out.println(mod);

            // const char* module_name, const char* version, const char* package,
            // const char* name, const char* extension, void* arg

            var visitor = JImageResourceVisitor_t.allocate(
                (jimage, module_name, version, package_name, name, extension, arg) -> {
                   System.out.println("module " + module_name.getUtf8String(0));
                   System.out.println("package " + package_name.getUtf8String(0));
                   System.out.println("name " + name.getUtf8String(0));
                   return 1;
                }, session);

            JIMAGE_ResourceIterator(jimageFile, visitor, NULL);

            JIMAGE_Close(jimageFile);
        }
    }
}
