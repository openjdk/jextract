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

import java.lang.constant.Constable;
import java.nio.ByteOrder;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import jdk.incubator.foreign.MemoryLayout;
import org.openjdk.jextract.Declaration;
import org.openjdk.jextract.Position;
import org.openjdk.jextract.Type;
import org.openjdk.jextract.clang.Cursor;
import org.openjdk.jextract.clang.CursorKind;
import org.openjdk.jextract.clang.CursorLanguage;
import org.openjdk.jextract.clang.SourceLocation;

class TreeMaker {
    public TreeMaker() {}

    TypeMaker typeMaker = new TypeMaker(this);

    public void freeze() {
        typeMaker.resolveTypeReferences();
    }

    interface ScopedFactoryLayout {
        Declaration.Scoped make(Position pos, String name, MemoryLayout layout, Declaration... decls);
    }

    interface ScopedFactoryNoLayout {
        Declaration.Scoped make(Position pos, String name, Declaration... decls);
    }

    interface VarFactoryNoLayout {
        Declaration.Variable make(Position pos, String name, Type type);
    }

    Map<String, List<Constable>> collectAttributes(Cursor c) {
        return c.children().filter(Cursor::isAttribute)
                .collect(Collectors.groupingBy(
                        attr -> attr.kind().name(),
                        Collectors.mapping(Cursor::spelling, Collectors.toList())
                ));
    }

    public Declaration createTree(Cursor c) {
        Objects.requireNonNull(c);
        CursorLanguage lang = c.language();
        if (lang != CursorLanguage.C && lang != CursorLanguage.Invalid) {
            throw new RuntimeException("Unsupported language: " + c.language());
        }
        var rv = (DeclarationImpl) createTreeInternal(c);
        return (rv == null) ? null : rv.withAttributes(collectAttributes(c));
    }

