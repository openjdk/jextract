/*
 * Copyright (c) 2020, 2024, Oracle and/or its affiliates. All rights reserved.
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

import java.lang.foreign.MemorySegment;
import java.lang.foreign.SegmentAllocator;

import org.openjdk.jextract.Declaration;
import org.openjdk.jextract.Type;
import org.openjdk.jextract.impl.DeclarationImpl.JavaName;

import java.lang.invoke.MethodType;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.StringJoiner;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * A helper class to generate header interface class in source form.
 * After aggregating various constituents of a .java source, build
 * method is called to get overall generated source string.
 */
class HeaderFileBuilder extends ClassSourceBuilder {

    private final static String ASMLABEL = "AsmLabelAttr";

    private static final boolean isMacOSX =
            System.getProperty("os.name", "unknown").contains("OS X");

    private final Set<String> holderClassNames = new HashSet<>();

    HeaderFileBuilder(SourceFileBuilder builder, String className, String superName, String runtimeHelperName) {
        super(builder, "public", Kind.CLASS, className, superName, null, runtimeHelperName);
    }

    public void addVar(Declaration.Variable varTree) {
        String javaName = JavaName.getOrThrow(varTree);
        appendBlankLine();
        String holderClass = emitVarHolderClass(varTree, javaName);
        if (Utils.isArray(varTree.type()) || Utils.isStructOrUnion(varTree.type())) {
            emitGlobalSegmentGetter(holderClass, javaName, varTree, "Getter for variable:");
            emitGlobalSegmentSetter(holderClass, javaName, varTree, "Setter for variable:");
            int dims = Utils.dimensions(varTree.type()).size();
            if (dims > 0) {
                IndexList indexList = IndexList.of(dims);
                emitGlobalArrayGetter(holderClass, indexList, javaName, varTree, "Indexed getter for variable:");
                emitGlobalArraySetter(holderClass, indexList, javaName, varTree, "Indexed setter for variable:");
            }
        } else if (Utils.isPointer(varTree.type()) || Utils.isPrimitive(varTree.type())) {
            emitGlobalGetter(holderClass, javaName, varTree, "Getter for variable:");
            emitGlobalSetter(holderClass, javaName, varTree, "Setter for variable:");
        } else {
            throw new IllegalArgumentException("Tree type not handled: " + varTree.type());
        }
    }

    public void addFunction(Declaration.Function funcTree) {
        String nativeName = funcTree.name();
        boolean isVarargs = funcTree.type().varargs();
        boolean needsAllocator = Utils.isStructOrUnion(funcTree.type().returnType());
        List<String> parameterNames = funcTree.parameters().
                stream().
                map(JavaName::getOrThrow).
                toList();


        emitFunctionWrapper(JavaName.getOrThrow(funcTree), nativeName,
                needsAllocator, isVarargs, parameterNames, funcTree);
    }

    private String lookupName(Declaration decl) {
        var attrs = decl.getAttribute(Declaration.ClangAttributes.class);
        if (attrs.isPresent() && attrs.get().attributes().containsKey(ASMLABEL)) {
            String asmLabel = attrs.get().attributes().get(ASMLABEL).get(0);
            return isMacOSX ?
                    asmLabel.substring(1) : // skip leading "_"
                    asmLabel;
        } else {
            return decl.name();
        }
    }

    public void addConstant(Declaration.Constant constantTree) {
        Object value = constantTree.value();
        emitConstant(Utils.carrierFor(constantTree.type()), JavaName.getOrThrow(constantTree), value, constantTree);
    }

    // private generation

    private static List<String> finalizeParameterNames(List<String> parameterNames, boolean needsAllocator, boolean isVarArg) {
        List<String> result = new ArrayList<>();

        int i = 0;
        for (; i < parameterNames.size(); i++) {
            String name = parameterNames.get(i);
            if (name.isEmpty()) {
                name = "x" + i;
            }
            result.add(name);
        }

        if (isVarArg) {
            result.add("x" + i);
        }

        if (needsAllocator) {
            String allocatorName = "allocator";
            while (result.contains(allocatorName)) {
                allocatorName = "_" + allocatorName;
            }
            result.add(0, allocatorName);
        }

        return result;
    }

    private static String paramExprs(MethodType type, List<String> parameterNames, boolean isVarArg) {
        assert parameterNames.size() >= type.parameterCount();
        StringJoiner sb = new StringJoiner(", ");
        int i = 0;
        for (; i < type.parameterCount(); i++) {
            String pName = parameterNames.get(i);
            sb.add(type.parameterType(i).getSimpleName() + " " + pName);
        }

        if (isVarArg) {
            sb.add("Object... " + parameterNames.get(i));
        }

        return sb.toString();
    }

