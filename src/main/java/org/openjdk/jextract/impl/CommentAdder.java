/*
 * Copyright (c) 2026, Oracle and/or its affiliates. All rights reserved.
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

import java.util.List;

/*
 * This visitor adds comments to declarations.
 */
public final class CommentAdder implements Declaration.Visitor<Void, List<String>> {

    public CommentAdder() {
    }

    @Override
    public Void visitConstant(Declaration.Constant constant, List<String> comments) {
        if (constant instanceof DeclarationImpl.ConstantImpl constantImpl) {
            constantImpl.setComments(comments);
            return null;
        }
        throw new AssertionError("constant not instance of ConstantImpl");
    }

    @Override
    public Void visitFunction(Declaration.Function function, List<String> comments) {
        if (function instanceof DeclarationImpl.FunctionImpl functionImpl) {
            functionImpl.setComments(comments);
            return null;
        }
        throw new AssertionError("function not instance of FunctionImpl");
    }

    @Override
    public Void visitTypedef(Declaration.Typedef typedef, List<String> comments) {
        if (typedef instanceof DeclarationImpl.TypedefImpl typedefImpl) {
            typedefImpl.setComments(comments);
            return null;
        }
        throw new AssertionError("typedef not instance of TypedefImpl");
    }

    @Override
    public Void visitVariable(Declaration.Variable variable, List<String> comments) {
        if (variable instanceof DeclarationImpl.VariableImpl variableImpl) {
            variableImpl.setComments(comments);
            return null;
        }
        throw new AssertionError("variable not instance of VariableImpl");
    }

    @Override
    public Void visitScoped(Declaration.Scoped scoped, List<String> comments) {
        if (scoped instanceof DeclarationImpl.ScopedImpl scopedImpl) {
            scopedImpl.setComments(comments);
            return null;
        }
        throw new AssertionError("scoped not instance of ScopedImpl");
    }
}
