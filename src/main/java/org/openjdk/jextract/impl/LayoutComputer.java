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

import java.lang.foreign.AddressLayout;
import java.lang.foreign.GroupLayout;
import java.lang.foreign.MemoryLayout;
import java.lang.foreign.SequenceLayout;
import java.lang.foreign.StructLayout;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.concurrent.atomic.AtomicInteger;

public class LayoutComputer implements Declaration.Visitor<Void, Void> {

    public Declaration.Scoped scan(Declaration.Scoped header) {
        header.members().forEach(fieldTree -> fieldTree.accept(this, null));
        return header;
    }

    @Override
    public Void visitScoped(Scoped scoped, Void unused) {
        addScopedLayout(scoped);
        return null;
    }

    @Override
    public Void visitFunction(Function d, Void unused) {
        processType(d.type().returnType());
        d.type().argumentTypes().forEach(this::processType);
        return null;
    }

    @Override
    public Void visitVariable(Variable d, Void unused) {
        processType(d.type());
        return null;
    }

    @Override
    public Void visitConstant(Constant d, Void unused) {
        return null;
    }

    @Override
    public Void visitTypedef(Typedef d, Void unused) {
        processType(d.type());
        return null;
    }

    @Override
    public Void visitDeclaration(Declaration d, Void unused) {
        return null;
    }

    void processType(Type type) {
        if (type instanceof Declared declared) {
            addScopedLayout(declared.tree());
        }
    }

    void addScopedLayout(Scoped scoped) {
        switch (scoped.kind()) {
            case Kind.STRUCT, Kind.UNION -> {
                if (ClangSizeOf.get(scoped).isPresent()) {
                    ScopedLayout.with(scoped, recordLayout(scoped));
                }
            }
            case Kind.ENUM ->
                    ScopedLayout.with(scoped, Type.layoutFor(((Constant)scoped.members().get(0)).type()).get());
        }
    }

    GroupLayout recordLayout(Scoped scoped) {
        return recordLayout(0, new AtomicInteger(), scoped);
    }

    GroupLayout recordLayout(long base, AtomicInteger anonCount, Scoped scoped) {
        boolean isStruct = scoped.kind() == Kind.STRUCT;
        String name = scoped.name().isEmpty() ?
                "$anon$" + anonCount.getAndIncrement() :
                scoped.name();

        long offset = base; // bits
        long size = 0L; // bits
        List<MemoryLayout> memberLayouts = new ArrayList<>();
        for (Declaration member : scoped.members()) {
            if (member instanceof Scoped nested && nested.kind() == Kind.BITFIELDS) {
                // skip
            } else if (nextOffset(member).isPresent()) {
                long nextOffset = nextOffset(member).getAsLong();
                long delta = nextOffset - offset;
                if (delta > 0) {
                    memberLayouts.add(MemoryLayout.paddingLayout(delta / 8));
                    offset += delta;
                    if (isStruct) {
                        size += delta;
                    }
                }
                boolean added = false;
                if (member instanceof Scoped nested) {
                    // nested anonymous struct or union, recurse
                    GroupLayout layout = recordLayout(base + offset, anonCount, nested);
                    ScopedLayout.with(nested, layout);
                    memberLayouts.add(layout);
                    added = true;
                } else {
                    Variable field = (Variable) member;
                    processType(field.type());
                    Optional<MemoryLayout> fieldLayout = Type.layoutFor(field.type());
                    if (fieldLayout.isPresent()) {
                        memberLayouts.add(fieldLayout.get()
                                .withName(field.name())
                                .withByteAlignment(ClangAlignOf.getOrThrow(field) / 8));
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
        }
        long expectedSize = ClangSizeOf.getOrThrow(scoped);
        if (size != expectedSize) {
            memberLayouts.add(MemoryLayout.paddingLayout((expectedSize - size) / 8));
        }
        long align = ClangAlignOf.getOrThrow(scoped) / 8;
        GroupLayout layout = isStruct ?
                MemoryLayout.structLayout(alignFields(memberLayouts, align)) :
                MemoryLayout.unionLayout(alignFields(memberLayouts, align));
        return layout.withName(name);
    }

    OptionalLong nextOffset(Declaration member) {
        if (member instanceof Variable) {
            return ClangOffsetOf.get(member);
        } else {
            Optional<Declaration> firstDecl = ((Scoped)member).members().stream().findFirst();
            return firstDecl.isEmpty() ?
                    OptionalLong.empty() :
                    nextOffset(firstDecl.get());
        }
    }

    MemoryLayout[] alignFields(List<MemoryLayout> members, long align) {
        return members.stream()
                .map(l -> forceAlign(l, align))
                .toArray(MemoryLayout[]::new);
    }

    MemoryLayout forceAlign(MemoryLayout layout, long align) {
        if (align >= layout.byteAlignment()) {
            return layout; // fast-path
        }
        MemoryLayout res = switch (layout) {
            case GroupLayout groupLayout -> {
                MemoryLayout[] newMembers = groupLayout.memberLayouts()
                        .stream().map(l -> forceAlign(l, align)).toArray(MemoryLayout[]::new);
                yield groupLayout instanceof StructLayout ?
                        MemoryLayout.structLayout(newMembers) :
                        MemoryLayout.unionLayout(newMembers);
            }
            case SequenceLayout sequenceLayout ->
                    MemoryLayout.sequenceLayout(sequenceLayout.elementCount(),
                            forceAlign(sequenceLayout.elementLayout(), align));
            default -> layout.withByteAlignment(align);
        };
        // copy name and target layout, if present
        if (layout.name().isPresent()) {
            res = res.withName(layout.name().get());
        }
        if (layout instanceof AddressLayout addressLayout && addressLayout.targetLayout().isPresent()) {
            ((AddressLayout)res).withTargetLayout(addressLayout.targetLayout().get());
        }
        return res;
    }
}
