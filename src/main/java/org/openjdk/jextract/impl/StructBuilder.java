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

import org.openjdk.jextract.Declaration;
import org.openjdk.jextract.Declaration.Scoped;
import org.openjdk.jextract.Declaration.Variable;
import org.openjdk.jextract.Type;
import org.openjdk.jextract.Type.Declared;
import org.openjdk.jextract.impl.DeclarationImpl.AnonymousStruct;
import org.openjdk.jextract.impl.DeclarationImpl.ClangAlignOf;
import org.openjdk.jextract.impl.DeclarationImpl.ClangOffsetOf;
import org.openjdk.jextract.impl.DeclarationImpl.ClangSizeOf;
import org.openjdk.jextract.impl.DeclarationImpl.JavaFunctionalInterfaceName;
import org.openjdk.jextract.impl.DeclarationImpl.JavaName;
import org.openjdk.jextract.impl.DeclarationImpl.Skip;

import java.text.MessageFormat;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * This class generates static utilities class for C structs, unions.
 */
final class StructBuilder extends ClassSourceBuilder implements OutputFactory.Builder {

    private final Declaration.Scoped structTree;
    private final Type structType;
    private final Deque<Declaration> nestedAnonDeclarations;

    StructBuilder(SourceFileBuilder builder, String modifiers, String className,
                  ClassSourceBuilder enclosing, String runtimeHelperName, Declaration.Scoped structTree) {
        super(builder, modifiers, Kind.CLASS, className, null, enclosing, runtimeHelperName);
        this.structTree = structTree;
        this.structType = Type.declared(structTree);
        this.nestedAnonDeclarations = new ArrayDeque<>();
    }

    private String safeParameterName(String paramName) {
        return isEnclosedBySameName(paramName) ? paramName + "$" : paramName;
    }

    private void pushNestedAnonDecl(Declaration anonDecl) {
        nestedAnonDeclarations.push(anonDecl);
    }

    private void popNestedAnonDecl() {
        nestedAnonDeclarations.pop();
    }

    void begin() {
        if (!inAnonymousNested()) {
            if (isNested()) {
                sourceFileBuilder().incrAlign();
            }
            appendBlankLine();
            emitDocComment(structTree);
            classBegin();
            emitDefaultConstructor();
            emitLayoutDecl();
        }
    }

    void end() {
        if (!inAnonymousNested()) {
            emitAsSlice();
            emitSizeof();
            emitAllocatorAllocate();
            emitAllocatorAllocateArray();
            emitReinterpret();
            classEnd();
            if (isNested()) {
                // we are nested. Decrease align
                sourceFileBuilder().decrAlign();
            }
        } else {
            // we're in an anonymous struct which got merged into this one, return this very builder and keep it open
            popNestedAnonDecl();
        }
    }

    private boolean inAnonymousNested() {
        return !nestedAnonDeclarations.isEmpty();
    }

    @Override
    public StructBuilder addStruct(Declaration.Scoped tree) {
        if (AnonymousStruct.isPresent(tree)) {
            //nested anon struct - merge into this builder!
            pushNestedAnonDecl(tree);
            return this;
        } else {
            StructBuilder builder = new StructBuilder(sourceFileBuilder(), "public static",
                    JavaName.getOrThrow(tree), this, runtimeHelperName(), tree);
            builder.begin();
            return builder;
        }
    }

    @Override
    public void addFunctionalInterface(Declaration parentDecl, Type.Function funcType) {
        incrAlign();
        FunctionalInterfaceBuilder.generate(sourceFileBuilder(), JavaFunctionalInterfaceName.getOrThrow(parentDecl),
                this, runtimeHelperName(), parentDecl, funcType, true);
        decrAlign();
    }

    @Override
    public void addVar(Declaration.Variable varTree) {
        String javaName = JavaName.getOrThrow(varTree);
        appendBlankLine();
        String layoutField = emitLayoutFieldDecl(varTree, javaName);
        appendBlankLine();
        String offsetField = emitOffsetFieldDecl(varTree, javaName);
        if (Utils.isArray(varTree.type()) || Utils.isStructOrUnion(varTree.type())) {
            emitSegmentGetter(javaName, varTree, offsetField, layoutField);
            emitSegmentSetter(javaName, varTree, offsetField, layoutField);
            int dims = Utils.dimensions(varTree.type()).size();
            if (dims > 0) {
                emitDimensionsFieldDecl(varTree, javaName);
                String arrayHandle = emitArrayElementHandle(javaName, varTree, layoutField, dims);
                IndexList indexList = IndexList.of(dims);
                emitFieldArrayGetter(javaName, varTree, arrayHandle, indexList);
                emitFieldArraySetter(javaName, varTree, arrayHandle, indexList);
            }
        } else if (Utils.isPointer(varTree.type()) || Utils.isPrimitive(varTree.type())) {
            emitFieldGetter(javaName, varTree, layoutField, offsetField);
            emitFieldSetter(javaName, varTree, layoutField, offsetField);
        } else {
            throw new IllegalArgumentException(String.format("Type not supported: %s", varTree.type()));
        }
    }

