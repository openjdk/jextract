/*
 *  Copyright (c) 2020, 2024, Oracle and/or its affiliates. All rights reserved.
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

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import org.openjdk.jextract.Declaration;
import org.openjdk.jextract.Declaration.ClangAttributes;
import org.openjdk.jextract.Declaration.Scoped;
import org.openjdk.jextract.Declaration.Typedef;
import org.openjdk.jextract.Declaration.Variable;
import org.openjdk.jextract.Position;
import org.openjdk.jextract.Type;
import org.openjdk.jextract.Type.Declared;
import org.openjdk.jextract.clang.Cursor;
import org.openjdk.jextract.clang.CursorKind;
import org.openjdk.jextract.clang.CursorLanguage;
import org.openjdk.jextract.clang.LinkageKind;
import org.openjdk.jextract.clang.PrintingPolicy;
import org.openjdk.jextract.clang.PrintingPolicyProperty;
import org.openjdk.jextract.clang.SourceLocation;
import org.openjdk.jextract.clang.TranslationUnit;
import org.openjdk.jextract.clang.TypeKind;
import org.openjdk.jextract.clang.libclang.Index_h;
import org.openjdk.jextract.impl.DeclarationImpl.AnonymousStruct;
import org.openjdk.jextract.impl.DeclarationImpl.ClangAlignOf;
import org.openjdk.jextract.impl.DeclarationImpl.ClangOffsetOf;
import org.openjdk.jextract.impl.DeclarationImpl.ClangSizeOf;
import org.openjdk.jextract.impl.DeclarationImpl.NestedDeclarations;
import org.openjdk.jextract.impl.DeclarationImpl.DeclarationString;

/**
 * This class turns a clang cursor into a jextract declaration. All declarations are de-duplicated,
 * based on the declaration position. Because of this, the tree maker's declaration cache effectively
 * acts as a symbol table.
 */
class TreeMaker {

    private static final int COPY_COMMENT_BATCH_SIZE = Math.max(1, Integer.getInteger("jextract.copy.comment.batch.size", 10));

    private final boolean copyComments;

    private final Map<Cursor.Key, Declaration> declarationCache = new HashMap<>();

    public TreeMaker(boolean copyComments) {
        this.copyComments = copyComments;
    }

    Declaration addAttributes(Declaration d, Cursor c) {
        if (d == null) return null;
        Map<String, List<String>> attributes = new HashMap<>();
        c.forEach(child -> {
            if (child.isAttribute()) {
                List<String> attrs = attributes.computeIfAbsent(child.kind().name(), _unused -> new ArrayList<>());
                attrs.add(child.spelling());
            }
        });
        if (!attributes.isEmpty()) {
            d.addAttribute(new ClangAttributes(Collections.unmodifiableMap(attributes)));
        }
        return d;
    }

    public Optional<Declaration> lookup(Cursor.Key key) {
        return Optional.ofNullable(declarationCache.get(key));
    }

    public Declaration createTree(Cursor c) {
        return createTree(c, false);
    }

    public Declaration createTree(Cursor c, boolean copyComments) {
        Objects.requireNonNull(c);
        CursorLanguage lang = c.language();
        LinkageKind linkage = c.linkage();

        /*
         * We detect non-C constructs to early exit with error for
         * unsupported features. But libclang maps both C11's _Static_assert
         * and C++11's static_assert to same CursorKind. But the language is
         * set a C++ always. Because we want to allow C11's _Static_Assert,
         * we allow that exception here.
         */
        if (lang != CursorLanguage.C && lang != CursorLanguage.Invalid &&
                c.kind() != CursorKind.StaticAssert) {
            throw new RuntimeException("Unsupported language: " + c.language());
        }

        // If we can clearly determine internal linkage, then filter it.
        if (linkage == LinkageKind.Internal) {
            return null;
        }

        // filter inline functions
        if (c.isFunctionInlined()) {
            return null;
        }
        var rv = (DeclarationImpl) createTreeInternal(c);
        if (rv != null && copyComments) {
            String rawCommentText = c.getRawCommentText();
            if (rawCommentText != null) {
                List<String> comments = normalizeComments(rawCommentTextToCommentList(rawCommentText));
                DeclarationImpl.DeclarationComments.with(rv, comments);
            }
        }
        return addAttributes(rv, c);
    }

