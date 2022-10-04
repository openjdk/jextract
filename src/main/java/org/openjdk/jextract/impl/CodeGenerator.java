/*
 * Copyright (c) 2022 Oracle and/or its affiliates. All rights reserved.
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
import java.util.stream.Stream;
import javax.tools.JavaFileObject;

public final class CodeGenerator {
    private CodeGenerator() {}

    public static JavaFileObject[] generate(Declaration.Scoped decl, String headerName,
                    String targetPkg, IncludeHelper includeHelper,
                    List<String> libNames) {
        var nameMangler = new NameMangler(headerName);
        var transformedDecl = Stream.of(decl).
            map(new IncludeFilter(includeHelper)::transform).
            map(new EnumConstantLifter()::transform).
            map(new DuplicateFilter()::transform).
            map(nameMangler::scan).
            findFirst().get();
        return OutputFactory.generateWrapped(transformedDecl, targetPkg, libNames, nameMangler);
    }
}