    private List<String> prefixNamesList() {
        return nestedAnonDeclarations.stream()
                .map(d -> AnonymousStruct.anonName((Declaration.Scoped)d))
                .toList().reversed();
    }

    private String fieldElementPaths(String nativeName) {
        StringBuilder builder = new StringBuilder();
        String prefix = "";
        for (String prefixElementName : prefixNamesList()) {
            builder.append(prefix + "groupElement(\"" + prefixElementName + "\")");
            prefix = ", ";
        }
        builder.append(prefix + "groupElement(\"" + nativeName + "\")");
        return builder.toString();
    }

    private void emitFieldDocComment(Declaration.Variable varTree, String header) {
        incrAlign();
        emitDocComment(varTree, header);
        decrAlign();
    }

    private String kindName() {
        return structTree.kind() == Scoped.Kind.STRUCT ? "struct" : "union";
    }

    private void emitFieldGetter(String javaName, Declaration.Variable varTree, String layoutField, String offsetField) {
        String segmentParam = safeParameterName(kindName());
        Class<?> type = Utils.carrierFor(varTree.type());
        appendBlankLine();
        emitFieldDocComment(varTree, "Getter for field:");
        appendIndentedLines("""
            public static %1$s %2$s(MemorySegment %3$s) {
                return %3$s.get(%4$s, %5$s);
            }
            """, type.getSimpleName(), javaName, segmentParam, layoutField, offsetField);
    }

    private void emitFieldSetter(String javaName, Declaration.Variable varTree, String layoutField, String offsetField) {
        String segmentParam = safeParameterName(kindName());
        String valueParam = safeParameterName("fieldValue");
        Class<?> type = Utils.carrierFor(varTree.type());
        appendBlankLine();
        emitFieldDocComment(varTree, "Setter for field:");
        appendIndentedLines("""
            public static void %1$s(MemorySegment %2$s, %3$s %4$s) {
                %2$s.set(%5$s, %6$s, %4$s);
            }
            """, javaName, segmentParam, type.getSimpleName(), valueParam, layoutField, offsetField);
    }

    private void emitSegmentGetter(String javaName, Declaration.Variable varTree, String offsetField, String layoutField) {
        appendBlankLine();
        emitFieldDocComment(varTree, "Getter for field:");
        String segmentParam = safeParameterName(kindName());
        appendIndentedLines("""
            public static MemorySegment %1$s(MemorySegment %2$s) {
                return %2$s.asSlice(%3$s, %4$s.byteSize());
            }
            """,
            javaName, segmentParam, offsetField, layoutField);
    }

    private void emitSegmentSetter(String javaName, Declaration.Variable varTree, String offsetField, String layoutField) {
        appendBlankLine();
        emitFieldDocComment(varTree, "Setter for field:");
        String segmentParam = safeParameterName(kindName());
        String valueParam = safeParameterName("fieldValue");
        appendIndentedLines("""
           public static void %1$s(MemorySegment %2$s, MemorySegment %3$s) {
                MemorySegment.copy(%3$s, 0L, %2$s, %4$s, %5$s.byteSize());
            }
            """, javaName, segmentParam, valueParam, offsetField, layoutField);
    }

    private String emitArrayElementHandle(String javaName, Declaration.Variable varTree, String fieldLayoutName, int dims) {
        String arrayHandleName = String.format("%s$ELEM_HANDLE", javaName);
        String path = IntStream.range(0, dims)
                .mapToObj(_ -> "sequenceElement()")
                .collect(Collectors.joining(", "));
        Type elemType = Utils.typeOrElemType(varTree.type());
        if (Utils.isStructOrUnion(elemType)) {
            appendIndentedLines("""
                private static final MethodHandle %s = %s.sliceHandle(%s);
                """, arrayHandleName, fieldLayoutName, path);
        } else {
            appendIndentedLines("""
                private static final VarHandle %s = %s.varHandle(%s);
                """, arrayHandleName, fieldLayoutName, path);
        }
        return arrayHandleName;
    }