    private void emitFunctionWrapper(String javaName, String nativeName, boolean needsAllocator,
                                     boolean isVarArg, List<String> parameterNames, Declaration.Function decl) {
        MethodType declType = Utils.methodTypeFor(decl.type());
        List<String> finalParamNames = finalizeParameterNames(parameterNames, needsAllocator, isVarArg);
        if (needsAllocator) {
            declType = declType.insertParameterTypes(0, SegmentAllocator.class);
        }

        String retType = declType.returnType().getSimpleName();
        boolean isVoid = declType.returnType().equals(void.class);
        String returnNoCast = isVoid ? "" : "return ";
        String returnWithCast = isVoid ? "" : String.format("%s(%s)", returnNoCast, retType);
        String paramList = String.join(", ", finalParamNames);
        String traceArgList = paramList.isEmpty() ?
                String.format("\"%s\"", nativeName) :
                String.format("\"%s\", %s", nativeName, paramList);
        incrAlign();
        if (!isVarArg) {
            String holderClass = newHolderClassName(javaName);
            appendLines(String.format("""

                private static class %s {
                    public static final FunctionDescriptor DESC = %s;

                    public static final MemorySegment ADDR = %s.findOrThrow("%s");

                    public static final MethodHandle HANDLE = Linker.nativeLinker().downcallHandle(ADDR, DESC);
                }
                """,
                holderClass, functionDescriptorString(1, decl.type()), runtimeHelperName(), lookupName(decl)));
            appendBlankLine();
            emitDocComment(decl, "Function descriptor for:");
            appendLines(String.format("""
                public static FunctionDescriptor %s$descriptor() {
                    return %s.DESC;
                }
                """, javaName, holderClass));
            appendBlankLine();
            emitDocComment(decl, "Downcall method handle for:");
            appendLines(String.format("""
                public static MethodHandle %s$handle() {
                    return %s.HANDLE;
                }
                """,
                javaName, holderClass));
            appendBlankLine();
            emitDocComment(decl, "Address for:");
            appendLines(String.format("""
                public static MemorySegment %s$address() {
                    return %s.ADDR;
                }
                """,
                javaName, holderClass));
            appendBlankLine();
            emitDocComment(decl);
            appendLines(String.format("""
            public static %s %s(%s) {
                var mh$ = %s.HANDLE;
                try {
                    if (TRACE_DOWNCALLS) {
                        traceDowncall(%s);
                    }
                    %s mh$.invokeExact(%s);
                } catch (Throwable ex$) {
                   throw new AssertionError("should not reach here", ex$);
                }
            }
            """,
            retType, javaName, paramExprs(declType, finalParamNames, isVarArg),
            holderClass, traceArgList, returnWithCast, paramList));
        } else {
            String invokerClassName = newHolderClassName(javaName);
            String paramExprs = paramExprs(declType, finalParamNames, isVarArg);
            appendBlankLine();
            emitDocComment(decl, "Variadic invoker class for:");
            appendLines(String.format("""
                public static class %s {
                    private static final FunctionDescriptor BASE_DESC = %s;
                    private static final MemorySegment ADDR = %s.findOrThrow("%s");

                    private final MethodHandle handle;
                    private final FunctionDescriptor descriptor;
                    private final MethodHandle spreader;

                    private %s(MethodHandle handle, FunctionDescriptor descriptor, MethodHandle spreader) {
                        this.handle = handle;
                        this.descriptor = descriptor;
                        this.spreader = spreader;
                    }
                """,
                invokerClassName, functionDescriptorString(2, decl.type()), runtimeHelperName(), lookupName(decl), invokerClassName));
            incrAlign();
            appendBlankLine();
            emitDocComment(decl, "Variadic invoker factory for:");
            appendLines(String.format("""
                public static %s makeInvoker(MemoryLayout... layouts) {
                    FunctionDescriptor desc$ = BASE_DESC.appendArgumentLayouts(layouts);
                    Linker.Option fva$ = Linker.Option.firstVariadicArg(BASE_DESC.argumentLayouts().size());
                    var mh$ = Linker.nativeLinker().downcallHandle(ADDR, desc$, fva$);
                    var spreader$ = mh$.asSpreader(Object[].class, layouts.length);
                    return new %s(mh$, desc$, spreader$);
                }
                """,
                invokerClassName, invokerClassName));
            decrAlign();
            appendLines(String.format("""

                    /**
                     * {@return the address}
                     */
                    public static MemorySegment address() {
                        return ADDR;
                    }

                    /**
                     * {@return the specialized method handle}
                     */
                    public MethodHandle handle() {
                        return handle;
                    }

                    /**
                     * {@return the specialized descriptor}
                     */
                    public FunctionDescriptor descriptor() {
                        return descriptor;
                    }

                    public %s apply(%s) {
                        try {
                            if (TRACE_DOWNCALLS) {
                                traceDowncall(%s);
                            }
                            %s spreader.invokeExact(%s);
                        } catch(IllegalArgumentException | ClassCastException ex$)  {
                            throw ex$; // rethrow IAE from passing wrong number/type of args
                        } catch (Throwable ex$) {
                           throw new AssertionError("should not reach here", ex$);
                        }
                    }
                }
                """,
                retType, paramExprs, traceArgList, returnWithCast, paramList));
        }
        decrAlign();
    }

