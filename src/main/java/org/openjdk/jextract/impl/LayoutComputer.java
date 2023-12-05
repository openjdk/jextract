package org.openjdk.jextract.impl;

import org.openjdk.jextract.Declaration;
import org.openjdk.jextract.Declaration.Constant;
import org.openjdk.jextract.Declaration.Function;
import org.openjdk.jextract.Declaration.Scoped;
import org.openjdk.jextract.Declaration.Scoped.Kind;
import org.openjdk.jextract.Declaration.Typedef;
import org.openjdk.jextract.Declaration.Variable;
import org.openjdk.jextract.Type;
import org.openjdk.jextract.Type.Declared;
import org.openjdk.jextract.impl.DeclarationImpl.ClangAlignOf;
import org.openjdk.jextract.impl.DeclarationImpl.ClangOffsetOf;
import org.openjdk.jextract.impl.DeclarationImpl.ClangSizeOf;
import org.openjdk.jextract.impl.DeclarationImpl.ScopedLayout;

import java.lang.foreign.GroupLayout;
import java.lang.foreign.MemoryLayout;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class LayoutComputer implements Declaration.Visitor<Void, Void> {

    public Declaration.Scoped scan(Declaration.Scoped header) {
        header.members().forEach(fieldTree -> fieldTree.accept(this, null));
        return header;
    }

    @Override
    public Void visitScoped(Scoped scoped, Void unused) {
        switch (scoped.kind()) {
            case Kind.STRUCT, Kind.UNION -> {
                if (ClangSizeOf.get(scoped).isPresent()) {
                    ScopedLayout.with(scoped, recordLayout(0, scoped));
                }
            }
            case Kind.ENUM ->
                ScopedLayout.with(scoped, Type.layoutFor(((Constant)scoped.members().get(0)).type()).get());
        }
        return null;
    }

    @Override
    public Void visitFunction(Function d, Void unused) {
        return null;
    }

    @Override
    public Void visitVariable(Variable d, Void unused) {
        return null;
    }

    @Override
    public Void visitConstant(Constant d, Void unused) {
        return null;
    }

    @Override
    public Void visitTypedef(Typedef d, Void unused) {
        return null;
    }

    @Override
    public Void visitDeclaration(Declaration d, Void unused) {
        return null;
    }

    GroupLayout recordLayout(long base, Scoped scoped) {
        boolean isStruct = scoped.kind() == Kind.STRUCT;

        long offset = 0L;
        long size = 0L;
        List<MemoryLayout> memberLayouts = new ArrayList<>();
        for (Declaration member : scoped.members()) {
            long align = ClangAlignOf.getOrThrow(member);
            long alignedOffset = (offset + align - 1) & ~(align - 1);
            long delta = alignedOffset - offset;
            if (delta > 0) {
                memberLayouts.add(MemoryLayout.paddingLayout(delta));
                offset += delta;
                if (isStruct) {
                    size += delta;
                }
            }
            boolean added = false;
            if (member instanceof Scoped nested) {
                // nested anonymous struct or union, recurse
                memberLayouts.add(recordLayout(base + offset, nested));
                added = true;
            } else {
                Variable field = (Variable) member;
                if (field.type() instanceof Declared declaredType) {
                    // propagate
                    declaredType.tree().accept(this, null);
                }
                if (base + offset != ClangOffsetOf.getOrThrow(field)) {
                    throw new AssertionError(String.format("Bad offset for field %s - expected %d, found %d",
                            field.name(), base + offset, ClangOffsetOf.getOrThrow(field)));
                }
                Optional<MemoryLayout> fieldLayout = Type.layoutFor(field.type());
                if (fieldLayout.isPresent()) {
                    memberLayouts.add(fieldLayout.get().withName(field.name()));
                    added = true;
                }
            }
            if (added) {
                // update offset and size
                long fieldSize = ClangSizeOf.getOrThrow(member);
                if (isStruct) {
                    offset += fieldSize;
                    size += fieldSize;
                } else {
                    size = Math.max(size, ClangSizeOf.getOrThrow(member));
                }
            }
        }
        long expectedSize = ClangSizeOf.getOrThrow(scoped);
        if (size != expectedSize) {
            memberLayouts.add(MemoryLayout.paddingLayout((expectedSize - size)));
        }
        GroupLayout layout = isStruct ?
                MemoryLayout.structLayout(memberLayouts.toArray(MemoryLayout[]::new)) :
                MemoryLayout.unionLayout(memberLayouts.toArray(MemoryLayout[]::new));
        if (!scoped.name().isEmpty()) {
            layout = layout.withName(scoped.name());
        }
        return layout;
    }
}
