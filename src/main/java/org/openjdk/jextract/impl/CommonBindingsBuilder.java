/*
 * Copyright (c) 2025, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
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

package org.openjdk.jextract.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Moving common layouts and static methods not relevant to component areas into a centralized location
 */

final class CommonBindingsBuilder extends ClassSourceBuilder {
    private CommonBindingsBuilder(SourceFileBuilder builder, String className, String runtimeHelperName) {
        super(builder, "public final", Kind.CLASS, className, null, null, runtimeHelperName);
    }

    public static void generate(SourceFileBuilder builder, String className, String runtimeHelperName) {
        CommonBindingsBuilder cbd = new CommonBindingsBuilder(builder, className, runtimeHelperName);
        cbd.appendBlankLine();
        cbd.classBegin();
        cbd.emitPrivateConstructor();
        cbd.appendBlankLine();
        cbd.emitPrimitiveTypes();
        cbd.classEnd();
    }

    public static void generate(SourceFileBuilder builder, String className, String runtimeHelperName, List<Options.Library> libs, Boolean useSystemLoadLibrary) {
        CommonBindingsBuilder cbd = new CommonBindingsBuilder(builder, className, runtimeHelperName);
        cbd.appendBlankLine();
        cbd.classBegin();
        cbd.emitPrivateConstructor();
        cbd.emitCommonFinalFields();
        cbd.emitFirstHeaderPreamble(libs, useSystemLoadLibrary);
        cbd.appendBlankLine();
        cbd.emitRuntimeHelperMethods();
        cbd.classEnd();
    }

    // emit basic primitive types
    private void emitPrimitiveTypes() {
        appendIndentedLines("""
            public static final ValueLayout.OfBoolean C_BOOL = (ValueLayout.OfBoolean) Linker.nativeLinker().canonicalLayouts().get("bool");
            public static final ValueLayout.OfByte C_CHAR =(ValueLayout.OfByte)Linker.nativeLinker().canonicalLayouts().get("char");
            public static final ValueLayout.OfShort C_SHORT = (ValueLayout.OfShort) Linker.nativeLinker().canonicalLayouts().get("short");
            public static final ValueLayout.OfInt C_INT = (ValueLayout.OfInt) Linker.nativeLinker().canonicalLayouts().get("int");
            public static final ValueLayout.OfLong C_LONG_LONG = (ValueLayout.OfLong) Linker.nativeLinker().canonicalLayouts().get("long long");
            public static final ValueLayout.OfFloat C_FLOAT = (ValueLayout.OfFloat) Linker.nativeLinker().canonicalLayouts().get("float");
            public static final ValueLayout.OfDouble C_DOUBLE = (ValueLayout.OfDouble) Linker.nativeLinker().canonicalLayouts().get("double");
            public static final AddressLayout C_POINTER = ((AddressLayout) Linker.nativeLinker().canonicalLayouts().get("void*"))
                    .withTargetLayout(MemoryLayout.sequenceLayout(java.lang.Long.MAX_VALUE, C_CHAR));
            """);
        if (TypeImpl.IS_WINDOWS) {
            appendIndentedLines("public static final ValueLayout.OfInt C_LONG = (ValueLayout.OfInt) Linker.nativeLinker().canonicalLayouts().get(\"long\");");
            appendIndentedLines("public static final ValueLayout.OfDouble C_LONG_DOUBLE = (ValueLayout.OfDouble) Linker.nativeLinker().canonicalLayouts().get(\"double\");");
        } else {
            appendIndentedLines("public static final ValueLayout.OfLong C_LONG = (ValueLayout.OfLong) Linker.nativeLinker().canonicalLayouts().get(\"long\");");
        }
    }

    void emitFirstHeaderPreamble(List<Options.Library> libraries, boolean useSystemLoadLibrary) {
        List<String> lookups = new ArrayList<>();
        // if legacy library loading is selected, load libraries (if any) into current loader
        if (useSystemLoadLibrary) {
            appendBlankLine();
            appendIndentedLines("""

                static {
                """);
            incrAlign();
            for (Options.Library lib : libraries) {
                String method = lib.specKind() == Options.Library.SpecKind.PATH ? "load" : "loadLibrary";
                appendIndentedLines("System.%1$s(\"%2$s\");", method, lib.toQuotedName());
            }
            decrAlign();
            appendIndentedLines("""
                }
                """);
        } else {
            // otherwise, add a library lookup per library (if any)
            libraries.stream() // add library lookups (if any)
                    .map(l -> l.specKind() == Options.Library.SpecKind.PATH ?
                            String.format("SymbolLookup.libraryLookup(\"%1$s\", LIBRARY_ARENA)", l.toQuotedName()) :
                            String.format("SymbolLookup.libraryLookup(System.mapLibraryName(\"%1$s\"), LIBRARY_ARENA)", l.toQuotedName()))
                    .collect(Collectors.toCollection(() -> lookups));
        }

        lookups.add("SymbolLookup.loaderLookup()"); // fallback to loader lookup
        lookups.add("Linker.nativeLinker().defaultLookup()"); // fallback to native lookup

        // wrap all lookups (but the first) with ".or(...)"
        List<String> lookupCalls = new ArrayList<>();
        boolean isFirst = true;
        for (String lookup : lookups) {
            lookupCalls.add(isFirst ? lookup : String.format(".or(%1$s)", lookup));
            isFirst = false;
        }

        // chain all the calls together into a combined symbol lookup
        appendBlankLine();
        appendIndentedLines(lookupCalls.stream()
                .collect(Collectors.joining(String.format("\n%1$s", indentString(2)), "static final SymbolLookup SYMBOL_LOOKUP = ", ";")));
    }

    private void emitCommonFinalFields() {
        appendIndentedLines("""
            static final Arena LIBRARY_ARENA = Arena.ofAuto();
            static final boolean TRACE_DOWNCALLS = Boolean.getBoolean("jextract.trace.downcalls");
                        
                """);
    }
    private void emitRuntimeHelperMethods() {
        appendIndentedLines("""
            static void traceDowncall(String name, Object... args) {
                 String traceArgs = Arrays.stream(args)
                               .map(Object::toString)
                               .collect(Collectors.joining(", "));
                 System.out.printf("%s(%s)\\n", name, traceArgs);
            }

            static MemorySegment findOrThrow(String symbol) {
                return SYMBOL_LOOKUP.findOrThrow(symbol);
            }

            static MethodHandle upcallHandle(Class<?> fi, String name, FunctionDescriptor fdesc) {
                try {
                    return MethodHandles.lookup().findVirtual(fi, name, fdesc.toMethodType());
                } catch (ReflectiveOperationException ex) {
                    throw new AssertionError(ex);
                }
            }

            static MemoryLayout align(MemoryLayout layout, long align) {
                return switch (layout) {
                    case PaddingLayout p -> p;
                    case ValueLayout v -> v.withByteAlignment(align);
                    case GroupLayout g -> {
                        MemoryLayout[] alignedMembers = g.memberLayouts().stream()
                                .map(m -> align(m, align)).toArray(MemoryLayout[]::new);
                        yield g instanceof StructLayout ?
                                MemoryLayout.structLayout(alignedMembers) : MemoryLayout.unionLayout(alignedMembers);
                    }
                    case SequenceLayout s -> MemoryLayout.sequenceLayout(s.elementCount(), align(s.elementLayout(), align));
                };
            }
            """);
    }
}