    void emitPrimitiveTypedef(Declaration.Typedef typedefTree, Type.Primitive primType, String name) {
        emitPrimitiveTypedefLayout(name, primType, typedefTree);
    }

    void emitPointerTypedef(Declaration.Typedef typedefTree, String name) {
        emitPrimitiveTypedefLayout(name, Type.pointer(), typedefTree);
    }

    void emitFirstHeaderPreamble(List<Options.Library> libraries, boolean useSystemLoadLibrary) {
        List<String> lookups = new ArrayList<>();
        // if legacy library loading is selected, load libraries (if any) into current loader
        if (useSystemLoadLibrary) {
            appendBlankLine();
            appendIndentedLines(String.format("""

            static {
            """));
            incrAlign();
            for (Options.Library lib : libraries) {
                String method = lib.specKind() == Options.Library.SpecKind.PATH ? "load" : "loadLibrary";
                appendIndentedLines(String.format("System.%s(\"%s\");", method, lib.toQuotedName()));
            }
            decrAlign();
            appendIndentedLines("""
            }
            """);
        } else {
            // otherwise, add a library lookup per library (if any)
            libraries.stream() // add library lookups (if any)
                    .map(l -> l.specKind() == Options.Library.SpecKind.PATH ?
                            String.format("SymbolLookup.libraryLookup(\"%s\", LIBRARY_ARENA)", l.toQuotedName()) :
                            String.format("SymbolLookup.libraryLookup(System.mapLibraryName(\"%s\"), LIBRARY_ARENA)", l.toQuotedName()))
                    .collect(Collectors.toCollection(() -> lookups));
        }

        lookups.add("SymbolLookup.loaderLookup()"); // fallback to loader lookup
        lookups.add("Linker.nativeLinker().defaultLookup()"); // fallback to native lookup

        // wrap all lookups (but the first) with ".or(...)"
        List<String> lookupCalls = new ArrayList<>();
        boolean isFirst = true;
        for (String lookup : lookups) {
            lookupCalls.add(isFirst ? lookup : String.format(".or(%s)", lookup));
            isFirst = false;
        }

        // chain all the calls together into a combined symbol lookup
        appendBlankLine();
        appendIndentedLines(lookupCalls.stream()
                .collect(Collectors.joining(String.format("\n%s", indentString(2)), "static final SymbolLookup SYMBOL_LOOKUP = ", ";")));
    }

