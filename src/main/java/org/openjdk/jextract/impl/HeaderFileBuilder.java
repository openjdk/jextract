/*
 * Copyright (c) 2020, 2025, Oracle and/or its affiliates. All rights reserved.
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
        String returnWithCast = isVoid ? "" : String.format("%1$s(%2$s)", returnNoCast, retType);
        String paramList = String.join(", ", finalParamNames);
        String traceArgList = paramList.isEmpty() ?
                String.format("\"%1$s\"", nativeName) :
                String.format("\"%1$s\", %2$s", nativeName, paramList);
        incrAlign();
        if (!isVarArg) {
            String holderClass = newHolderClassName(javaName);
            appendLines("""

                private static class %1$s {
                    public static final FunctionDescriptor DESC = %2$s;

                    public static final MemorySegment ADDR = SYMBOL_LOOKUP.findOrThrow("%3$s");

                    public static final MethodHandle HANDLE = Linker.nativeLinker().downcallHandle(ADDR, DESC);
                }
                """, holderClass, functionDescriptorString(1, decl.type()),
                    lookupName(decl));
            appendBlankLine();
            emitDocComment(decl, "Function descriptor for:");
            appendLines("""
                public static FunctionDescriptor %1$s$descriptor() {
                    return %2$s.DESC;
                }
                """, javaName, holderClass);
            appendBlankLine();
            emitDocComment(decl, "Downcall method handle for:");
            appendLines("""
                public static MethodHandle %1$s$handle() {
                    return %2$s.HANDLE;
                }
                """, javaName, holderClass);
            appendBlankLine();
            emitDocComment(decl, "Address for:");
            appendLines("""
                public static MemorySegment %1$s$address() {
                    return %2$s.ADDR;
                }
                """, javaName, holderClass);
            appendBlankLine();
            emitDocComment(decl);
            appendLines("""
            public static %1$s %2$s(%3$s) {
                var mh$ = %4$s.HANDLE;
                try {
                    if (TRACE_DOWNCALLS) {
                        traceDowncall(%5$s);
                    }
                    %6$smh$.invokeExact(%7$s);
                } catch (Throwable ex$) {
                   throw new AssertionError("should not reach here", ex$);
                }
            }
            """, retType, javaName,
            paramExprs(declType, finalParamNames, isVarArg),
            holderClass, traceArgList, returnWithCast, paramList);
        } else {
            String invokerClassName = newHolderClassName(javaName);
            String paramExprs = paramExprs(declType, finalParamNames, isVarArg);
            appendBlankLine();
            emitDocComment(decl, "Variadic invoker class for:");
            appendLines("""
                public static class %1$s {
                    private static final FunctionDescriptor BASE_DESC = %2$s;
                    private static final MemorySegment ADDR = SYMBOL_LOOKUP.findOrThrow("%3$s");

                    private final MethodHandle handle;
                    private final FunctionDescriptor descriptor;
                    private final MethodHandle spreader;

                    private %4$s(MethodHandle handle, FunctionDescriptor descriptor, MethodHandle spreader) {
                        this.handle = handle;
                        this.descriptor = descriptor;
                        this.spreader = spreader;
                    }
                """, invokerClassName, functionDescriptorString(2, decl.type()),
                    lookupName(decl), invokerClassName);
            incrAlign();
            appendBlankLine();
            emitDocComment(decl, "Variadic invoker factory for:");
            appendLines("""
                public static %1$s makeInvoker(MemoryLayout... layouts) {
                    FunctionDescriptor desc$ = BASE_DESC.appendArgumentLayouts(layouts);
                    Linker.Option fva$ = Linker.Option.firstVariadicArg(BASE_DESC.argumentLayouts().size());
                    var mh$ = Linker.nativeLinker().downcallHandle(ADDR, desc$, fva$);
                    var spreader$ = mh$.asSpreader(Object[].class, layouts.length);
                    return new %1$s(mh$, desc$, spreader$);
                }
                """, invokerClassName);
            decrAlign();
            appendLines("""

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

                    public %1$s apply(%2$s) {
                        try {
                            if (TRACE_DOWNCALLS) {
                                traceDowncall(%3$s);
                            }
                            %4$s spreader.invokeExact(%5$s);
                        } catch(IllegalArgumentException | ClassCastException ex$)  {
                            throw ex$; // rethrow IAE from passing wrong number/type of args
                        } catch (Throwable ex$) {
                           throw new AssertionError("should not reach here", ex$);
                        }
                    }
                }
                """, retType, paramExprs, traceArgList, returnWithCast, paramList);
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
        appendLines("""
            public static %1$s %2$s() {
                return %3$s.SEGMENT.get(%3$s.LAYOUT, 0L);
            }
            """, type.getSimpleName(), javaName, holderClass);
        decrAlign();
    }

    private void emitGlobalSetter(String holderClass, String javaName,
                                  Declaration.Variable decl, String docHeader) {
        appendBlankLine();
        incrAlign();
        emitDocComment(decl, docHeader);
        Class<?> type = Utils.carrierFor(decl.type());
        appendLines("""
            public static void %1$s(%2$s varValue) {
                %3$s.SEGMENT.set(%3$s.LAYOUT, 0L, varValue);
            }
            """, javaName, type.getSimpleName(), holderClass);
        decrAlign();
    }

    private void emitGlobalSegmentGetter(String holderClass, String javaName,
                                         Declaration.Variable varTree, String docHeader) {
        appendBlankLine();
        incrAlign();
        emitDocComment(varTree, docHeader);
        appendLines("""
            public static MemorySegment %1$s() {
                return %2$s.SEGMENT;
            }
            """, javaName, holderClass);
        decrAlign();
    }

    private void emitGlobalSegmentSetter(String holderClass, String javaName,
                                         Declaration.Variable varTree, String docHeader) {
        appendBlankLine();
        incrAlign();
        emitDocComment(varTree, docHeader);
        appendLines("""
            public static void %1$s(MemorySegment varValue) {
                MemorySegment.copy(varValue, 0L, %2$s.SEGMENT, 0L, %2$s.LAYOUT.byteSize());
            }
            """, javaName, holderClass);
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
            appendLines("""
                public static MemorySegment %1$s(%2$s) {
                    try {
                        return (MemorySegment)%3$s.HANDLE.invokeExact(%3$s.SEGMENT, 0L, %4$s);
                    } catch (Throwable ex$) {
                        throw new AssertionError("should not reach here", ex$);
                    }
                }
                """, javaName, indexList.decl(), holderClass, indexList.use());
        } else {
            appendLines("""
                public static %1$s %2$s(%3$s) {
                    return (%1$s)%4$s.HANDLE.get(%4$s.SEGMENT, 0L, %5$s);
                }
                """, typeCls.getSimpleName(), javaName, indexList.decl(),
                    holderClass, indexList.use());
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
            appendLines("""
                public static void %1$s(%2$s, MemorySegment varValue) {
                    MemorySegment.copy(varValue, 0L, %1$s(%3$s), 0L, %4$s.byteSize());
                }
                """, javaName, indexList.decl(), indexList.use(), layoutString(elemType));
        } else {
            appendLines("""
                public static void %1$s(%2$s, %3$s varValue) {
                    %4$s.HANDLE.set(%4$s.SEGMENT, 0L, %5$s, varValue);
                }
                """, javaName, indexList.decl(), typeCls.getSimpleName(), holderClass, indexList.use());
        }
        decrAlign();
    }

    private String emitVarHolderClass(Declaration.Variable var, String javaName) {
        Type varType = var.type();
        String mangledName = newHolderClassName(String.format("%1$s$constants", javaName));
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
            appendIndentedLines("""
                private static class %1$s {
                    public static final %2$s LAYOUT = %3$s;
                    public static final MemorySegment SEGMENT = SYMBOL_LOOKUP.findOrThrow("%4$s").reinterpret(LAYOUT.byteSize());
                %5$s
                    public static final long[] DIMS = { %6$s };
                }
                """, mangledName, layoutType, layoutString(varType),lookupName(var),
                    accessHandle, dimsString);
        } else {
            appendIndentedLines("""
                private static class %1$s {
                    public static final %2$s LAYOUT = %3$s;
                    public static final MemorySegment SEGMENT = SYMBOL_LOOKUP.findOrThrow("%4$s").reinterpret(LAYOUT.byteSize());
                }
                """, mangledName, layoutType, layoutString(varType), lookupName(var));
        }
        incrAlign();
        appendBlankLine();
        emitDocComment(var, "Layout for variable:");
        appendLines("""
                public static %1$s %2$s$layout() {
                    return %3$s.LAYOUT;
                }
                """, layoutType, javaName, mangledName);
        if (!Utils.isStructOrUnion(varType) && !Utils.isArray(varType)) {
            appendBlankLine();
            emitDocComment(var, "Segment for variable:");
            appendLines("""
                    public static MemorySegment %1$s$segment() {
                        return %2$s.SEGMENT;
                    }
                    """, javaName, mangledName);
        }
        if (varType instanceof Type.Array) {
            appendBlankLine();
            emitDocComment(var, "Dimensions for array variable:");
            appendLines("""
                public static long[] %1$s$dimensions() {
                    return %2$s.DIMS;
                }
                """, javaName, mangledName);
        }
        decrAlign();
        return mangledName;
    }

    private void emitConstant(Class<?> javaType, String constantName, Object value, Declaration declaration) {
        incrAlign();
        if (value instanceof String) {
            emitDocComment(declaration);
            appendLines("""
                public static %1$s %2$s() {
                    class Holder {
                        static final %1$s %2$s
                            = %3$s.LIBRARY_ARENA.allocateFrom("%4$s");
                    }
                    return Holder.%2$s;
                }
                """,
                javaType.getSimpleName(),
                constantName,
                runtimeHelperName(),
                Utils.quote(Objects.toString(value)));
        } else {
            appendLines("""
                private static final %1$s %2$s = %3$s;
                """,
                javaType.getSimpleName(),
                constantName,
                constantValue(javaType, value));
            emitDocComment(declaration);
            appendLines("""
                public static %1$s %2$s() {
                    return %2$s;
                }
                """,
                javaType.getSimpleName(),
                constantName);
        }
        decrAlign();
    }

    private String constantValue(Class<?> type, Object value) {
        if (type == MemorySegment.class) {
            return String.format("MemorySegment.ofAddress(%1$dL)", ((Number)value).longValue());
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
                throw new IllegalArgumentException(String.format("Unhandled type: %1$s, or value: %2$s", type, value));
            }
            return buf.toString();
        }
    }

    private void emitPrimitiveTypedefLayout(String javaName, Type type, Declaration declaration) {
        incrAlign();
        emitDocComment(declaration);
        appendLines("""
        public static final %1$s %2$s = %3$s;
        """, Utils.layoutCarrierFor(type).getSimpleName(), javaName, layoutString(type));
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
