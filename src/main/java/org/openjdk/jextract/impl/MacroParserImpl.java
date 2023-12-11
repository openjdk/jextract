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
import org.openjdk.jextract.Type;
import org.openjdk.jextract.JextractTool;
import org.openjdk.jextract.clang.Cursor;
import org.openjdk.jextract.clang.CursorKind;
import org.openjdk.jextract.clang.Diagnostic;
import org.openjdk.jextract.clang.EvalResult;
import org.openjdk.jextract.clang.Index;
import org.openjdk.jextract.clang.LibClang;
import org.openjdk.jextract.clang.TranslationUnit;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

class MacroParserImpl implements AutoCloseable {

    private final ClangReparser reparser;
    private final TreeMaker treeMaker;
    final MacroTable macroTable;

    private MacroParserImpl(ClangReparser reparser, TreeMaker treeMaker) {
        this.reparser = reparser;
        this.treeMaker = treeMaker;
        this.macroTable = new MacroTable();
    }

    static MacroParserImpl make(TreeMaker treeMaker, TranslationUnit tu, Collection<String> args) {
        ClangReparser reparser;
        try {
            reparser = new ClangReparser(tu, args);
        } catch (IOException | Index.ParsingFailedException ex) {
            throw new RuntimeException(ex);
        }

        return new MacroParserImpl(reparser, treeMaker);
    }

    /**
     * This method attempts to evaluate the macro. Evaluation occurs in two steps: first, an attempt is made
     * to see if the macro corresponds to a simple numeric constant. If so, the constant is parsed in Java directly.
     * If that is not possible (e.g. because the macro refers to other macro, or has a more complex grammar), fall
     * back to use clang evaluation support.
     */
    Optional<Declaration.Constant> parseConstant(Cursor cursor, String name, String[] tokens) {
        if (cursor.isMacroFunctionLike()) {
            return Optional.empty();
        } else if (tokens.length == 2) {
            //check for fast path
            Integer num = toNumber(tokens[1]);
            if (num != null) {
                return Optional.of(treeMaker.createMacro(TreeMaker.CursorPosition.of(cursor), name, Type.primitive(Type.Primitive.Kind.Int), (long)num));
            }
        }
        macroTable.enterMacro(name, tokens, TreeMaker.CursorPosition.of(cursor));
        return Optional.empty();
    }

    private Integer toNumber(String str) {
        try {
            // Integer.decode supports '#' hex literals which is not valid in C.
            return str.length() > 0 && str.charAt(0) != '#'? Integer.decode(str) : null;
        } catch (NumberFormatException nfe) {
            return null;
        }
    }

    /**
     * This class allows client to reparse a snippet of code against a given set of include files.
     * For performance reasons, the set of includes (which comes from the jextract parser) is compiled
     * into a precompiled header, so as to speed to incremental recompilation of the generated snippets.
     */
    static class ClangReparser {
        final Path macro;
        final Index macroIndex = LibClang.createIndex(true);
        final TranslationUnit macroUnit;

        public ClangReparser(TranslationUnit tu, Collection<String> args) throws IOException, Index.ParsingFailedException {
            Path precompiled = Files.createTempFile("jextract$", ".pch");
            precompiled.toFile().deleteOnExit();
            tu.save(precompiled);
            this.macro = Files.createTempFile("jextract$", ".h");
            this.macro.toFile().deleteOnExit();
            String[] patchedArgs = Stream.concat(
                Stream.of(
                    // Avoid system search path, use bundled instead
                    "-nostdinc",
                    "-ferror-limit=0",
                    // precompiled header
                    "-include-pch", precompiled.toAbsolutePath().toString()),
                args.stream()).toArray(String[]::new);
            this.macroUnit = macroIndex.parse(macro.toAbsolutePath().toString(),
                    this::processDiagnostics,
                    false, //add serialization support (needed for macros)
                    patchedArgs);
        }

        void processDiagnostics(Diagnostic diag) {
            if (JextractTool.DEBUG) {
                System.err.println("Error while processing macro: " + diag.spelling());
            }
        }

        public Cursor reparse(String snippet) {
            macroUnit.reparse(this::processDiagnostics,
                    Index.UnsavedFile.of(macro, snippet));
            return macroUnit.getCursor();
        }
    }

    /**
     * This abstraction is used to collect all macros which could not be interpreted during {@link #parseConstant(Position, String, String[])}.
     * All unparsed macros in the table can have three different states: UNPARSED (which means the macro has not been parsed yet),
     * SUCCESS (which means the macro has been parsed and has a type and a value) and FAILURE, which means the macro has been
     * parsed with some errors, but for which we were at least able to infer a type.
     *
     * The reparsing process goes as follows:
     * 1. all unparsed macros are added to the table in the UNPARSED state.
     * 2. a snippet for all macros in the UNPARSED state is compiled and the table state is updated
     * 3. a recovery snippet for all macros in the FAILURE state is compiled and the table state is updated again
     * 4. we repeat from (2) until no further progress is made.
     * 5. we return a list of macro which are in the SUCCESS state.
     *
     * State transitions in the table are as follows:
     * - an UNPARSED macro can go to either SUCCESS, to FAILURE or be removed (if not even a type can be inferred)
     * - a FAILURE macro can go to either SUCCESS (if recovery step succeds) or be removed
     * - a SUCCESS macro cannot go in any other state
     */
    class MacroTable {

