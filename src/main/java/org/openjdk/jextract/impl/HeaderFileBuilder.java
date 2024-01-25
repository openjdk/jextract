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

import java.io.File;
import java.lang.foreign.MemoryLayout;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.SegmentAllocator;
import java.lang.invoke.MethodHandle;

import org.openjdk.jextract.Declaration;
import org.openjdk.jextract.Type;
import org.openjdk.jextract.impl.DeclarationImpl.JavaName;

import java.lang.invoke.MethodType;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.StringJoiner;

/**
 * A helper class to generate header interface class in source form.
 * After aggregating various constituents of a .java source, build
 * method is called to get overall generated source string.
 */
class HeaderFileBuilder extends ClassSourceBuilder {

    HeaderFileBuilder(SourceFileBuilder builder, String className, String superName, String runtimeHelperName) {
        super(builder, "public", Kind.CLASS, className, superName, null, runtimeHelperName);
    }

    public void addVar(Declaration.Variable varTree) {
        String nativeName = varTree.name();
        String javaName = JavaName.getOrThrow(varTree);
        String layoutVar = emitVarLayout(varTree.type(), javaName);
        if (Utils.isArray(varTree.type()) || Utils.isStructOrUnion(varTree.type())) {
            String segmentConstant = emitGlobalSegment(layoutVar, javaName, nativeName);
            emitGlobalSegmentGetter(segmentConstant, layoutVar, javaName, varTree, "Getter for variable:");
            emitGlobalSegmentSetter(segmentConstant, layoutVar, javaName, varTree, "Setter for variable:");
        } else if (Utils.isPointer(varTree.type()) || Utils.isPrimitive(varTree.type())) {
            String segmentConstant = emitGlobalSegment(layoutVar, javaName, nativeName);
            emitGlobalGetter(segmentConstant, layoutVar, javaName, varTree, "Getter for variable:");
            emitGlobalSetter(segmentConstant, layoutVar, javaName, varTree, "Setter for variable:");
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
        emitFunctionWrapper(JavaName.getOrThrow(funcTree), nativeName, needsAllocator, isVarargs, parameterNames, funcTree);
    }

    public void addConstant(Declaration.Constant constantTree) {
        Object value = constantTree.value();
        emitConstant(Utils.carrierFor(constantTree.type()), JavaName.getOrThrow(constantTree), value, constantTree);
    }

    // private generation

    private static List<String> finalizeParameterNames(List<String> parameterNames, boolean needsAllocator, boolean isVarArg) {
        List<String> result = new ArrayList<>();

        if (needsAllocator) {
            result.add("allocator");
        }

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
        String returnNoCast = isVoid ? "" : STR."return ";
        String returnWithCast = isVoid ? "" : STR."\{returnNoCast}(\{retType})";
        String getterName = mangleName(javaName, MethodHandle.class);
        String paramList = String.join(", ", finalParamNames);
        String traceArgList = paramList.isEmpty() ?
                STR."\"\{nativeName}\"" :
                STR."\"\{nativeName}\", \{paramList}";
        incrAlign();
        if (!isVarArg) {
            appendLines(STR."""

                private static MethodHandle \{getterName}() {
                    class Holder {
                        static final FunctionDescriptor DESC = \{functionDescriptorString(2, decl.type())};

                        static final MethodHandle MH = Linker.nativeLinker().downcallHandle(
                                \{runtimeHelperName()}.findOrThrow("\{nativeName}"),
                                DESC);
                    }
                    return Holder.MH;
                }
                """);
                appendBlankLine();
                emitDocComment(decl);
                appendLines(STR."""
                public static \{retType} \{javaName}(\{paramExprs(declType, finalParamNames, isVarArg)}) {
                    var mh$ = \{getterName}();
                    try {
                        if (TRACE_DOWNCALLS) {
                            traceDowncall(\{traceArgList});
                        }
                        \{returnWithCast}mh$.invokeExact(\{paramList});
                    } catch (Throwable ex$) {
                       throw new AssertionError("should not reach here", ex$);
                    }
                }
                """);
        } else {
            String paramExprs = paramExprs(declType, finalParamNames, isVarArg);
            String varargsParam = finalParamNames.get(finalParamNames.size() - 1);
            appendBlankLine();
            emitDocComment(decl, "Variadic invoker interface for:");
            appendLines(STR."""
                public interface \{javaName} {
                    \{retType} apply(\{paramExprs});

                    /**
                     * Invoke the variadic function with the given parameters. Layouts for variadic arguments are inferred.
                     */
                    static \{retType} invoke(\{paramExprs}) {
                        MemoryLayout[] inferredLayouts$ = \{runtimeHelperName()}.inferVariadicLayouts(\{varargsParam});
                        \{returnNoCast}\{javaName}(inferredLayouts$).apply(\{String.join(", ", finalParamNames)});
                    }
                }

                """);
            emitDocComment(decl);
            appendLines(STR."""
                public static \{javaName} \{javaName}(MemoryLayout... layouts) {
                    FunctionDescriptor baseDesc$ = \{functionDescriptorString(2, decl.type())};
                    var mh$ = \{runtimeHelperName()}.downcallHandleVariadic("\{nativeName}", baseDesc$, layouts);
                    return (\{paramExprs}) -> {
                        try {
                            if (TRACE_DOWNCALLS) {
                                traceDowncall(\{traceArgList});
                            }
                            \{returnWithCast}mh$.invokeExact(\{paramList});
                        } catch(IllegalArgumentException ex$)  {
                            throw ex$; // rethrow IAE from passing wrong number/type of args
                        } catch (Throwable ex$) {
                           throw new AssertionError("should not reach here", ex$);
                        }
                    };
                }

                """);
        }
        decrAlign();
    }

    void emitPrimitiveTypedef(Declaration.Typedef typedefTree, Type.Primitive primType, String name) {
        emitPrimitiveTypedefLayout(name, primType, typedefTree);
    }

    void emitPointerTypedef(Declaration.Typedef typedefTree, String name) {
        emitPrimitiveTypedefLayout(name, Type.pointer(), typedefTree);
    }

    void emitFirstHeaderPreamble(List<String> libraries) {
        appendIndentedLines("""

            static final SymbolLookup SYMBOL_LOOKUP
                    = SymbolLookup.loaderLookup().or(Linker.nativeLinker().defaultLookup());
            """);
        if (!libraries.isEmpty()) {
            appendIndentedLines("""

                static {
                """);
            incrAlign();
            for (String lib : libraries) {
                String quotedLibName = lib.replace("\\", "\\\\"); // double up slashes
                String method = quotedLibName.indexOf(File.separatorChar) != -1 ? "load" : "loadLibrary";
                appendIndentedLines(STR."System.\{method}(\"\{quotedLibName}\");");
            }
            decrAlign();
            appendIndentedLines("""
                }
                """);
        }
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

            static MemoryLayout[] inferVariadicLayouts(Object[] varargs) {
                MemoryLayout[] result = new MemoryLayout[varargs.length];
                for (int i = 0; i < varargs.length; i++) {
                    result[i] = variadicLayout(varargs[i].getClass());
                }
                return result;
            }

            static MethodHandle upcallHandle(Class<?> fi, String name, FunctionDescriptor fdesc) {
                try {
                    return MethodHandles.lookup().findVirtual(fi, name, fdesc.toMethodType());
                } catch (ReflectiveOperationException ex) {
                    throw new AssertionError(ex);
                }
            }

            static MethodHandle downcallHandleVariadic(String name, FunctionDescriptor baseDesc, MemoryLayout[] variadicLayouts) {
                FunctionDescriptor variadicDesc = baseDesc.appendArgumentLayouts(variadicLayouts);
                Linker.Option fva = Linker.Option.firstVariadicArg(baseDesc.argumentLayouts().size());
                return SYMBOL_LOOKUP.find(name)
                        .map(addr -> Linker.nativeLinker().downcallHandle(addr, variadicDesc, fva)
                                .asSpreader(Object[].class, variadicLayouts.length))
                        .orElse(null);
            }

            // Internals only below this point

            private static MemoryLayout variadicLayout(Class<?> c) {
                // apply default argument promotions per C spec
                // note that all primitives are boxed, since they are passed through an Object[]
                if (c == Boolean.class || c == Byte.class || c == Character.class || c == Short.class || c == Integer.class) {
                    return JAVA_INT;
                } else if (c == Long.class) {
                    return JAVA_LONG;
                } else if (c == Float.class || c == Double.class) {
                    return JAVA_DOUBLE;
                } else if (MemorySegment.class.isAssignableFrom(c)) {
                    return ADDRESS;
                }
                throw new IllegalArgumentException("Invalid type for ABI: " + c.getTypeName());
            }
            """);
    }

    private void emitGlobalGetter(String segmentConstant, String layoutVar, String javaName,
                                  Declaration.Variable decl, String docHeader) {
        appendBlankLine();
        incrAlign();
        emitDocComment(decl, docHeader);
        Class<?> type = Utils.carrierFor(decl.type());
        appendLines(STR."""
            public static \{type.getSimpleName()} \{javaName}() {
                return \{segmentConstant}.get(\{layoutVar}, 0L);
            }
            """);
        decrAlign();
    }

    private void emitGlobalSetter(String segmentConstant, String layoutVar, String javaName,
                                  Declaration.Variable decl, String docHeader) {
        appendBlankLine();
        incrAlign();
        emitDocComment(decl, docHeader);
        Class<?> type = Utils.carrierFor(decl.type());
        appendLines(STR."""
            public static void \{javaName}(\{type.getSimpleName()} x) {
                \{segmentConstant}.set(\{layoutVar}, 0L, x);
            }
            """);
        decrAlign();
    }

    private void emitGlobalSegmentGetter(String segmentConstant, String layoutVar, String javaName,
                                         Declaration.Variable varTree, String docHeader) {
        appendBlankLine();
        incrAlign();
        emitDocComment(varTree, docHeader);
        appendLines(STR."""
            public static MemorySegment \{javaName}() {
                return \{segmentConstant};
            }
            """);
        decrAlign();
    }

    private void emitGlobalSegmentSetter(String segmentConstant, String layoutVar, String javaName,
                                         Declaration.Variable varTree, String docHeader) {
        appendBlankLine();
        incrAlign();
        emitDocComment(varTree, docHeader);
        appendLines(STR."""
            public static void \{javaName}(MemorySegment varValue) {
                MemorySegment.copy(varValue, 0L, \{segmentConstant}, 0L, \{layoutVar}.byteSize());
            }
            """);
        decrAlign();
    }

    public String emitGlobalSegment(String layout, String javaName, String nativeName) {
        String mangledName = mangleName(javaName, MemorySegment.class);
        appendIndentedLines(STR."""

            private static MemorySegment \{mangledName}() {
                class Holder {
                    static final MemorySegment SEGMENT = \{runtimeHelperName()}.findOrThrow("\{nativeName}")
                        .reinterpret(\{layout}.byteSize());
                }
                return Holder.SEGMENT;
            }
            """);
        return STR."\{mangledName}()";
    }

    private String emitVarLayout(Type varType, String javaName) {
        String mangledName = mangleName(javaName, MemoryLayout.class);
        String layoutType = Utils.layoutCarrierFor(varType).getSimpleName();
        appendIndentedLines(STR."""
            private static \{layoutType} \{mangledName}() {
                class Holder {
                    static final \{layoutType} LAYOUT = \{layoutString(varType)};
                }
                return Holder.LAYOUT;
            }
            """);
        return STR."\{mangledName}()";
    }

    private void emitConstant(Class<?> javaType, String constantName, Object value, Declaration declaration) {
        incrAlign();
        if (value instanceof String) {
            emitDocComment(declaration);
            appendLines(STR."""
                public static \{javaType.getSimpleName()} \{constantName}() {
                    class Holder {
                        static final \{javaType.getSimpleName()} \{constantName}
                            = \{runtimeHelperName()}.LIBRARY_ARENA.allocateFrom("\{Utils.quote(Objects.toString(value))}");
                    }
                    return Holder.\{constantName};
                }
                """);
        } else {
            appendLines(STR."""
                private static final \{javaType.getSimpleName()} \{constantName} = \{constantValue(javaType, value)};
                """);
            emitDocComment(declaration);
            appendLines(STR."""
                public static \{javaType.getSimpleName()} \{constantName}() {
                    return \{constantName};
                }
                """);
        }
        decrAlign();
    }

    private String constantValue(Class<?> type, Object value) {
        if (type == MemorySegment.class) {
            return STR."MemorySegment.ofAddress(\{((Number)value).longValue()}L)";
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
                throw new IllegalArgumentException(STR."Unhandled type: \{type}, or value: \{value}");
            }
            return buf.toString();
        }
    }

    private void emitPrimitiveTypedefLayout(String javaName, Type type, Declaration declaration) {
        incrAlign();
        emitDocComment(declaration);
        appendLines(STR."""
        public static final \{Utils.layoutCarrierFor(type).getSimpleName()} \{javaName} = \{layoutString(type)};
        """);
        decrAlign();
    }
}