    private void emitFieldArrayGetter(String javaName, Declaration.Variable varTree, String arrayElementHandle, IndexList indexList) {
        String segmentParam = safeParameterName(kindName());
        Type elemType = Utils.typeOrElemType(varTree.type());
        Class<?> elemTypeCls = Utils.carrierFor(elemType);
        appendBlankLine();
        emitFieldDocComment(varTree, "Indexed getter for field:");
        if (Utils.isStructOrUnion(elemType)) {
            appendIndentedLines("""
                public static MemorySegment %1$s(MemorySegment %2$s, %3$s) {
                    try {
                        return (MemorySegment)%4$s.invokeExact(%2$s, 0L, %5$s);
                    } catch (Throwable ex$) {
                        throw new AssertionError("should not reach here", ex$);
                    }
                }
                """, javaName, segmentParam, indexList.decl(), arrayElementHandle, indexList.use());
        } else {
            appendIndentedLines("""
                public static %1$s %2$s(MemorySegment %3$s, %4$s) {
                    return (%1$s)%5$s.get(%3$s, 0L, %6$s);
                }
                """, elemTypeCls.getSimpleName(), javaName, segmentParam
                   ,indexList.decl(), arrayElementHandle, indexList.use());
        }
    }

    private void emitFieldArraySetter(String javaName, Declaration.Variable varTree, String arrayElementHandle, IndexList indexList) {
        String segmentParam = safeParameterName(kindName());
        String valueParam = safeParameterName("fieldValue");
        Type elemType = Utils.typeOrElemType(varTree.type());
        Class<?> elemTypeCls = Utils.carrierFor(elemType);
        appendBlankLine();
        emitFieldDocComment(varTree, "Indexed setter for field:");
        if (Utils.isStructOrUnion(elemType)) {
            appendIndentedLines("""
                public static void %1$s(MemorySegment %2$s, %3$s, MemorySegment %4$s) {
                    MemorySegment.copy(%4$s, 0L, %1$s(%2$s, %5$s), 0L, %6$s.byteSize());
                }
                """, javaName, segmentParam, indexList.decl()
                   ,valueParam, indexList.use(), layoutString(elemType));
        } else {
            appendIndentedLines("""
                public static void %1$s(MemorySegment %2$s, %3$s, %4$s %5$s) {
                    %6$s.set(%2$s, 0L, %7$s, %5$s);
                }
                """, javaName, segmentParam, indexList.decl(), elemTypeCls.getSimpleName()
                   ,valueParam, arrayElementHandle, indexList.use());
        }
    }

    private void emitAsSlice() {
        String arrayParam = safeParameterName("array");
        appendIndentedLines(MessageFormat.format("""

            /**
             * Obtains a slice of {0} which selects the array element at {0}.
             * The returned segment has address {0}.address() + index * layout().byteSize()
             */
            public static MemorySegment asSlice(MemorySegment {0}, long index) '{'
                return {0}.asSlice(layout().byteSize() * index);
            '}'
            """, arrayParam));
    }

    private void emitSizeof() {
        appendIndentedLines("""
            /**
             * The size (in bytes) of this %s
             */
            public static long sizeof() { return layout().byteSize(); }
            """, kindName());
    }

    private void emitAllocatorAllocate() {
        String allocatorParam = safeParameterName("allocator");
        appendIndentedLines(MessageFormat.format("""

            /**
             * Allocate a segment of size '{'@code layout().byteSize()'}' using {0}
             */
            public static MemorySegment allocate(SegmentAllocator {0}) '{'
                return {0}.allocate(layout());
            '}'
            """, allocatorParam));
    }

    private void emitAllocatorAllocateArray() {
        String allocatorParam = safeParameterName("allocator");
        String elementCountParam = safeParameterName("elementCount");
        appendIndentedLines("""

            /**
             * Allocate an array of size {@code %1$s} using %2$s.
             * The returned segment has size {@code %1$s * layout().byteSize()}.
             */
            public static MemorySegment allocateArray(long %1$s, SegmentAllocator %2$s) {
                return %2$s.allocate(MemoryLayout.sequenceLayout(%1$s, layout()));
            }
            """, elementCountParam, allocatorParam);
    }

    private void emitReinterpret() {
        appendIndentedLines("""

            /**
             * Reinterprets {@code addr} using target {@code arena} and {@code cleanupAction} (if any).
             * The returned segment has size {@code layout().byteSize()}
             */
            public static MemorySegment reinterpret(MemorySegment addr, Arena arena, Consumer<MemorySegment> cleanup) {
                return reinterpret(addr, 1, arena, cleanup);
            }

            /**
             * Reinterprets {@code addr} using target {@code arena} and {@code cleanupAction} (if any).
             * The returned segment has size {@code elementCount * layout().byteSize()}
             */
            public static MemorySegment reinterpret(MemorySegment addr, long elementCount, Arena arena, Consumer<MemorySegment> cleanup) {
                return addr.reinterpret(layout().byteSize() * elementCount, arena, cleanup);
            }
            """);
    }

    private void emitLayoutDecl() {
        appendIndentedLines("""

            private static final GroupLayout $LAYOUT = %s;

            /**
             * The layout of this %s
             */
            public static final GroupLayout layout() {
                return $LAYOUT;
            }
            """,
            structOrUnionLayoutString(structType), kindName());
    }

