/*
 * Copyright (c) 2020, 2022, Oracle and/or its affiliates. All rights reserved.
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
import java.lang.foreign.MemorySegment;
import java.lang.foreign.SequenceLayout;
import java.lang.foreign.UnionLayout;
import java.lang.foreign.ValueLayout;
import org.openjdk.jextract.Declaration;
import org.openjdk.jextract.Type;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.List;
import java.util.Optional;

/**
 * This class generates static utilities class for C structs, unions.
 */
class StructBuilder extends ConstantBuilder {

    private static final String MEMBER_MODS = "public static";

    private final Declaration.Scoped structTree;
    private final GroupLayout structLayout;
    private final Type structType;
    private final Deque<String> prefixElementNames;

    StructBuilder(JavaSourceBuilder enclosing, Declaration.Scoped structTree,
        String name, GroupLayout structLayout) {
        super(enclosing, name);
        this.structTree = structTree;
        this.structLayout = structLayout;
        this.structType = Type.declared(structTree);
        prefixElementNames = new ArrayDeque<>();
    }

    private String safeParameterName(String paramName) {
        return isEnclosedBySameName(paramName)? paramName + "$" : paramName;
    }

    void pushPrefixElement(String prefixElementName) {
        prefixElementNames.push(prefixElementName);
    }

    void popPrefixElement() {
        prefixElementNames.pop();
    }

    private List<String> prefixNamesList() {
        List<String> prefixes = new ArrayList<>(prefixElementNames);
        Collections.reverse(prefixes);
        return Collections.unmodifiableList(prefixes);
    }

    @Override
    void classBegin() {
        if (!inAnonymousNested()) {
            super.classBegin();
            addLayout(layoutField(), ((Type.Declared) structType).tree().layout().orElseThrow())
                    .emitGetter(this, MEMBER_MODS, Constant.SUFFIX_ONLY);
        }
    }

    @Override
    void classDeclBegin() {
        if (!inAnonymousNested()) {
            emitDocComment(structTree);
        }
    }

    @Override
    JavaSourceBuilder classEnd() {
        if (!inAnonymousNested()) {
            emitSizeof();
            emitAllocatorAllocate();
            emitAllocatorAllocateArray();
            emitOfAddressScoped();
            return super.classEnd();
        } else {
            // we're in an anonymous struct which got merged into this one, return this very builder and keep it open
            popPrefixElement();
            return this;
        }
    }

