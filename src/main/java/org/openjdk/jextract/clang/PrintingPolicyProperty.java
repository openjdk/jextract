/*
 *  Copyright (c) 2020, Oracle and/or its affiliates. All rights reserved.
 *  DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 *  This code is free software; you can redistribute it and/or modify it
 *  under the terms of the GNU General Public License version 2 only, as
 *  published by the Free Software Foundation.  Oracle designates this
 *  particular file as subject to the "Classpath" exception as provided
 *  by Oracle in the LICENSE file that accompanied this code.
 *
 *  This code is distributed in the hope that it will be useful, but WITHOUT
 *  ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 *  FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 *  version 2 for more details (a copy is included in the LICENSE file that
 *  accompanied this code).
 *
 *  You should have received a copy of the GNU General Public License version
 *  2 along with this work; if not, write to the Free Software Foundation,
 *  Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 *   Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 *  or visit www.oracle.com if you need additional information or have any
 *  questions.
 *
 */

package org.openjdk.jextract.clang;

import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;
import static org.openjdk.jextract.clang.libclang.Index_h.*;

public enum PrintingPolicyProperty {
    Indentation(CXPrintingPolicy_Indentation()),
    SuppressSpecifiers(CXPrintingPolicy_SuppressSpecifiers()),
    SuppressTagKeyword(CXPrintingPolicy_SuppressTagKeyword()),
    IncludeTagDefinition(CXPrintingPolicy_IncludeTagDefinition()),
    SuppressScope(CXPrintingPolicy_SuppressScope()),
    SuppressUnwrittenScope(CXPrintingPolicy_SuppressUnwrittenScope()),
    SuppressInitializers(CXPrintingPolicy_SuppressInitializers()),
    ConstantArraySizeAsWritten(CXPrintingPolicy_ConstantArraySizeAsWritten()),
    AnonymousTagLocations(CXPrintingPolicy_AnonymousTagLocations()),
    SuppressStrongLifetime(CXPrintingPolicy_SuppressStrongLifetime()),
    SuppressLifetimeQualifiers(CXPrintingPolicy_SuppressLifetimeQualifiers()),
    SuppressTemplateArgsInCXXConstructors(CXPrintingPolicy_SuppressTemplateArgsInCXXConstructors()),
    Bool(CXPrintingPolicy_Bool()),
    Restrict(CXPrintingPolicy_Restrict()),
    Alignof(CXPrintingPolicy_Alignof()),
    UnderscoreAlignof(CXPrintingPolicy_UnderscoreAlignof()),
    UseVoidForZeroParams(CXPrintingPolicy_UseVoidForZeroParams()),
    TerseOutput(CXPrintingPolicy_TerseOutput()),
    PolishForDeclaration(CXPrintingPolicy_PolishForDeclaration()),
    Half(CXPrintingPolicy_Half()),
    MSWChar(CXPrintingPolicy_MSWChar()),
    IncludeNewlines(CXPrintingPolicy_IncludeNewlines()),
    MSVCFormatting(CXPrintingPolicy_MSVCFormatting()),
    ConstantsAsWritten(CXPrintingPolicy_ConstantsAsWritten()),
    SuppressImplicitBase(CXPrintingPolicy_SuppressImplicitBase()),
    FullyQualifiedName(CXPrintingPolicy_FullyQualifiedName()),
    LastProperty(CXPrintingPolicy_LastProperty());

    private final int value;

    PrintingPolicyProperty(int value) {
        this.value = value;
    }

    public int value() {
        return value;
    }

    private final static Map<Integer, PrintingPolicyProperty> lookup;

    static {
        lookup = new HashMap<>();
        for (PrintingPolicyProperty e: PrintingPolicyProperty.values()) {
            lookup.put(e.value(), e);
        }
    }

    public final static PrintingPolicyProperty valueOf(int value) {
        PrintingPolicyProperty x = lookup.get(value);
        if (null == x) {
            throw new NoSuchElementException("Invalid PrintingPolicyProperty value: " + value);
        }
        return x;
    }
}