    void emitRuntimeHelperMethods() {
        appendIndentedLines("""

            static final Arena LIBRARY_ARENA = Arena.ofAuto();
            static final boolean TRACE_DOWNCALLS = Boolean.getBoolean("jextract.trace.downcalls");

            static void traceDowncall(String name, Object... args) {
                 String traceArgs = Arrays.stream(args)
                               .map(Object::toString)
                               .collect(Collectors.joining(", "));
                 System.out.printf("%s(%s)\\n", name, traceArgs);
            }

            static MemorySegment findOrThrow(String symbol) {
                return SYMBOL_LOOKUP.find(symbol)
                    .orElseThrow(() -> new UnsatisfiedLinkError("unresolved symbol: " + symbol));
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

    private void emitGlobalGetter(String holderClass, String javaName,
                                  Declaration.Variable decl, String docHeader) {
        appendBlankLine();
        incrAlign();
        emitDocComment(decl, docHeader);
        Class<?> type = Utils.carrierFor(decl.type());
        appendLines(String.format("""
            public static %s %s() {
                return %s.SEGMENT.get(%s.LAYOUT, 0L);
            }
            """, type.getSimpleName(), javaName, holderClass, holderClass));

        decrAlign();
    }

    private void emitGlobalSetter(String holderClass, String javaName,
                                  Declaration.Variable decl, String docHeader) {
        appendBlankLine();
        incrAlign();
        emitDocComment(decl, docHeader);
        Class<?> type = Utils.carrierFor(decl.type());
        appendLines(String.format("""
            public static void %s(%s varValue) {
                %s.SEGMENT.set(%s.LAYOUT, 0L, varValue);
            }
            """, javaName, type.getSimpleName(), holderClass, holderClass));

        decrAlign();
    }

    private void emitGlobalSegmentGetter(String holderClass, String javaName,
                                         Declaration.Variable varTree, String docHeader) {
        appendBlankLine();
        incrAlign();
        emitDocComment(varTree, docHeader);
        appendLines(String.format("""
            public static MemorySegment %s() {
                return %s.SEGMENT;
            }
            """, javaName, holderClass));

        decrAlign();
    }

    private void emitGlobalSegmentSetter(String holderClass, String javaName,
                                         Declaration.Variable varTree, String docHeader) {
        appendBlankLine();
        incrAlign();
        emitDocComment(varTree, docHeader);
        appendLines(String.format("""
            public static void %s(MemorySegment varValue) {
                MemorySegment.copy(varValue, 0L, %s.SEGMENT, 0L, %s.LAYOUT.byteSize());
            }
            """, javaName, holderClass, holderClass));
        decrAlign();
    }

    private void emitGlobalArrayGetter(String holderClass, IndexList indexList,
                                       String javaName, Declaration.Variable varTree, String docHeader) {
        Type elemType = Utils.typeOrElemType(varTree.type());
        Class<?> typeCls = Utils.carrierFor(elemType);
        appendBlankLine();
        incrAlign();
        emitDocComment(varTree, docHeader);
        if (Utils.isStructOrUnion(elemType)) {
            appendLines(String.format("""
                public static MemorySegment %s(%s) {
                    try {
                        return (MemorySegment)%s.HANDLE.invokeExact(%s.SEGMENT, 0L, %s);
                    } catch (Throwable ex$) {
                        throw new AssertionError("should not reach here", ex$);
                    }
                }
                """, javaName, indexList.decl(), holderClass, holderClass, indexList.use()));
        } else {
            appendLines(String.format("""
                public static %s %s(%s) {
                    return (%s)%s.HANDLE.get(%s.SEGMENT, 0L, %s);
                }
                """, typeCls.getSimpleName(), javaName, indexList.decl(), typeCls.getSimpleName(), holderClass, holderClass, indexList.use()));
        }
        decrAlign();
    }

    private void emitGlobalArraySetter(String holderClass, IndexList indexList,
                                       String javaName, Declaration.Variable varTree, String docHeader) {
        Type elemType = Utils.typeOrElemType(varTree.type());
        Class<?> typeCls = Utils.carrierFor(elemType);
        appendBlankLine();
        incrAlign();
        emitDocComment(varTree, docHeader);
        if (Utils.isStructOrUnion(elemType)) {
            appendLines(String.format("""
        public static void %s(%s, MemorySegment varValue) {
            MemorySegment.copy(varValue, 0L, %s(%s), 0L, %s.byteSize());
        }
        """, javaName, indexList.decl(), javaName, indexList.use(), layoutString(elemType)));
        } else {
            appendLines(String.format("""
        public static void %s(%s, %s varValue) {
            %s.HANDLE.set(%s.SEGMENT, 0L, %s, varValue);
        }
        """, javaName, indexList.decl(), typeCls.getSimpleName(), holderClass, holderClass, indexList.use()));
        }
        decrAlign();
    }

    private String emitVarHolderClass(Declaration.Variable var, String javaName) {
        Type varType = var.type();
        String mangledName = newHolderClassName(String.format("%s$constants", javaName));
        String layoutType = Utils.layoutCarrierFor(varType).getSimpleName();
        if (varType instanceof Type.Array) {
            List<Long> dimensions = Utils.dimensions(varType);
            String path = IntStream.range(0, dimensions.size())
                    .mapToObj(_ -> "sequenceElement()")
                    .collect(Collectors.joining(", "));
            Type elemType = Utils.typeOrElemType(varType);
            String accessHandle = Utils.isStructOrUnion(elemType) ?
                    "public static final MethodHandle HANDLE = LAYOUT.sliceHandle(" + path + ");" :
                    "public static final VarHandle HANDLE = LAYOUT.varHandle(" + path + ");\n";
            String dimsString = dimensions.stream().map(Object::toString)
                    .collect(Collectors.joining(", "));
            appendIndentedLines(String.format("""
            private static class %s {
                public static final %s LAYOUT = %s;
                public static final MemorySegment SEGMENT = %s.findOrThrow("%s").reinterpret(LAYOUT.byteSize());
                %s
                public static final long[] DIMS = { %s };
            }
            """, mangledName, layoutType, layoutString(varType), runtimeHelperName(), lookupName(var), accessHandle, dimsString));
        } else {
            appendIndentedLines(String.format("""
            private static class %s {
                public static final %s LAYOUT = %s;
                public static final MemorySegment SEGMENT = %s.findOrThrow("%s").reinterpret(LAYOUT.byteSize());
            }
            """, mangledName, layoutType, layoutString(varType), runtimeHelperName(), lookupName(var)));
        }
        incrAlign();
        appendBlankLine();
        emitDocComment(var, "Layout for variable:");
        appendLines(String.format("""
        public static %s %s$layout() {
            return %s.LAYOUT;
        }
        """, layoutType, javaName, mangledName));
        if (!Utils.isStructOrUnion(varType) && !Utils.isArray(varType)) {
            appendBlankLine();
            emitDocComment(var, "Segment for variable:");
            appendLines(String.format("""
            public static MemorySegment %s$segment() {
                return %s.SEGMENT;
            }
            """, javaName, mangledName));
        }
        if (varType instanceof Type.Array) {
            appendBlankLine();
            emitDocComment(var, "Dimensions for array variable:");
            appendLines(String.format("""
            public static long[] %s$dimensions() {
                return %s.DIMS;
            }
            """, javaName, mangledName));
        }
        decrAlign();
        return mangledName;
    }

    private void emitConstant(Class<?> javaType, String constantName, Object value, Declaration declaration) {
        incrAlign();
        if (value instanceof String) {
            emitDocComment(declaration);
            appendLines(String.format("""
                public static %s %s() {
                    class Holder {
                        static final %s %s
                            = %s.LIBRARY_ARENA.allocateFrom("%s");
                    }
                    return Holder.%s;
                }
                """,
                    javaType.getSimpleName(),
                    constantName,
                    javaType.getSimpleName(),
                    constantName,
                    runtimeHelperName(),
                    Utils.quote(Objects.toString(value)),
                    constantName));
        } else {
            appendLines(String.format("""
                private static final %s %s = %s;
                """,
                javaType.getSimpleName(),
                constantName,
                constantValue(javaType, value)));
                 emitDocComment(declaration);
            appendLines(String.format("""
                public static %s %s() {
                    return %s;
                }
                """,
                javaType.getSimpleName(),
                constantName,
                constantName));
        }
        decrAlign();
    }

    private String constantValue(Class<?> type, Object value) {
        if (type == MemorySegment.class) {
            return String.format("MemorySegment.ofAddress(%dL)", ((Number)value).longValue());
        } else {
            StringBuilder buf = new StringBuilder();
            if (type == float.class) {
                float f = ((Number)value).floatValue();
                if (Float.isFinite(f)) {
                    buf.append(value);
                    buf.append("f");
                } else {
                    buf.append("Float.valueOf(\"");
                    buf.append(value);
                    buf.append("\")");
                }
            } else if (type == long.class) {
                buf.append(value.toString());
                buf.append("L");
            } else if (type == double.class) {
                double d = ((Number)value).doubleValue();
                if (Double.isFinite(d)) {
                    buf.append(value);
                    buf.append("d");
                } else {
                    buf.append("Double.valueOf(\"");
                    buf.append(value);
                    buf.append("\")");
                }
            } else if (type == boolean.class) {
                boolean booleanValue = ((Number)value).byteValue() != 0;
                buf.append(booleanValue);
            } else if (value instanceof Number n) {
                buf.append("(" + type.getName() + ")");
                buf.append(n.longValue() + "L");
            } else {
                throw new IllegalArgumentException(String.format("Unhandled type: %s, or value: %s", type, value));
            }
            return buf.toString();
        }
    }

    private void emitPrimitiveTypedefLayout(String javaName, Type type, Declaration declaration) {
        incrAlign();
        emitDocComment(declaration);
        appendLines(String.format("""
        public static final %s %s = %s;
        """,
         Utils.layoutCarrierFor(type).getSimpleName(), javaName, layoutString(type)));
        decrAlign();
    }

    private String newHolderClassName(String javaName) {
        String holderClassName = javaName;
        while (!holderClassNames.add(holderClassName.toLowerCase())) {
            holderClassName += "$";
        }
        return holderClassName;
    }
}