    /**
     * This function normalizes the comment contents, and returns a list of cleaned up
     * comment lines. Here, normalization means that
     *
     * <ul>
     *     <li>(Doxygen) comment delimiters are removed,</li>
     *     <li>leading asterixes are removed,</li>
     *     <li>comments with newlines are split up.</li>
     * </ul>
     */
    private static List<String> normalizeComments(List<String> rawComments) {
        return rawComments.stream().map(comment -> {
            if (comment.startsWith("///")) {
                return comment.substring("///".length()).strip();
            }
            if (comment.startsWith("//")) {
                return comment.substring("//".length()).strip();
            }
            if (comment.startsWith("/**")) {
                return removeLeadingAsterixes(comment.substring("/**".length(), comment.length() - "*/".length()).strip());
            }
            if (comment.startsWith("/*")) {
                return removeLeadingAsterixes(comment.substring("/*".length(), comment.length() - "*/".length()).strip());
            }
            // a C comment must be a line comment (`//...`) or a block comment (`/*...*/`)
            throw new AssertionError("Invalid C comment");
        }).flatMap(String::lines).toList();
    }

    private static String removeLeadingAsterixes(String blockComment) {
        return blockComment.lines().map(String::stripLeading).map(line -> {
            if (line.startsWith("*")) {
                return line.substring("*".length()).strip();
            }
            return line.strip();
        }).collect(Collectors.joining("\n"));
    }

    /**
     * Convert raw comment text (as returned by {@link Cursor#getRawCommentText()}) to a
     * list of comments. The input string may consist of multiple line and block comments.
     * This function reconstructs the individual comments from the combined text. Note
     * that the comment delimiters are kept. This is because you need them to apply the
     * correct normalization logic (see {@link #normalizeComments}). The returned list
     * contains the individual comments as elements. This in particular means that
     * newlines in block comments are kept.
     */
    private static List<String> rawCommentTextToCommentList(String rawCommentText) {
        List<String> comments = new ArrayList<>();
        int cursor = 0;
        int length = rawCommentText.length();
        while (cursor < length) {
            while (cursor < length && Character.isWhitespace(rawCommentText.charAt(cursor))) {
                cursor++;
            }
            if (cursor == length) {
                break;
            }
            // comment delimiter is always 2 characters and starts with '/'
            if (cursor + 1 >= length || rawCommentText.charAt(cursor) != '/') {
                throw new AssertionError("Invalid C comment start");
            }
            char secondChar = rawCommentText.charAt(cursor + 1);

            switch (secondChar) {
                case '/' -> {
                    int end = cursor + "//".length();
                    while (end < length) {
                        char c = rawCommentText.charAt(end);
                        if (c == '\n' || c == '\r') {
                            break;
                        }
                        end++;
                    }
                    comments.add(rawCommentText.substring(cursor, end));
                    cursor = end;
                }
                case '*' -> {
                    int end = rawCommentText.indexOf("*/", cursor + "/*".length());
                    if (end == -1) {
                        throw new AssertionError("Unterminated C comment");
                    }
                    end += "*/".length();
                    comments.add(rawCommentText.substring(cursor, end));
                    cursor = end;
                }
                default -> throw new AssertionError("Invalid C comment start");
            }
        }
        return Collections.unmodifiableList(comments);
    }

    static List<String> extractMacroComments(Cursor c) {
        if (c.kind() != CursorKind.MacroDefinition) {
            throw new IllegalCallerException();
        }

        List<String> comments = new ArrayList<>();
        SourceLocation end = c.getExtent().getBegin();

        // ignore last tokens as they are part of the current declaration
        int skips = 3; // skip `#`, `define` and `<name>`

        // parse batches of `COPY_COMMENT_BATCH_SIZE` lines at a time
        // stop as soon as the token is no longer a comment token
        while (true) {
            SourceLocation.Location endLocation = end.getFileLocation();
            if (endLocation.line() == 1 && endLocation.column() == 1) {
                break;
            }
            int endLine = endLocation.line();
            int beginLine = Math.max(1, endLine - COPY_COMMENT_BATCH_SIZE);
            SourceLocation begin = c.getTranslationUnit().getLCLocationForLocation(end, beginLine, 1);
            boolean onlyComments = getPrecedingCommentTokens(c.getTranslationUnit(), begin, end, skips, comments);
            // skip overlap between begin/end
            skips = 1;
            if (!onlyComments) {
                break;
            }
            end = begin;
        }

        return normalizeComments(comments.reversed());
    }