    private String emitOffsetFieldDecl(Declaration.Variable field, String javaName) {
        String offsetFieldName = String.format("%s$OFFSET", javaName);
        appendIndentedLines(String.format("""
            private static final long %s = %d;
            """,
            offsetFieldName, ClangOffsetOf.getOrThrow(field) / 8));
        appendBlankLine();
        emitFieldDocComment(field, "Offset for field:");
        appendIndentedLines("""
            public static final long %s$offset() {
                return %s;
            }
            """, javaName, offsetFieldName);
        return offsetFieldName;
    }

    private String emitLayoutFieldDecl(Declaration.Variable field, String javaName) {
        String layoutFieldName = String.format("%s$LAYOUT", javaName);
        String layoutType = Utils.layoutCarrierFor(field.type()).getSimpleName();
        appendIndentedLines("""
            private static final %s %s = (%1$s)$LAYOUT.select(%s);
            """, layoutType, layoutFieldName, fieldElementPaths(field.name()));
        appendBlankLine();
        emitFieldDocComment(field, "Layout for field:");
        appendIndentedLines("""
            public static final %s %s$layout() {
                return %s;
            }
            """, layoutType, javaName, layoutFieldName);
        return layoutFieldName;
    }

    private void emitDimensionsFieldDecl(Declaration.Variable field, String javaName) {
        String dimsFieldName = String.format("%s$DIMS", javaName);
        List<Long> dimensions = Utils.dimensions(field.type());
        String dimsString = dimensions.stream().map(Object::toString)
                .collect(Collectors.joining(", "));
        appendIndentedLines("""

            private static long[] %s = { %s };
            """, dimsFieldName, dimsString);
        appendBlankLine();
        emitFieldDocComment(field, "Dimensions for array field:");
        appendIndentedLines("""
            public static long[] %s$dimensions() {
                return %s;
            }
            """, javaName, dimsFieldName);
    }

    private String structOrUnionLayoutString(Type type) {
        return switch (type) {
            case Declared d when Utils.isStructOrUnion(type) -> structOrUnionLayoutString(0, d.tree(), 0);
            default -> throw new UnsupportedOperationException(type.toString());
        };
    }

    private static long recordMemberOffset(Declaration member) {
        if (member instanceof Variable) {
            return ClangOffsetOf.get(member).orElseThrow();
        } else {
            // anonymous struct
            return AnonymousStruct.getOrThrow((Scoped) member).offset().orElseThrow();
        }
    }

    private String structOrUnionLayoutString(long base, Declaration.Scoped scoped, int indent) {
        List<String> memberLayouts = new ArrayList<>();

        boolean isStruct = scoped.kind() == Scoped.Kind.STRUCT;

        long align = ClangAlignOf.getOrThrow(scoped) / 8;
        long offset = base;

        long size = 0L; // bits
        for (Declaration member : scoped.members()) {
            if (!Skip.isPresent(member)) {
                long nextOffset = recordMemberOffset(member);
                long delta = nextOffset - offset;
                if (delta > 0) {
                    memberLayouts.add(paddingLayoutString(delta / 8, indent + 1));
                    offset += delta;
                    if (isStruct) {
                        size += delta;
                    }
                }
                String memberLayout;
                if (member instanceof Variable var) {
                    memberLayout = layoutString(var.type(), align);
                    memberLayout = String.format("%s%s.withName(\"%s\")", indentString(indent + 1), memberLayout, member.name());
                } else {
                    // anon struct
                    memberLayout = structOrUnionLayoutString(offset, (Scoped) member, indent + 1);
                }
                memberLayouts.add(memberLayout);
                // update offset and size
                long fieldSize = ClangSizeOf.getOrThrow(member);
                if (isStruct) {
                    offset += fieldSize;
                    size += fieldSize;
                } else {
                    size = Math.max(size, ClangSizeOf.getOrThrow(member));
                }
            }
        }
        long expectedSize = ClangSizeOf.getOrThrow(scoped);
        if (size != expectedSize) {
            long trailPadding = isStruct ?
                    (expectedSize - size) / 8 :
                    expectedSize / 8;
            memberLayouts.add(paddingLayoutString(trailPadding, indent + 1));
        }

        String prefix = isStruct ?
                String.format("%sMemoryLayout.structLayout(\n", indentString(indent)) :
                String.format("%sMemoryLayout.unionLayout(\n", indentString(indent));
        String suffix = String.format("\n%s)", indentString(indent));
        String layoutString = memberLayouts.stream()
                .collect(Collectors.joining(",\n", prefix, suffix));

        // the name is only useful for clients accessing the layout, jextract doesn't care about it
        String name = scoped.name().isEmpty() ?
                AnonymousStruct.anonName(scoped) : scoped.name();
        return String.format("%s.withName(\"%s\")", layoutString, name);
    }
}