    private Declaration createTreeInternal(Cursor c) {
        switch (c.kind()) {
            case EnumDecl:
                return createEnum(c, Declaration::enum_, Declaration::enum_);
            case EnumConstantDecl:
                return createEnumConstant(c);
            case FieldDecl:
                return createVar(c.isBitField() ?
                        Declaration.Variable.Kind.BITFIELD : Declaration.Variable.Kind.FIELD, c, Declaration::field);
            case ParmDecl:
                return createVar(Declaration.Variable.Kind.PARAMETER, c, Declaration::parameter);
            case FunctionDecl:
                return createFunction(c);
            case StructDecl:
                return createRecord(c, Declaration.Scoped.Kind.STRUCT, Declaration::struct, Declaration::struct);
            case UnionDecl:
                return createRecord(c, Declaration.Scoped.Kind.UNION, Declaration::union, Declaration::union);
            case TypedefDecl: {
                return createTypedef(c);
            }
            case VarDecl:
                return createVar(Declaration.Variable.Kind.GLOBAL, c, Declaration::globalVariable);
            default:
                return null;
        }
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

    public Declaration.Constant createMacro(Cursor c, String name, Type type, Object value) {
        checkCursorAny(c, CursorKind.MacroDefinition);
        return Declaration.constant(CursorPosition.of(c), name, value, type);
    }

    public Declaration.Constant createEnumConstant(Cursor c) {
        return Declaration.constant(CursorPosition.of(c), c.spelling(), c.getEnumConstantValue(), typeMaker.makeType(c.type()));
    }

    public Declaration.Scoped createHeader(Cursor c, List<Declaration> decls) {
        return Declaration.toplevel(CursorPosition.of(c), filterNestedDeclarations(decls).toArray(new Declaration[0]));
    }

    public Declaration.Scoped createRecord(Cursor c, Declaration.Scoped.Kind scopeKind, ScopedFactoryLayout factoryLayout, ScopedFactoryNoLayout factoryNoLayout) {
        Type.Declared t = (Type.Declared)RecordLayoutComputer.compute(typeMaker, 0, c.type(), c.type());
        List<Declaration> decls = filterNestedDeclarations(t.tree().members());
        if (c.isDefinition()) {
            //just a declaration AND definition, we have a layout
            return factoryLayout.make(CursorPosition.of(c), c.spelling(), t.tree().layout().get(), decls.toArray(new Declaration[0]));
        } else {
            //just a declaration
            if (scopeKind == Declaration.Scoped.Kind.STRUCT ||
                    scopeKind == Declaration.Scoped.Kind.UNION ||
                    scopeKind == Declaration.Scoped.Kind.CLASS) {
                //if there's a real definition somewhere else, skip this redundant declaration
                if (!c.getDefinition().isInvalid()) {
                    return null;
                }
            }
            return factoryNoLayout.make(CursorPosition.of(c), c.spelling(), decls.toArray(new Declaration[0]));
        }
    }

    public Declaration.Scoped createEnum(Cursor c, ScopedFactoryLayout factoryLayout, ScopedFactoryNoLayout factoryNoLayout) {
        List<Declaration> decls = filterNestedDeclarations(c.children()
                .filter(fc -> {
                    if (fc.isBitField()) {
                        // only non-empty and named bit fields are generated
                        return fc.getBitFieldWidth() != 0 && !fc.spelling().isEmpty();
                    }
                    return true;
                })
                .map(this::createTree).collect(Collectors.toList()));
        if (c.isDefinition()) {
            //just a declaration AND definition, we have a layout
            MemoryLayout layout = TypeMaker.valueLayoutForSize(c.type().size() * 8).layout().orElseThrow();
            return factoryLayout.make(CursorPosition.of(c), c.spelling(), layout, decls.toArray(new Declaration[0]));
        } else {
            //just a declaration
            //if there's a real definition somewhere else, skip this redundant declaration
            if (!c.getDefinition().isInvalid()) {
                return null;
            }
            return factoryNoLayout.make(CursorPosition.of(c), c.spelling(), decls.toArray(new Declaration[0]));
        }
    }

    private static boolean isEnum(Declaration d) {
        return d instanceof Declaration.Scoped && ((Declaration.Scoped)d).kind() == Declaration.Scoped.Kind.ENUM;
    }

    private static boolean isBitfield(Declaration d) {
        return d instanceof Declaration.Scoped && ((Declaration.Scoped)d).kind() == Declaration.Scoped.Kind.BITFIELDS;
    }

    private static boolean isAnonymousStruct(Declaration declaration) {
        return ((CursorPosition)declaration.pos()).cursor.isAnonymousStruct();
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
        if (canonicalType instanceof Type.Declared) {
            Declaration.Scoped s = ((Type.Declared) canonicalType).tree();
            if (s.name().equals(c.spelling())) {
                // typedef record with the same name, no need to present twice
                return null;
            }
        }
        Type.Function funcType = null;
        boolean isFuncPtrType = false;
        if (canonicalType instanceof Type.Function) {
            funcType = (Type.Function)canonicalType;
        } else if (Utils.isPointerType(canonicalType)) {
            Type pointeeType = null;
            try {
                pointeeType = ((Type.Delegated)canonicalType).type();
            } catch (NullPointerException npe) {
                // exception thrown for unresolved pointee type. Ignore if we hit that case.
            }
            if (pointeeType instanceof Type.Function) {
                funcType = (Type.Function)pointeeType;
                isFuncPtrType = true;
            }
        }
        if (funcType != null) {
            List<String> params = c.children().
                filter(ch -> ch.kind() == CursorKind.ParmDecl).
                map(this::createTree).
                map(Declaration.Variable.class::cast).
                map(Declaration::name).
                collect(Collectors.toList());
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

    private Declaration.Variable createVar(Declaration.Variable.Kind kind, Cursor c, VarFactoryNoLayout varFactory) {
        checkCursorAny(c, CursorKind.VarDecl, CursorKind.FieldDecl, CursorKind.ParmDecl);
        if (c.isBitField()) {
            return Declaration.bitfield(CursorPosition.of(c), c.spelling(), toType(c),
                    MemoryLayout.paddingLayout(c.getBitFieldWidth()));
        } else {
            Type type = null;
            try {
                type = toType(c);
            } catch (TypeMaker.TypeException ex) {
                System.err.println(ex);
                System.err.println("WARNING: ignoring variable: " + c.spelling());
                return null;
            }
            return varFactory.make(CursorPosition.of(c), c.spelling(), type);
        }
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