    private static boolean getPrecedingCommentTokens(TranslationUnit tu, SourceLocation begin, SourceLocation end, int skips, List<String> outputComments) {
        try (TranslationUnit.Tokens tokens = tu.tokenizeRange(begin, end)) {
            int i = tokens.size() - 1 - skips;
            SourceLocation nextSourceLocation;
            if (i + 1 < tokens.size()) {
                nextSourceLocation = tokens.getToken(i + 1).getLocation();
            } else {
                nextSourceLocation = end;
            }

            for (; i >= 0; i--) {
                TranslationUnit.Tokens.Token token = tokens.getToken(i);
                if (token.kind() != Index_h.CXToken_Comment()) {
                    return false;
                }

                SourceLocation commentEnd = token.getExtent().getEnd();
                SourceLocation.Location commentEndLocation = commentEnd.getFileLocation();
                SourceLocation.Location nextLocation = nextSourceLocation.getFileLocation();
                int commentDistance = nextLocation.line() - commentEndLocation.line();
                // only associate comments that are separated by at most one newline
                if (commentDistance > 1) {
                    return false;
                }
                outputComments.add(token.spelling());
                nextSourceLocation = token.getLocation();
            }
        }
        return true;
    }

    private Declaration createTreeInternal(Cursor c) {
        Position pos = CursorPosition.of(c);
        if (pos == Position.NO_POSITION) return null; // intrinsic, skip
        // dedup multiple declarations that point to the same source location
        Cursor.Key key = c.toKey();
        Optional<Declaration> cachedDecl = lookup(key);
        if (cachedDecl.isPresent()) {
            return cachedDecl.get();
        }
        Declaration decl = switch (c.kind()) {
            case EnumDecl -> createEnum(c);
            case EnumConstantDecl -> createEnumConstant(c);
            case FieldDecl -> createVar(c, Declaration.Variable.Kind.FIELD);
            case ParmDecl -> createVar(c, Declaration.Variable.Kind.PARAMETER);
            case FunctionDecl -> createFunction(c);
            case StructDecl -> createRecord(c, Declaration.Scoped.Kind.STRUCT);
            case UnionDecl -> createRecord(c, Declaration.Scoped.Kind.UNION);
            case TypedefDecl -> createTypedef(c);
            case VarDecl -> createVar(c, Declaration.Variable.Kind.GLOBAL);
            default -> null; // skip
        };
        if (decl != null) {
            declarationCache.put(key, withDeclarationString(decl, c));
        }
        return decl;
    }

    static class CursorPosition implements Position {
        private final Cursor cursor;
        private final Path path;
        private final int line;
        private final int column;

        private CursorPosition(Cursor cursor) {
            this.cursor = cursor;
            SourceLocation.Location loc = cursor.getSourceLocation().getFileLocation();
            this.path = loc.path().toAbsolutePath();
            this.line = loc.line();
            this.column = loc.column();
        }

        static Position of(Cursor cursor) {
            SourceLocation loc = cursor.getSourceLocation();
            if (loc == null) {
                return NO_POSITION;
            }
            SourceLocation.Location sloc = loc.getFileLocation();
            if (sloc == null) {
                return NO_POSITION;
            }
            return new CursorPosition(cursor);
        }


        @Override
        public Path path() {
            return path;
        }

        @Override
        public int line() {
            return line;
        }

        @Override
        public int col() {
            return column;
        }

        public Cursor cursor() {
            return cursor;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (obj instanceof Position pos) {
                return Objects.equals(path, pos.path()) &&
                    Objects.equals(line, pos.line()) &&
                    Objects.equals(column, pos.col());
            }
            return false;
        }

        @Override
        public int hashCode() {
            return Objects.hash(path, line, column);
        }

        @Override
        public String toString() {
            return PrettyPrinter.position(this);
        }
    }

    public Declaration.Function createFunction(Cursor c) {
        checkCursor(c, CursorKind.FunctionDecl);
        List<Declaration.Variable> params = new ArrayList<>();
        for (int i = 0 ; i < c.numberOfArgs() ; i++) {
            params.add((Declaration.Variable)createTree(c.getArgument(i)));
        }
        Type type = toType(c);
        Type funcType = canonicalType(type);
        return withNestedTypes(Declaration.function(CursorPosition.of(c), c.spelling(), (Type.Function)funcType,
                params.toArray(new Declaration.Variable[0])), c, true);
    }

