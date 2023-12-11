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

import java.lang.foreign.GroupLayout;
import java.lang.foreign.MemoryLayout;
import java.lang.foreign.SequenceLayout;
import java.lang.foreign.ValueLayout;
import org.openjdk.jextract.Declaration;
import org.openjdk.jextract.Type;
import org.openjdk.jextract.impl.DeclarationImpl.AnonymousStruct;
import org.openjdk.jextract.impl.DeclarationImpl.ClangOffsetOf;
import org.openjdk.jextract.impl.DeclarationImpl.ClangSizeOf;
import org.openjdk.jextract.impl.DeclarationImpl.JavaName;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Optional;

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
        return isEnclosedBySameName(paramName)? paramName + "$" : paramName;
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
            emitDocComment(structTree);
            classBegin();
            emitLayoutDecl();
        }
    }

    void end() {
        if (!inAnonymousNested()) {
            emitSizeof();
            emitAllocatorAllocate();
            emitAllocatorAllocateArray();
            emitOfAddressScoped();
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
            StructBuilder builder = new StructBuilder(sourceFileBuilder(), "public static final",
                    JavaName.getOrThrow(tree), this, runtimeHelperName(), tree);
            builder.begin();
            builder.emitPrivateDefaultConstructor();
            return builder;
        }
    }

    @Override
    public void addFunctionalInterface(String name, Type.Function funcType) {
        incrAlign();
        FunctionalInterfaceBuilder.generate(sourceFileBuilder(), name,
                this, runtimeHelperName(), funcType,
                funcType.parameterNames().map(NameMangler::javaSafeIdentifiers));
        decrAlign();
    }

    @Override
    public void addVar(Declaration.Variable varTree, Optional<String> fiName) {
        String javaName = JavaName.getOrThrow(varTree);
        long offset = ClangOffsetOf.getOrThrow(varTree) / 8;
        long size = ClangSizeOf.getOrThrow(varTree) / 8;
        if (Utils.isArray(varTree.type()) || Utils.isStructOrUnion(varTree.type())) {
            emitSegmentGetter(javaName, offset, size);
        } else if (Utils.isPointer(varTree.type()) || Utils.isPrimitive(varTree.type())) {
            emitFieldDocComment(varTree, "Getter for field:");
            emitFieldGetter(javaName, varTree.type(), offset);
            emitFieldDocComment(varTree, "Setter for field:");
            emitFieldSetter(javaName, varTree.type(), offset);
            emitIndexedFieldGetter(javaName, varTree.type(), offset);
            emitIndexedFieldSetter(javaName, varTree.type(), offset);
            if (fiName.isPresent()) {
                emitFunctionalInterfaceGetter(fiName.get(), javaName);
            }
        }
    }

    private void emitFieldDocComment(Declaration.Variable varTree, String header) {
        incrAlign();
        emitDocComment(varTree, header);
        decrAlign();
    }

    private void emitFunctionalInterfaceGetter(String fiName, String javaName) {
        appendIndentedLines(STR."""
            public static \{fiName} \{javaName}(MemorySegment segment, Arena scope) {
                return \{fiName}.ofAddress(\{javaName}$get(segment), scope);
            }
            """);
    }

    private void emitFieldGetter(String javaName, Type varType, long offset) {
        String seg = safeParameterName("seg");
        Class<?> type = Utils.carrierFor(varType);
        MemoryLayout layout = Type.layoutFor(varType).get();
        appendIndentedLines(STR."""
            public static \{type.getSimpleName()} \{javaName}$get(MemorySegment \{seg}) {
                return \{seg}.get(\{layoutString(1, layout)}, \{offset});
            }
            """);
    }

    private void emitFieldSetter(String javaName, Type varType, long offset) {
        String seg = safeParameterName("seg");
        String x = safeParameterName("x");
        Class<?> type = Utils.carrierFor(varType);
        MemoryLayout layout = Type.layoutFor(varType).get();
        appendIndentedLines(STR."""
            public static void \{javaName}$set(MemorySegment \{seg}, \{type.getSimpleName()} \{x}) {
                \{seg}.set(\{layoutString(1, layout)}, \{offset}, \{x});
            }
            """);
    }

    private void emitSegmentGetter(String javaName, long offset, long size) {
        String seg = safeParameterName("seg");
        appendIndentedLines(STR."""
            public static MemorySegment \{javaName}$slice(MemorySegment \{seg}) {
                return \{seg}.asSlice(\{offset}, \{size});
            }
            """);
    }

    private void emitSizeof() {
        appendIndentedLines("""
            public static long sizeof() { return $LAYOUT().byteSize(); }
            """);
    }

    private void emitAllocatorAllocate() {
        appendIndentedLines("""
            public static MemorySegment allocate(SegmentAllocator allocator) { return allocator.allocate($LAYOUT()); }
            """);
    }

    private void emitAllocatorAllocateArray() {
        appendIndentedLines("""
            public static MemorySegment allocateArray(long len, SegmentAllocator allocator) {
                return allocator.allocate(MemoryLayout.sequenceLayout(len, $LAYOUT()));
            }
            """);
    }

    private void emitOfAddressScoped() {
        appendIndentedLines("""
            public static MemorySegment ofAddress(MemorySegment addr, Arena scope) {
                return addr.reinterpret($LAYOUT().byteSize(), scope, null);
            }
            """);
    }

    private void emitIndexedFieldGetter(String javaName, Type varType, long offset) {
        String index = safeParameterName("index");
        String seg = safeParameterName("seg");
        Class<?> type = Utils.carrierFor(varType);
        MemoryLayout layout = Type.layoutFor(varType).get();
        appendIndentedLines(STR."""
            public static \{type.getSimpleName()} \{javaName}$get(MemorySegment \{seg}, long \{index}) {
                return \{seg}.get(\{layoutString(1, layout)}, \{offset} + (\{index} * sizeof()));
            }
            """);
    }

    private void emitIndexedFieldSetter(String javaName, Type varType, long offset) {
        String index = safeParameterName("index");
        String seg = safeParameterName("seg");
        String x = safeParameterName("x");
        Class<?> type = Utils.carrierFor(varType);
        MemoryLayout layout = Type.layoutFor(varType).get();
        appendIndentedLines(STR."""
            public static void \{javaName}$set(MemorySegment \{seg}, long \{index}, \{type.getSimpleName()} \{x}) {
                \{seg}.set(\{layoutString(1, layout)}, \{offset} + (\{index} * sizeof()), \{x});
            }
            """);
    }

    private void emitLayoutDecl() {
        MemoryLayout structLayout = Type.layoutFor(structType).get();
        appendIndentedLines(STR."""
            private static final MemoryLayout $LAYOUT = \{layoutString(0, structLayout)};

            public static final MemoryLayout $LAYOUT() {
                return $LAYOUT;
            }
            """);
    }
}