    boolean inAnonymousNested() {
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
            return new StructBuilder(this, tree, name, layout);
        }
    }

    @Override
    public void addFunctionalInterface(Type.Function funcType, String javaName,
        FunctionDescriptor descriptor, Optional<List<String>> parameterNames) {
        FunctionalInterfaceBuilder builder = new FunctionalInterfaceBuilder(this, funcType, javaName, descriptor, parameterNames);
        builder.classBegin();
        builder.classEnd();
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
            Constant vhConstant = addFieldVarHandle(javaName, nativeName, valueLayout, layoutField(), prefixNamesList())
                    .emitGetter(this, MEMBER_MODS, Constant.QUALIFIED_NAME);
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
        incrAlign();
        indent();
        append(MEMBER_MODS + " ");
        append(fiName + " " + javaName + "(MemorySegment segment, MemorySession session) {\n");
        incrAlign();
        indent();
        append("return " + fiName + ".ofAddress(" + javaName + "$get(segment), session);\n");
        decrAlign();
        indent();
        append("}\n");
        decrAlign();
    }

    private void emitFieldGetter(Constant vhConstant, String javaName, Class<?> type) {
        incrAlign();
        indent();
        String seg = safeParameterName("seg");
        append(MEMBER_MODS + " " + type.getSimpleName() + " " + javaName + "$get(MemorySegment " + seg + ") {\n");
        incrAlign();
        indent();
        append("return (" + type.getName() + ")"
                + vhConstant.accessExpression() + ".get(" + seg + ");\n");
        decrAlign();
        indent();
        append("}\n");
        decrAlign();
    }

    private void emitFieldSetter(Constant vhConstant, String javaName, Class<?> type) {
        incrAlign();
        indent();
        String seg = safeParameterName("seg");
        String x = safeParameterName("x");
        String param = MemorySegment.class.getSimpleName() + " " + seg;
        append(MEMBER_MODS + " void " + javaName + "$set(" + param + ", " + type.getSimpleName() + " " + x + ") {\n");
        incrAlign();
        indent();
        append(vhConstant.accessExpression() + ".set(" + seg + ", " + x + ");\n");
        decrAlign();
        indent();
        append("}\n");
        decrAlign();
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
        incrAlign();
        indent();
        String seg = safeParameterName("seg");
        append(MEMBER_MODS + " MemorySegment " + javaName + "$slice(MemorySegment " + seg + ") {\n");
        incrAlign();
        indent();
        append("return " + seg + ".asSlice(");
        append(structLayout.byteOffset(elementPaths(nativeName)));
        append(", ");
        append(layout.byteSize());
        append(");\n");
        decrAlign();
        indent();
        append("}\n");
        decrAlign();
    }

    private void emitSizeof() {
        incrAlign();
        indent();
        append(MEMBER_MODS);
        append(" long sizeof() { return $LAYOUT().byteSize(); }\n");
        decrAlign();
    }

    private void emitAllocatorAllocate() {
        incrAlign();
        indent();
        append(MEMBER_MODS);
        append(" MemorySegment allocate(SegmentAllocator allocator) { return allocator.allocate($LAYOUT()); }\n");
        decrAlign();
    }

    private void emitAllocatorAllocateArray() {
        incrAlign();
        indent();
        append(MEMBER_MODS);
        append(" MemorySegment allocateArray(long len, SegmentAllocator allocator) {\n");
        incrAlign();
        indent();
        append("return allocator.allocate(MemoryLayout.sequenceLayout(len, $LAYOUT()));\n");
        decrAlign();
        indent();
        append("}\n");
        decrAlign();
    }

    private void emitOfAddressScoped() {
        incrAlign();
        indent();
        append(MEMBER_MODS);
        append(" MemorySegment ofAddress(MemorySegment addr, MemorySession session) { return RuntimeHelper.asArray(addr, $LAYOUT(), 1, session); }\n");
        decrAlign();
    }

    private void emitIndexedFieldGetter(Constant vhConstant, String javaName, Class<?> type) {
        incrAlign();
        indent();
        String index = safeParameterName("index");
        String seg = safeParameterName("seg");
        String params = MemorySegment.class.getSimpleName() + " " + seg + ", long " + index;
        append(MEMBER_MODS + " " + type.getSimpleName() + " " + javaName + "$get(" + params + ") {\n");
        incrAlign();
        indent();
        append("return (" + type.getName() + ")");
        append(vhConstant.accessExpression());
        append(".get(");
        append(seg);
        append(".asSlice(");
        append(index);
        append("*sizeof()));\n");
        decrAlign();
        indent();
        append("}\n");
        decrAlign();
    }

    private void emitIndexedFieldSetter(Constant vhConstant, String javaName, Class<?> type) {
        incrAlign();
        indent();
        String index = safeParameterName("index");
        String seg = safeParameterName("seg");
        String x = safeParameterName("x");
        String params = MemorySegment.class.getSimpleName() + " " + seg +
            ", long " + index + ", " + type.getSimpleName() + " " + x;
        append(MEMBER_MODS + " void " + javaName + "$set(" + params + ") {\n");
        incrAlign();
        indent();
        append(vhConstant.accessExpression());
        append(".set(");
        append(seg);
        append(".asSlice(");
        append(index);
        append("*sizeof()), ");
        append(x);
        append(");\n");
        decrAlign();
        indent();
        append("}\n");
        decrAlign();
    }

    private String qualifiedName(ClassSourceBuilder builder) {
        if (builder.isNested()) {
            String prefix = qualifiedName((ClassSourceBuilder)builder.enclosing);
            return prefix.isEmpty() ?
                    builder.className() :
                    prefix + "$" + builder.className();
        } else {
            return "";
        }
    }

    private String layoutField() {
        String suffix = (structLayout instanceof UnionLayout) ? "union" : "struct";
        return qualifiedName(this) + "$" + suffix;
    }
}
