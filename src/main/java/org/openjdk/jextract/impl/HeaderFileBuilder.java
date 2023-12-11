/*
 * Copyright (c) 2020, 2023, Oracle and/or its affiliates. All rights reserved.
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
import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.GroupLayout;
import java.lang.foreign.MemoryLayout;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.SegmentAllocator;
import java.lang.foreign.SequenceLayout;
import java.lang.foreign.ValueLayout;
import java.lang.invoke.MethodHandle;

import org.openjdk.jextract.Declaration;
import org.openjdk.jextract.Type;
import org.openjdk.jextract.impl.DeclarationImpl.JavaName;

import java.lang.invoke.MethodType;
import java.lang.invoke.VarHandle;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.StringJoiner;

/**
 * A helper class to generate header interface class in source form.
 * After aggregating various constituents of a .java source, build
 * method is called to get overall generated source string.
 */
class HeaderFileBuilder extends ClassSourceBuilder {

    static final String MEMBER_MODS = "public static";

    HeaderFileBuilder(SourceFileBuilder builder, String className, String superName, String runtimeHelperName) {
        super(builder, "public", Kind.CLASS, className, superName, null, runtimeHelperName);
    }

    public void addVar(Declaration.Variable varTree, Optional<String> fiName) {
        String nativeName = varTree.name();
        String javaName = JavaName.getOrThrow(varTree);
        String layoutVar = emitVarLayout(varTree.type(), javaName);
        if (Utils.isArray(varTree.type()) || Utils.isStructOrUnion(varTree.type())) {
            emitGlobalSegment(layoutVar, javaName, nativeName, varTree);
        } else if (Utils.isPointer(varTree.type()) || Utils.isPrimitive(varTree.type())) {
            String vhConstant = emitGlobalVarHandle(javaName, layoutVar);
            String segmentConstant = emitGlobalSegment(layoutVar, javaName, nativeName, null);
            emitGlobalGetter(segmentConstant, vhConstant, javaName, varTree, "Getter for variable:");
            emitGlobalSetter(segmentConstant, vhConstant, javaName, varTree, "Setter for variable:");

            if (fiName.isPresent()) {
                emitFunctionalInterfaceGetter(fiName.get(), javaName);
            }
        }
    }