    public Declaration.Constant createMacro(Position pos, String name, Type type, Object value) {
        Declaration.Constant macro = Declaration.constant(pos, name, value, type);
        String valueString = value.toString();
        if (value instanceof String) {
            // quote string literal
            valueString = String.format("\"%1$s\"", valueString);
        } else if (Utils.isPointer(type)) {
            // add pointer cast to make it look different from a numeric constant
            valueString = String.format("(void*) %1$s", valueString);
        }
        DeclarationString.with(macro, String.format("#define %1$s %2$s", name, valueString));
        return macro;
    }

    public Declaration.Constant createEnumConstant(Cursor c) {
        return Declaration.constant(CursorPosition.of(c), c.spelling(), c.getEnumConstantValue(),
                toType(c));
    }

    public Declaration.Scoped createHeader(Cursor c, List<Declaration> decls) {
        return Declaration.toplevel(CursorPosition.of(c), filterHeaderDeclarations(decls).toArray(new Declaration[0]));
    }

    public Declaration.Scoped createRecord(Cursor c, Declaration.Scoped.Kind scopeKind) {
        checkCursorAny(c, CursorKind.StructDecl, CursorKind.UnionDecl);
        if (c.isDefinition()) {
            Type.Declared t = recordDeclaration(c, c);
            return t.tree();
        } else {
            //if there's a real definition somewhere else, skip this redundant declaration
            if (!c.getDefinition().isInvalid()) {
                return null;
            }
            return Declaration.scoped(scopeKind, CursorPosition.of(c), c.spelling());
        }
    }

    final Type.Declared recordDeclaration(Cursor parent, Cursor recordCursor) {
        List<Declaration> pendingFields = new ArrayList<>();
        List<Variable> pendingBitFields = new ArrayList<>();
        AtomicReference<Position> pendingBitfieldsPos = new AtomicReference<>();
        recordCursor.forEach(fc -> {
            if (Utils.isFlattenable(fc)) {
                if (fc.isBitField()) {
                    if (pendingBitfieldsPos.get() == null) {
                        pendingBitfieldsPos.set(CursorPosition.of(fc));
                    }
                    Type fieldType = toType(fc);
                    Variable bitfieldDecl = Declaration.bitfield(CursorPosition.of(fc), fc.spelling(), fc.getBitFieldWidth(), fieldType);
                    if (!fc.spelling().isEmpty()) {
                        ClangOffsetOf.with(bitfieldDecl, parent.type().getOffsetOf(fc.spelling()));
                    }
                    pendingBitFields.add(bitfieldDecl);
                } else {
                    if (!pendingBitFields.isEmpty()) {
                        pendingFields.add(Declaration.bitfields(pendingBitfieldsPos.get(), pendingBitFields.toArray(Variable[]::new)));
                        pendingBitFields.clear();
                        pendingBitfieldsPos.set(null);
                    }
                    if (fc.isAnonymousStruct()) {
                        // process struct recursively
                        pendingFields.add(recordDeclaration(parent, fc).tree());
                    } else {
                        Declaration fieldDecl = createTree(fc, copyComments);
                        ClangSizeOf.with(fieldDecl, fc.type().kind() == TypeKind.IncompleteArray ?
                                0 : fc.type().size() * 8);
                        ClangOffsetOf.with(fieldDecl, parent.type().getOffsetOf(fc.spelling()));
                        ClangAlignOf.with(fieldDecl, fc.type().align() * 8);
                        pendingFields.add(fieldDecl);
                    }
                }
            } else {
                // propagate
                createTree(fc);
            }
        });

        if (!pendingBitFields.isEmpty()) {
            pendingFields.add(Declaration.bitfields(pendingBitfieldsPos.get(), pendingBitFields.toArray(Variable[]::new)));
            pendingBitFields.clear();
            pendingBitfieldsPos.set(null);
        }

        Scoped structOrUnionDecl = recordCursor.kind() == CursorKind.StructDecl ?
                Declaration.struct(CursorPosition.of(recordCursor), recordCursor.spelling(),
                        pendingFields.toArray(new Declaration[0])) :
                Declaration.union(CursorPosition.of(recordCursor), recordCursor.spelling(),
                        pendingFields.toArray(new Declaration[0]));
        ClangSizeOf.with(structOrUnionDecl, recordCursor.type().size() * 8);
        ClangAlignOf.with(structOrUnionDecl, recordCursor.type().align() * 8);
        if (recordCursor.isAnonymousStruct()) {
            AnonymousStruct.with(structOrUnionDecl, offsetOfAnonymousRecord(parent, recordCursor, recordCursor));
        }

        return Type.declared(structOrUnionDecl);
    }

