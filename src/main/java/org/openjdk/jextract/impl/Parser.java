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
package org.openjdk.jextract.impl;

import org.openjdk.jextract.Declaration;
import org.openjdk.jextract.Position;
import org.openjdk.jextract.clang.Cursor;
import org.openjdk.jextract.clang.CursorKind;
import org.openjdk.jextract.clang.Diagnostic;
import org.openjdk.jextract.clang.Index;
import org.openjdk.jextract.clang.LibClang;
import org.openjdk.jextract.clang.SourceLocation;
import org.openjdk.jextract.clang.SourceRange;
import org.openjdk.jextract.clang.TranslationUnit;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

public class Parser {
    private final TreeMaker treeMaker;
    private final Logger logger;

    public Parser(Logger logger) {
        this.treeMaker = new TreeMaker(logger);
        this.logger = logger;
    }

    private Declaration.Scoped collectDeclarations(TranslationUnit tu, MacroParserImpl macroParser) {
        List<Declaration> decls = new ArrayList<>();
        Cursor tuCursor = tu.getCursor();
        tuCursor.forEach(c -> {
            SourceLocation loc = c.getSourceLocation();
            if (loc == null) {
                return;
            }

            SourceLocation.Location src = loc.getFileLocation();
            if (src == null) {
                return;
            }

            if (c.isDeclaration()) {
                parseDeclaration(c, decls);
            } else if (isMacro(c) && src.path() != null) {
                SourceRange range = c.getExtent();
                String[] tokens = c.getTranslationUnit().tokens(range);
                Optional<Declaration.Constant> constant = macroParser.parseConstant(c, c.spelling(), tokens);
                if (constant.isPresent()) {
                    decls.add(constant.get());
                }
            }
        });

        decls.addAll(macroParser.macroTable.reparseConstants());
        Declaration.Scoped rv = treeMaker.createHeader(tuCursor, decls);
        return rv;
    }

    private void parseDeclaration(Cursor c, List<Declaration> decls) {
        if (c.kind() == CursorKind.UnexposedDecl || c.kind() == CursorKind.Namespace) {
            c.forEach(t -> parseDeclaration(t, decls));
        } else {
            Declaration decl = treeMaker.createTree(c);
            if (decl != null) {
                decls.add(decl);
            }
        }
    }

    public Declaration.Scoped parse(String name, String content, Collection<String> args) {
        try (Index index = LibClang.createIndex(false) ;
             TranslationUnit tu = index.parse(name, content,
                d -> {
                    Position pos = asPosition(d.location().getSpellingLocation());
                    if (d.severity() > Diagnostic.CXDiagnostic_Warning) {
                        logger.clangErr(pos, d.spelling());
                    } else if (d.severity() == Diagnostic.CXDiagnostic_Warning) {
                        logger.clangWarn(pos, d.spelling());
                    } else if (d.severity() == Diagnostic.CXDiagnostic_Note) {
                        logger.clangInfo(pos, d.spelling());
                    }
                },
            true, args.toArray(new String[0])) ;
            MacroParserImpl macroParser = MacroParserImpl.make(treeMaker, logger, tu, args)) {
            return collectDeclarations(tu, macroParser);
        }
    }

    private Position asPosition(SourceLocation.Location loc) {
        record PositionRecord(Path path, int line, int col) implements Position {}

        return loc.path() == null ? Position.NO_POSITION :
               new PositionRecord(loc.path(), loc.line(), loc.column());
    }

    private boolean isMacro(Cursor c) {
        return c.isPreprocessing() && c.kind() == CursorKind.MacroDefinition;
    }
}
