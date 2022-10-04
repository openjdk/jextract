/*
 *  Copyright (c) 2020, 2022, Oracle and/or its affiliates. All rights reserved.
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

import java.lang.constant.Constable;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import java.lang.foreign.MemoryLayout;
import org.openjdk.jextract.Declaration;
import org.openjdk.jextract.Position;
import org.openjdk.jextract.Type;
import org.openjdk.jextract.clang.Cursor;
import org.openjdk.jextract.clang.CursorKind;
import org.openjdk.jextract.clang.CursorLanguage;
import org.openjdk.jextract.clang.LinkageKind;
import org.openjdk.jextract.clang.SourceLocation;

class TreeMaker {
    public TreeMaker() {}

    TypeMaker typeMaker = new TypeMaker(this);

    public void freeze() {
        typeMaker.resolveTypeReferences();
    }

    Map<String, List<Constable>> collectAttributes(Cursor c) {
        Map<String, List<Constable>> attributeMap = new HashMap<>();
        c.forEach(child -> {
            if (child.isAttribute()) {
                List<Constable> attrs = attributeMap.computeIfAbsent(child.kind().name(), _unused -> new ArrayList<>());
                attrs.add(child.spelling());
            }
        });
        return attributeMap;
    }

    public Declaration createTree(Cursor c) {
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
        return (rv == null) ? null : rv.withAttributes(collectAttributes(c));
    }

    private Declaration createTreeInternal(Cursor c) {
        return switch (c.kind()) {
            case EnumDecl -> createEnum(c);
            case EnumConstantDecl -> createEnumConstant(c);
            case FieldDecl -> c.isBitField() ?
                        createBitfield(c) :
                        createVar(c, Declaration.Variable.Kind.FIELD);
            case ParmDecl -> createVar(c, Declaration.Variable.Kind.PARAMETER);
            case FunctionDecl -> createFunction(c);
            case StructDecl -> createRecord(c, Declaration.Scoped.Kind.STRUCT);
            case UnionDecl -> createRecord(c, Declaration.Scoped.Kind.UNION);
            case TypedefDecl -> createTypedef(c);
            case VarDecl -> createVar(c, Declaration.Variable.Kind.GLOBAL);
            default -> null; // skip
        };
    }

    static class CursorPosition implements Position {
        private final Cursor cursor;
        private final Path path;
        private final int line;
        private final int column;

        private CursorPosition(Cursor cursor) {
            this.cursor = cursor;
            SourceLocation.Location loc = cursor.getSourceLocation().getFileLocation();
            this.path = loc.path();
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
        return Declaration.function(CursorPosition.of(c), c.spelling(), (Type.Function)funcType,
                params.toArray(new Declaration.Variable[0]));
    }

    public Declaration.Constant createMacro(Position pos, String name, Type type, Object value) {
        return Declaration.constant(pos, name, value, type);
    }

    public Declaration.Constant createEnumConstant(Cursor c) {
        return Declaration.constant(CursorPosition.of(c), c.spelling(), c.getEnumConstantValue(), typeMaker.makeType(c.type()));
    }

    public Declaration.Scoped createHeader(Cursor c, List<Declaration> decls) {
        return Declaration.toplevel(CursorPosition.of(c), filterNestedDeclarations(decls).toArray(new Declaration[0]));
    }

    public Declaration.Scoped createRecord(Cursor c, Declaration.Scoped.Kind scopeKind) {
        Type.Declared t = (Type.Declared)RecordLayoutComputer.compute(typeMaker, 0, c.type(), c.type());
        List<Declaration> decls = filterNestedDeclarations(t.tree().members());
        if (c.isDefinition()) {
            //just a declaration AND definition, we have a layout
            return Declaration.scoped(scopeKind, CursorPosition.of(c), c.spelling(),
                                      t.tree().layout().get(), decls.toArray(new Declaration[0]));
        } else {
            //if there's a real definition somewhere else, skip this redundant declaration
            if (!c.getDefinition().isInvalid()) {
                return null;
            }
            return Declaration.scoped(scopeKind, CursorPosition.of(c), c.spelling(), decls.toArray(new Declaration[0]));
        }
    }

    public Declaration.Scoped createEnum(Cursor c) {
        List<Declaration> allDecls = new ArrayList<>();
        c.forEach(child -> {
            if (!child.isBitField() || (child.getBitFieldWidth() != 0 && !child.spelling().isEmpty())) {
                allDecls.add(createTree(child));
            }
        });
        List<Declaration> decls = filterNestedDeclarations(allDecls);
        if (c.isDefinition()) {
            //just a declaration AND definition, we have a layout
            MemoryLayout layout = TypeMaker.valueLayoutForSize(c.type().size() * 8).layout().orElseThrow();
            return Declaration.enum_(CursorPosition.of(c), c.spelling(), layout, decls.toArray(new Declaration[0]));
        } else {
            //just a declaration
            //if there's a real definition somewhere else, skip this redundant declaration
            if (!c.getDefinition().isInvalid()) {
                return null;
            }
            return Declaration.enum_(CursorPosition.of(c), c.spelling(), decls.toArray(new Declaration[0]));
        }
    }

    private static boolean isEnum(Declaration d) {
        return d instanceof Declaration.Scoped scoped &&
                scoped.kind() == Declaration.Scoped.Kind.ENUM;
    }

    private static boolean isBitfield(Declaration d) {
        return d instanceof Declaration.Scoped scoped &&
                scoped.kind() == Declaration.Scoped.Kind.BITFIELDS;
    }

    private static boolean isAnonymousStruct(Declaration declaration) {
        return declaration.getAttribute("ANONYMOUS").isPresent();
    }

    private List<Declaration> filterNestedDeclarations(List<Declaration> declarations) {
        return declarations.stream()
                .filter(Objects::nonNull)
                .filter(d -> isEnum(d) || !d.name().isEmpty() || isAnonymousStruct(d) || isBitfield(d))
                .collect(Collectors.toList());
    }

    private Declaration.Typedef createTypedef(Cursor c) {
        Type cursorType = toType(c);
        Type canonicalType = canonicalType(cursorType);
        if (canonicalType instanceof Type.Declared declaredCanonicalType) {
            Declaration.Scoped s = declaredCanonicalType.tree();
            if (s.name().equals(c.spelling())) {
                // typedef record with the same name, no need to present twice
                return null;
            }
        }
        Type.Function funcType = null;
        boolean isFuncPtrType = false;
        if (canonicalType instanceof Type.Function canonicalFunctionType) {
            funcType = canonicalFunctionType;
        } else if (Utils.isPointerType(canonicalType)) {
            Type pointeeType = null;
            try {
                pointeeType = ((Type.Delegated)canonicalType).type();
            } catch (NullPointerException npe) {
                // exception thrown for unresolved pointee type. Ignore if we hit that case.
            }
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
        return Declaration.typedef(CursorPosition.of(c), c.spelling(), canonicalType);
    }

    private Type canonicalType(Type t) {
        if (t instanceof Type.Delegated delegated &&
           delegated.kind() == Type.Delegated.Kind.TYPEDEF) {
            return delegated.type();
        } else {
            return t;
        }
    }

    private Declaration.Variable createBitfield(Cursor c) {
        checkCursorAny(c, CursorKind.FieldDecl);
        return Declaration.bitfield(CursorPosition.of(c), c.spelling(), toType(c),
                MemoryLayout.paddingLayout(c.getBitFieldWidth()));
    }

    private Declaration.Variable createVar(Cursor c, Declaration.Variable.Kind kind) {
        checkCursorAny(c, CursorKind.VarDecl, CursorKind.FieldDecl, CursorKind.ParmDecl);
        Type type;
        try {
            type = toType(c);
        } catch (TypeMaker.TypeException ex) {
            System.err.println(ex);
            System.err.println("WARNING: ignoring variable: " + c.spelling());
            return null;
        }
        return Declaration.var(kind, CursorPosition.of(c), c.spelling(), type);
    }

    private Type toType(Cursor c) {
        return typeMaker.makeType(c.type());
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
}