        final Map<String, Entry> macrosByMangledName = new LinkedHashMap<>();

        abstract class Entry {
            final String name;
            final String[] tokens;
            final Position position;

            Entry(String name, String[] tokens, Position position) {
                this.name = name;
                this.tokens = tokens;
                this.position = position;
            }

            String mangledName() {
                return "jextract$macro$" + name;
            }

            Entry success(Type type, Object value) {
                throw new IllegalStateException();
            }

            Entry failure(Type type) {
                throw new IllegalStateException();
            }

            boolean isSuccess() {
                return false;
            }
            boolean isRecoverableFailure() {
                return false;
            }
            boolean isUnparsed() {
                return false;
            }

            void update() {
                macrosByMangledName.put(mangledName(), this);
            }
        }

        class Unparsed extends Entry {
            Unparsed(String name, String[] tokens, Position position) {
                super(name, tokens, position);
            }

            @Override
            Entry success(Type type, Object value) {
                return new Success(name, tokens, position, type, value);
            }

            @Override
            Entry failure(Type type) {
                return type != null ?
                        new RecoverableFailure(name, tokens, type, position) :
                        new UnparseableMacro(name, tokens, position);
            }

            @Override
            boolean isUnparsed() {
                return true;
            }

            @Override
            void update() {
                throw new IllegalStateException();
            }
        }

        class RecoverableFailure extends Entry {

            final Type type;

            public RecoverableFailure(String name, String[] tokens, Type type, Position position) {
                super(name, tokens, position);
                this.type = type;
            }

            @Override
            Entry success(Type type, Object value) {
                return new Success(name, tokens, position, this.type, value);
            }

            @Override
            Entry failure(Type type) {
                return new UnparseableMacro(name, tokens, position);
            }

            @Override
            boolean isRecoverableFailure() {
                return true;
            }
        }

        class Success extends Entry {
            final Declaration.Constant constant;

            public Success(String name, String[] tokens, Position position, Type type, Object value) {
                super(name, tokens, position);
                constant = treeMaker.createMacro(position, name, type, value);
            }

            @Override
            boolean isSuccess() {
                return true;
            }

            Declaration.Constant constant() {
                return constant;
            }
        }

        class UnparseableMacro extends Entry {

            UnparseableMacro(String name, String[] tokens, Position position) {
                super(name, tokens, position);
            }

            @Override
            void update() {
                macrosByMangledName.remove(mangledName());
            }
        };

        void enterMacro(String name, String[] tokens, Position position) {
            Unparsed unparsed = new Unparsed(name, tokens, position);
            macrosByMangledName.put(unparsed.mangledName(), unparsed);
        }

        public List<Declaration.Constant> reparseConstants() {
            int last = -1;
            while (macrosByMangledName.size() > 0 && last != macrosByMangledName.size()) {
                last = macrosByMangledName.size();
                // step 1 - try parsing macros as var declarations
                reparseMacros(false);
                // step 2 - retry failed parsed macros as pointers
                reparseMacros(true);
            }
            return macrosByMangledName.values().stream()
                    .filter(Entry::isSuccess)
                    .map(e -> ((Success) e).constant())
                    .collect(Collectors.toList());
        }

        void updateTable(TreeMaker treeMaker, Cursor decl) {
            String mangledName = decl.spelling();
            Entry entry = macrosByMangledName.get(mangledName);
            try (EvalResult result = decl.eval()) {
                Entry newEntry = switch (result.getKind()) {
                    case Integral -> {
                        long value = result.getAsInt();
                        yield entry.success(treeMaker.toType(decl), value);
                    }
                    case FloatingPoint -> {
                        double value = result.getAsFloat();
                        yield entry.success(treeMaker.toType(decl), value);
                    }
                    case StrLiteral -> {
                        String value = result.getAsString();
                        yield entry.success(treeMaker.toType(decl), value);
                    }
                    default -> {
                        Type type = decl.type().equals(decl.type().canonicalType()) ?
                                null : treeMaker.toType(decl);
                        yield entry.failure(type);
                    }
                };
                newEntry.update();
            }
        }

        void reparseMacros(boolean recovery) {
            String snippet = macroDecl(recovery);
            TreeMaker treeMaker = new TreeMaker(MacroParserImpl.this.treeMaker);
            reparser.reparse(snippet).forEach(c -> {
                if (c.kind() == CursorKind.VarDecl &&
                        c.spelling().contains("jextract$")) {
                    updateTable(treeMaker, c);
                }
            });
        }

        String macroDecl(boolean recovery) {
            StringBuilder buf = new StringBuilder();
            if (recovery) {
                buf.append("#include <stdint.h>\n");
            }
            macrosByMangledName.values().stream()
                    .filter(e -> !e.isSuccess()) // skip macros that already have passed
                    .filter(recovery ? Entry::isRecoverableFailure : Entry::isUnparsed)
                    .forEach(e -> {
                        buf.append("__auto_type ")
                                .append(e.mangledName())
                                .append(" = ");
                        if (recovery) {
                            buf.append("(uintptr_t)");
                        }
                        buf.append(e.name)
                                .append(";\n");
                    });
            return buf.toString();
        }
    }

    @Override
    public void close() {
        reparser.macroUnit.close();
        reparser.macroIndex.close();
    }
}
