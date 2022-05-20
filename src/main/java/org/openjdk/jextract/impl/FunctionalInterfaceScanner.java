/*
 * Copyright (c) 2020, Oracle and/or its affiliates. All rights reserved.
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
import org.openjdk.jextract.Type;
import java.lang.foreign.FunctionDescriptor;

import java.util.Optional;
import java.util.Set;

class FunctionalInterfaceScanner implements Declaration.Visitor<Void, Set<FunctionDescriptor>> {

    private final Set<FunctionDescriptor> descriptors;

    FunctionalInterfaceScanner(Set<FunctionDescriptor> descriptors) {
        this.descriptors = descriptors;
    }

    Declaration.Scoped scan(Declaration.Scoped decl) {
        decl.accept(this, descriptors);
        return decl;
    }

    void scanType(Type t, Set<FunctionDescriptor> functionDescriptors) {
        t.accept(new TypeScanner(), functionDescriptors);
    }

    @Override
    public Void visitScoped(Declaration.Scoped d, Set<FunctionDescriptor> functionDescriptors) {
        d.members().forEach(m -> m.accept(this, functionDescriptors));
        return null;
    }

    @Override
    public Void visitFunction(Declaration.Function d, Set<FunctionDescriptor> functionDescriptors) {
        scanType(d.type().returnType(), functionDescriptors);
        d.parameters().forEach(p -> p.accept(this, functionDescriptors));
        return null;
    }

    @Override
    public Void visitVariable(Declaration.Variable d, Set<FunctionDescriptor> functionDescriptors) {
        scanType(d.type(), functionDescriptors);
        return null;
    }

    @Override
    public Void visitConstant(Declaration.Constant d, Set<FunctionDescriptor> functionDescriptors) {
        scanType(d.type(), functionDescriptors);
        return null;
    }

    static class TypeScanner implements Type.Visitor<Void, Set<FunctionDescriptor>> {

        @Override
        public Void visitPrimitive(Type.Primitive t, Set<FunctionDescriptor> functionDescriptors) {
            return null;
        }

        @Override
        public Void visitDelegated(Type.Delegated t, Set<FunctionDescriptor> functionDescriptors) {
            return t.type().accept(this, functionDescriptors);
        }

        @Override
        public Void visitFunction(Type.Function t, Set<FunctionDescriptor> functionDescriptors) {
            t.returnType().accept(this, functionDescriptors);
            t.argumentTypes().forEach(a -> a.accept(this, functionDescriptors));
            Optional<FunctionDescriptor> descriptor = Type.descriptorFor(t);
            if (descriptor.isPresent()) {
                functionDescriptors.add(descriptor.get());
            }
            return null;
        }

        @Override
        public Void visitDeclared(Type.Declared t, Set<FunctionDescriptor> functionDescriptors) {
            return null;
        }

        @Override
        public Void visitArray(Type.Array t, Set<FunctionDescriptor> functionDescriptors) {
            return t.elementType().accept(this, functionDescriptors);
        }

        @Override
        public Void visitType(Type t, Set<FunctionDescriptor> functionDescriptors) {
            throw new UnsupportedOperationException();
        }
    }
}
