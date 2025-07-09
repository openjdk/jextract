/*
 * Copyright (c) 2025, Oracle and/or its affiliates. All rights reserved.
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

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Stream;
import java.lang.classfile.ClassFile;
import java.lang.classfile.ClassModel;
import java.lang.classfile.CodeModel;
import java.lang.classfile.MethodModel;
import java.lang.classfile.instruction.InvokeInstruction;
import java.lang.classfile.instruction.FieldInstruction;

/**
 * Scans a directory of compiled client class files and lists all native
 * symbols referenced and writes the sorted, unique list of symbols into an output file.
 * Requires JDK 24+ (java.lang.classfile API).
 *
 * Usage:
 *   java --enable-preview NativeSymbolLister <clientClasses-dir> <output-file>
 */
public class NativeSymbolLister {
    public static void main(String[] args) throws IOException {
        if (args.length != 2) {
            System.err.println("Usage: java NativeSymbolLister <clientClasses-dir> <output-file>");
            System.exit(1);
        }
        Path dir = Paths.get(args[0]);
        Path output = Paths.get(args[1]);
        if (!Files.isDirectory(dir)) {
            System.err.println("Not a directory: " + dir);
            System.exit(1);
        }

        Set<String> symbols = new TreeSet<>();
        try (Stream<Path> paths = Files.walk(dir)) {
            paths.filter(p -> p.toString().endsWith(".class"))
                    .forEach(p -> {
                        try {
                            byte[] bytes = Files.readAllBytes(p);
                            symbols.addAll(extractSymbols(bytes));
                        } catch (IOException e) {
                            System.err.println("Failed to read " + p + ": " + e.getMessage());
                        }
                    });
        }

        try (BufferedWriter writer = Files.newBufferedWriter(output, StandardCharsets.UTF_8)) {
            for (String sym : symbols) {
                writer.write(sym);
                writer.newLine();
            }
        }

        System.out.println("Symbols written to " + output + " (" + symbols.size() + " entries)");
    }

    private static Set<String> extractSymbols(byte[] classBytes) {
        Set<String> set = new HashSet<>();
        ClassModel cm = ClassFile.of().parse(classBytes);
        cm.elementStream()
                .flatMap(ce -> ce instanceof MethodModel mm
                        ? mm.elementStream()
                        .flatMap(me -> me instanceof CodeModel code
                                ? code.elementStream()
                                : Stream.empty())
                        : Stream.empty())
                .forEach(elem -> {
                    if (elem instanceof InvokeInstruction inv) {
                        String name = inv.name().stringValue();
                        if (!"<init>".equals(name) && !"<clinit>".equals(name)) {
                            set.add(name);
                        }
                    } else if (elem instanceof FieldInstruction fld) {
                        set.add(fld.name().stringValue());
                    }
                });
        return set;
    }
}