    /*
     * For the first named field that is nested somewhere inside anonRecord, we get the offset
     * to outermostParent and anonRecord itself. Subtracting the latter from the former
     * then gives us the offset of the anonRecord within outermostParent.
     *
     * Deals with cases like this too:
     *
     * struct Foo {
     *     char c; // offset = 0
     *     struct <anon1> { // offset = 96 - 64 = 32
     *         int: 32;
     *         struct <anon2> { // offset = 96 - 32 = 64
     *             int: 32;
     *             int x; // offset(Foo) = 96, offset(anon2) = 32, offset(anon1) = 64
     *         };
     *     };
     * };
     */
    private static OptionalLong offsetOfAnonymousRecord(Cursor outermostParent, Cursor anonRecord, Cursor record) {
        AtomicReference<OptionalLong> result = new AtomicReference<>(OptionalLong.empty());
        record.forEachShortCircuit(fc -> {
            if (Utils.isFlattenable(fc)) {
                if (!fc.spelling().isEmpty()) {
                    long offsetToOutermost = outermostParent.type().getOffsetOf(fc.spelling());
                    long offsetToAnon = anonRecord.type().getOffsetOf(fc.spelling());
                    result.set(OptionalLong.of(offsetToOutermost - offsetToAnon));
                    return false;
                } else if (fc.isAnonymousStruct()) {
                    OptionalLong nestedResult = offsetOfAnonymousRecord(outermostParent, anonRecord, fc);
                    if (nestedResult.isPresent()) {
                        result.set(nestedResult);
                        return false;
                    }
                    return true;
                }
            }
            return true;
        });
        return result.get();
    }

    public Declaration.Scoped createEnum(Cursor c) {
        if (c.isDefinition()) {
            List<Declaration> decls = new ArrayList<>();
            c.forEach(child -> {
                if (child.kind() == CursorKind.EnumConstantDecl) {
                    Declaration enumConstantDecl = createTree(child, copyComments);
                    DeclarationString.with(enumConstantDecl, enumConstantString(c.spelling(), (Declaration.Constant) enumConstantDecl));
                    decls.add(enumConstantDecl);
                }
            });
            Declaration.Scoped enumDecl = Declaration.enum_(CursorPosition.of(c), c.spelling(), decls.toArray(new Declaration[0]));
            DeclarationImpl.ClangEnumType.with(enumDecl, toType(c.getEnumDeclIntegerType()));
            return enumDecl;
        } else {
            //if there's a real definition somewhere else, skip this redundant declaration
            return null;
        }
    }

    private static boolean isRedundantTypedef(Declaration d) {
        return d instanceof Typedef typedef &&
                typedef.type() instanceof Declared declaredType &&
                declaredType.tree().name().equals(typedef.name());
    }

    /*
     * This method drops anonymous structs from the resulting toplevel declaration. These structs
     * can appear as part of a typedef, but are presented by libclang as toplevel structs, so we
     * need to filter them out.
     */
    private List<Declaration> filterHeaderDeclarations(List<Declaration> declarations) {
        return declarations.stream()
                .filter(Objects::nonNull)
                .filter(d -> Utils.isEnum(d) || (!d.name().isEmpty() && !isRedundantTypedef(d)))
                .collect(Collectors.toList());
    }

    private Declaration.Typedef createTypedef(Cursor c) {
        Type cursorType = toType(c);
        Type canonicalType = canonicalType(cursorType);
        Type.Function funcType = null;
        boolean isFuncPtrType = false;
        if (canonicalType instanceof Type.Function canonicalFunctionType) {
            funcType = canonicalFunctionType;
        } else if (Utils.isPointer(canonicalType)) {
            Type pointeeType = ((Type.Delegated)canonicalType).type();
            if (pointeeType instanceof Type.Function pointeeFunctionType) {
                funcType = pointeeFunctionType;
                isFuncPtrType = true;
            }
        }
        if (funcType != null) {
            List<String> params = new ArrayList<>();
            c.forEach(child -> {
                if (child.kind() == CursorKind.ParmDecl) {
                    params.add(createTree(child).name());
                }
            });
            if (!params.isEmpty()) {
                canonicalType = funcType.withParameterNames(params);
                if (isFuncPtrType) {
                   canonicalType = new TypeImpl.PointerImpl(canonicalType);
                }
            }
        }
        return withNestedTypes(Declaration.typedef(CursorPosition.of(c), c.spelling(), canonicalType), c, false);
    }