    public void addFunction(Declaration.Function funcTree, FunctionDescriptor descriptor) {
        String nativeName = funcTree.name();
        boolean isVarargs = funcTree.type().varargs();
        boolean needsAllocator = descriptor.returnLayout().isPresent() &&
                descriptor.returnLayout().get() instanceof GroupLayout;
        List<String> parameterNames = funcTree.parameters().
                stream().
                map(JavaName::getOrThrow).
                toList();
        emitFunctionWrapper(JavaName.getOrThrow(funcTree), nativeName, descriptor, needsAllocator, isVarargs, parameterNames, funcTree);
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

    private void emitFunctionWrapper(String javaName, String nativeName, FunctionDescriptor descriptor, boolean needsAllocator,
                                     boolean isVarArg, List<String> parameterNames, Declaration decl) {
        MethodType declType = descriptor.toMethodType();
        List<String> finalParamNames = finalizeParameterNames(parameterNames, needsAllocator, isVarArg);
        if (needsAllocator) {
            declType = declType.insertParameterTypes(0, SegmentAllocator.class);
        }

        String retType = declType.returnType().getSimpleName();
        String returnExpr = "";
        if (!declType.returnType().equals(void.class)) {
            returnExpr = STR."return (\{retType}) ";
        }
        String getterName = mangleName(javaName, MethodHandle.class);
        incrAlign();
        if (!isVarArg) {
        emitDocComment(decl);
        appendLines(STR."""
            \{MEMBER_MODS} MethodHandle \{getterName}() {
                class Holder {
                    static final FunctionDescriptor DESC = \{descriptorString(2, descriptor)};

                    static final MethodHandle MH = Linker.nativeLinker().downcallHandle(
                            \{runtimeHelperName()}.findOrThrow("\{nativeName}"),
                            DESC);
                }
                return Holder.MH;
            }

            public static \{retType} \{javaName}(\{paramExprs(declType, finalParamNames, isVarArg)}) {
                var mh$ = \{getterName}();
                try {
                    \{returnExpr}mh$.invokeExact(\{String.join(", ", finalParamNames)});
                } catch (Throwable ex$) {
                   throw new AssertionError("should not reach here", ex$);
                }
            }
            """);
        } else {
            String invokerName = javaName + "$invoker";
            String invokerFactoryName = javaName + "$makeInvoker";
            String paramExprs = paramExprs(declType, finalParamNames, isVarArg);
            appendLines(STR."""
                public interface \{invokerName} {
                    \{retType} \{javaName}(\{paramExprs});
                }

                """);
            emitDocComment(decl);
            appendLines(STR."""
                public static \{invokerName} \{invokerFactoryName}(MemoryLayout... layouts) {
                    FunctionDescriptor baseDesc$ = \{descriptorString(2, descriptor)};
                    var mh$ = \{runtimeHelperName()}.downcallHandleVariadic("\{nativeName}", baseDesc$, layouts);
                    return (\{paramExprs}) -> {
                        try {
                            \{returnExpr}mh$.invokeExact(\{String.join(", ", finalParamNames)});
                        } catch(IllegalArgumentException ex$)  {
                            throw ex$; // rethrow IAE from passing wrong number/type of args
                        } catch (Throwable ex$) {
                           throw new AssertionError("should not reach here", ex$);
                        }
                    };
                }

                """);
            emitDocComment(decl);
            String varargsParam = finalParamNames.get(finalParamNames.size() - 1);
            appendLines(STR."""
                public static \{retType} \{javaName}(\{paramExprs}) {
                    MemoryLayout[] inferredLayouts$ = \{runtimeHelperName()}.inferVariadicLayouts(\{varargsParam});
                    \{returnExpr}\{invokerFactoryName}(inferredLayouts$).\{javaName}(\{String.join(", ", finalParamNames)});
                }
                """);
        }
        decrAlign();
    }

    private void emitFunctionalInterfaceGetter(String fiName, String javaName) {
        appendIndentedLines(STR."""
            public static \{fiName} \{javaName}() {
                return \{fiName}.ofAddress(\{javaName}$get(), Arena.global());
            }
            """);
    }

    void emitPrimitiveTypedef(Type.Primitive primType, String name) {
        emitPrimitiveTypedef(null, primType, name);
    }

    void emitPrimitiveTypedef(Declaration.Typedef typedefTree, Type.Primitive primType, String name) {
        emitPrimitiveTypedefLayout(name, primType, typedefTree);
    }

    void emitPointerTypedef(Declaration.Typedef typedefTree, String name) {
        emitPrimitiveTypedefLayout(name, Type.pointer(), typedefTree);
    }

    void emitFirstHeaderPreamble(List<String> libraries) {
        incrAlign();
        appendLines("static final SymbolLookup SYMBOL_LOOKUP;");
        appendLines("static {");
        for (String lib : libraries) {
            String quotedLibName = lib.replace("\\", "\\\\"); // double up slashes
            String method = quotedLibName.indexOf(File.separatorChar) != -1 ? "load" : "loadLibrary";
            appendIndentedLines(STR."System.\{method}(\"\{quotedLibName}\");");
        }
        appendLines("""

            SymbolLookup loaderLookup = SymbolLookup.loaderLookup();
            Linker linker = Linker.nativeLinker();
            SYMBOL_LOOKUP = name -> loaderLookup.find(name).or(() -> linker.defaultLookup().find(name));
            """);
        appendLines("}");
        decrAlign();
    }

    void emitRuntimeHelperMethods() {
        appendIndentedLines("""
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

    private void emitGlobalGetter(String segmentConstant, String vhConstant, String javaName,
                                  Declaration.Variable decl, String docHeader) {
        incrAlign();
        emitDocComment(decl, docHeader);
        Class<?> type = Utils.carrierFor(decl.type());
        appendLines(STR."""
            public static \{type.getSimpleName()} \{javaName}$get() {
                return (\{type.getSimpleName()}) \{vhConstant}.get(\{segmentConstant}(), 0L);
            }
            """);
        decrAlign();
    }

    private void emitGlobalSetter(String segmentConstant, String vhConstant, String javaName,
                                  Declaration.Variable decl, String docHeader) {
        incrAlign();
        emitDocComment(decl, docHeader);
        Class<?> type = Utils.carrierFor(decl.type());
        appendLines(STR."""
            public static void \{javaName}$set(\{type.getSimpleName()} x) {
                \{vhConstant}.set(\{segmentConstant}(), 0L, x);
            }
            """);
        decrAlign();
    }

    public String emitGlobalSegment(String layout, String javaName, String nativeName, Declaration declaration) {
        String mangledName = mangleName(javaName, MemorySegment.class);
        incrAlign();
        if (declaration != null) {
            emitDocComment(declaration);
        }
        appendLines(STR."""
            \{MEMBER_MODS} MemorySegment \{mangledName}() {
                class Holder {
                    static final MemorySegment SEGMENT = \{runtimeHelperName()}.findOrThrow("\{nativeName}")
                        .reinterpret(\{layout}.byteSize());
                }
                return Holder.SEGMENT;
            }
            """);
        decrAlign();
        return mangledName;
    }

    private String emitVarLayout(Type varType, String javaName) {
        String mangledName = mangleName(javaName, MemoryLayout.class);
        MemoryLayout layout = Type.layoutFor(varType).get();
        appendIndentedLines(STR."""
            private static final MemoryLayout \{mangledName} = \{layoutString(0, layout)};

            \{MEMBER_MODS} MemoryLayout \{mangledName}() {
                return \{mangledName};
            }
            """);
        return mangledName;
    }

    private String emitGlobalVarHandle(String javaName, String layoutVar) {
        String mangledName = mangleName(javaName, VarHandle.class);
        appendIndentedLines(STR."""
            private static final VarHandle \{mangledName} = \{layoutVar}.varHandle();

            \{MEMBER_MODS} VarHandle \{mangledName}() {
                return \{mangledName};
            }
            """);
        return mangledName;
    }

    private void emitConstant(Class<?> javaType, String constantName, Object value, Declaration declaration) {
        incrAlign();
        appendLines(STR."""
            private static final \{javaType.getSimpleName()} \{constantName} = \{constantValue(javaType, value)};

            """);
        if (declaration != null) {
            emitDocComment(declaration);
        }
        appendLines(STR."""
            \{MEMBER_MODS} \{javaType.getSimpleName()} \{constantName}() {
                return \{constantName};
            }
            """);
        decrAlign();
    }

    private String constantValue(Class<?> type, Object value) {
        if (value instanceof String) {
            return STR."Arena.ofAuto().allocateFrom(\"\{Utils.quote(Objects.toString(value))}\");";
        } else if (type == MemorySegment.class) {
            return STR."MemorySegment.ofAddress(\{((Number)value).longValue()}L);";
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
            } else {
                buf.append("(" + type.getName() + ")");
                buf.append(value + "L");
            }
            return buf.toString();
        }
    }

    private void emitPrimitiveTypedefLayout(String javaName, Type type, Declaration declaration) {
        incrAlign();
        if (declaration != null) {
            emitDocComment(declaration);
        }
        MemoryLayout layout = Type.layoutFor(type).get();
        appendLines(STR."""
        public static final \{Utils.valueLayoutCarrierFor(type).getSimpleName()} \{javaName} = \{layoutString(0, layout)};
        """);
        decrAlign();
    }
}
