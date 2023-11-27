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

import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.GroupLayout;
import java.lang.foreign.MemoryLayout;
import java.lang.foreign.SequenceLayout;
import java.lang.foreign.ValueLayout;
import org.openjdk.jextract.Declaration;
import org.openjdk.jextract.Type;
import org.openjdk.jextract.impl.Constants.Constant;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.List;
import java.util.Optional;

/**
 * This class generates static utilities class for C structs, unions.
 */
final class StructBuilder extends ClassSourceBuilder implements OutputFactory.Builder {

    private static final String MEMBER_MODS = "public static";

    private final Declaration.Scoped structTree;
    private final GroupLayout structLayout;
    private final Type structType;
    private final Deque<String> prefixElementNames;
    private final Constants constants;

    StructBuilder(SourceFileBuilder builder, Constants constants, String modifiers, String className,
                  ClassSourceBuilder enclosing, Declaration.Scoped structTree, GroupLayout structLayout) {
        super(builder, modifiers, Kind.CLASS, className, null, enclosing);
        this.structTree = structTree;
        this.structLayout = structLayout;
        this.structType = Type.declared(structTree);
        this.prefixElementNames = new ArrayDeque<>();
        this.constants = constants;
    }

    private String safeParameterName(String paramName) {
        return isEnclosedBySameName(paramName)? paramName + "$" : paramName;
    }

    private void pushPrefixElement(String prefixElementName) {
        prefixElementNames.push(prefixElementName);
    }

    private void popPrefixElement() {
        prefixElementNames.pop();
    }

    private List<String> prefixNamesList() {
        List<String> prefixes = new ArrayList<>(prefixElementNames);
        Collections.reverse(prefixes);
        return Collections.unmodifiableList(prefixes);
    }

    void begin() {
        if (!inAnonymousNested()) {
            if (isNested()) {
                sourceFileBuilder().incrAlign();
            }
            emitDocComment(structTree);
            classBegin();
            Constant layoutConstant = constants.addLayout(((Type.Declared) structType).tree().layout().orElseThrow());
            layoutConstant.emitGetter(this, MEMBER_MODS, Constant::nameSuffix);
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
            popPrefixElement();
        }
    }

    private boolean inAnonymousNested() {
        return !prefixElementNames.isEmpty();
    }

    @Override
    public StructBuilder addStruct(Declaration.Scoped tree, boolean isNestedAnonStruct,
                                   String name, GroupLayout layout) {
        if (isNestedAnonStruct) {
            //nested anon struct - merge into this builder!
            String anonName = layout.name().orElseThrow();
            pushPrefixElement(anonName);
            return this;
        } else {
            StructBuilder builder = new StructBuilder(sourceFileBuilder(), constants, "public static final", name, this, tree, layout);
            builder.begin();
            builder.emitPrivateDefaultConstructor();
            return builder;
        }
    }

    @Override
    public void addFunctionalInterface(Type.Function funcType, String javaName,
                                       FunctionDescriptor descriptor, Optional<List<String>> parameterNames) {
        incrAlign();
        FunctionalInterfaceBuilder.generate(sourceFileBuilder(), constants, javaName, this, funcType, descriptor, parameterNames);
        decrAlign();
    }

    @Override
    public void addVar(Declaration.Variable varTree, String javaName,
        MemoryLayout layout, Optional<String> fiName) {
        String nativeName = varTree.name();
        try {
            structLayout.byteOffset(elementPaths(nativeName));
        } catch (UnsupportedOperationException uoe) {
            // bad layout - do nothing
            OutputFactory.warn("skipping '" + className() + "." + nativeName + "' : " + uoe.toString());
            return;
        }
        if (layout instanceof SequenceLayout || layout instanceof GroupLayout) {
            if (layout.byteSize() > 0) {
                emitSegmentGetter(javaName, nativeName, layout);
            }
        } else if (layout instanceof ValueLayout valueLayout) {
            Constant vhConstant = constants.addFieldVarHandle(nativeName, structLayout, prefixNamesList())
                    .emitGetter(this, MEMBER_MODS, javaName);
            emitFieldDocComment(varTree, "Getter for field:");
            emitFieldGetter(vhConstant, javaName, valueLayout.carrier());
            emitFieldDocComment(varTree, "Setter for field:");
            emitFieldSetter(vhConstant, javaName, valueLayout.carrier());
            emitIndexedFieldGetter(vhConstant, javaName, valueLayout.carrier());
            emitIndexedFieldSetter(vhConstant, javaName, valueLayout.carrier());
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

    private void emitFieldGetter(Constant vhConstant, String javaName, Class<?> type) {
        String seg = safeParameterName("seg");
        appendIndentedLines(STR."""
            public static \{type.getSimpleName()} \{javaName}$get(MemorySegment \{seg}) {
                return (\{type.getName()}) \{vhConstant}.get(\{seg}, 0L);
            }
            """);
    }

    private void emitFieldSetter(Constant vhConstant, String javaName, Class<?> type) {
        String seg = safeParameterName("seg");
        String x = safeParameterName("x");
        appendIndentedLines(STR."""
            public static void \{javaName}$set(MemorySegment \{seg}, \{type.getSimpleName()} \{x}) {
                \{vhConstant}.set(\{seg}, 0L, \{x});
            }
            """);
    }

    private MemoryLayout.PathElement[] elementPaths(String nativeFieldName) {
        List<String> prefixElements = prefixNamesList();
        MemoryLayout.PathElement[] elems = new MemoryLayout.PathElement[prefixElements.size() + 1];
        int i = 0;
        for (; i < prefixElements.size(); i++) {
            elems[i] = MemoryLayout.PathElement.groupElement(prefixElements.get(i));
        }
        elems[i] = MemoryLayout.PathElement.groupElement(nativeFieldName);
        return elems;
    }

    private void emitSegmentGetter(String javaName, String nativeName, MemoryLayout layout) {
        String seg = safeParameterName("seg");
        appendIndentedLines(STR."""
            public static MemorySegment \{javaName}$slice(MemorySegment \{seg}) {
                return \{seg}.asSlice(\{structLayout.byteOffset(elementPaths(nativeName))}, \{layout.byteSize()});
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
                return RuntimeHelper.asArray(addr, $LAYOUT(), 1, scope);
            }
            """);
    }

    private void emitIndexedFieldGetter(Constant vhConstant, String javaName, Class<?> type) {
        String index = safeParameterName("index");
        String seg = safeParameterName("seg");
        appendIndentedLines(STR."""
            public static \{type.getSimpleName()} \{javaName}$get(MemorySegment \{seg}, long \{index}) {
                return (\{type.getName()}) \{vhConstant}.get(\{seg}, \{index} * sizeof());
            }
            """);
    }

    private void emitIndexedFieldSetter(Constant vhConstant, String javaName, Class<?> type) {
        String index = safeParameterName("index");
        String seg = safeParameterName("seg");
        String x = safeParameterName("x");
        appendIndentedLines(STR."""
            public static void \{javaName}$set(MemorySegment \{seg}, long \{index}, \{type.getSimpleName()} \{x}) {
                \{vhConstant}.set(\{seg}, \{index} * sizeof(), \{x});
            }
            """);
    }
}