    private Type canonicalType(Type t) {
        if (t instanceof Type.Delegated delegated &&
           delegated.kind() == Type.Delegated.Kind.TYPEDEF) {
            return delegated.type();
        } else {
            return t;
        }
    }

    private Declaration.Variable createVar(Cursor c, Declaration.Variable.Kind kind) {
        if (c.isBitField()) throw new AssertionError("Cannot get here!");
        checkCursorAny(c, CursorKind.VarDecl, CursorKind.FieldDecl, CursorKind.ParmDecl);
        Type type = toType(c);
        return withNestedTypes(Declaration.var(kind, CursorPosition.of(c), c.spelling(), type), c, false);
    }

    /*
     * Some declarations (global vars, struct/union fields, function parameter/return types might contain
     * inline nested struct/union/enum definitions. This method collects such definitions and
     * attaches them to the original declaration, using an attribute.
     */
    private <D extends Declaration> D withNestedTypes(D d, Cursor c, boolean ignoreNestedParams) {
        List<Declaration> nestedDefinitions = new ArrayList<>();
        collectNestedTypes(c, nestedDefinitions, ignoreNestedParams);
        List<Scoped> nestedDecls = nestedDefinitions.stream()
                .filter(m -> m instanceof Scoped)
                .map(Scoped.class::cast)
                .toList();
        if (!nestedDecls.isEmpty()) {
            NestedDeclarations.with(d, nestedDecls);
        }
        return d;
    }

    private void collectNestedTypes(Cursor c, List<Declaration> nestedTypes, boolean ignoreNestedParams) {
        c.forEach(m -> {
            if (m.isDefinition()) {
                if (m.kind() == CursorKind.ParmDecl && !ignoreNestedParams) {
                    collectNestedTypes(m, nestedTypes, ignoreNestedParams);
                } else {
                    nestedTypes.add(createTree(m));
                }
            }
        });
    }

    Type toType(Cursor c) {
        return TypeMaker.makeType(c.type(), this);
    }

    Type toType(org.openjdk.jextract.clang.Type t) {
        return TypeMaker.makeType(t, this);
    }

    private void checkCursor(Cursor c, CursorKind k) {
        if (c.kind() != k) {
            throw new IllegalArgumentException("Invalid cursor kind");
        }
    }

    private void checkCursorAny(Cursor c, CursorKind... kinds) {
        CursorKind expected = Objects.requireNonNull(c.kind());
        for (CursorKind k : kinds) {
            if (k == expected) {
                return;
            }
        }
        throw new IllegalArgumentException("Invalid cursor kind");
    }

    private <D extends Declaration> D withDeclarationString(D decl, Cursor cursor) {
        String declString = switch (decl) {
            case Declaration.Constant _ -> null; // do nothing for enum constants
            case Typedef _ -> declarationString(cursor, true); // always expand typedefs
            default -> {
                // heuristic, try w/o expanding first, and check if there are <anonymous> strings
                String cursorString = declarationString(cursor, false);
                if (cursorString.matches(".*\\((unnamed|anonymous) (struct|union|enum) at.*")) {
                    // the output contains anonymous definitions, fallback and expand them
                    cursorString = declarationString(cursor, true);
                }
                yield cursorString;
            }
        };
        if (declString != null) {
            DeclarationString.with(decl, declString);
        }
        return decl;
    }

    private String declarationString(Cursor cursor, boolean expandNestedDecls) {
        PrintingPolicy pp = cursor.getPrintingPolicy();
        if (expandNestedDecls) {
            pp.setProperty(PrintingPolicyProperty.IncludeTagDefinition, true);
        }
        pp.setProperty(PrintingPolicyProperty.PolishForDeclaration, true);
        return cursor.prettyPrinted(pp);
    }

    private String enumConstantString(String enumName, Declaration.Constant enumConstant) {
        if (enumName.isEmpty()) {
            enumName = "<anonymous>";
        }
        return String.format("enum %1$s.%2$s = %3$s", enumName, enumConstant.name(), enumConstant.value());
    }
}
