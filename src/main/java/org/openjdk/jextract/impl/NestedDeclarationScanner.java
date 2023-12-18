package org.openjdk.jextract.impl;

import org.openjdk.jextract.Declaration;
import org.openjdk.jextract.Declaration.Function;
import org.openjdk.jextract.Declaration.Scoped;
import org.openjdk.jextract.Declaration.Typedef;
import org.openjdk.jextract.Declaration.Variable;

public abstract class NestedDeclarationScanner<P> implements Declaration.Visitor<Void, P> {
    @Override
    public Void visitScoped(Scoped d, P p) {
        d.members().forEach(m -> m.accept(this, p));
        return null;
    }

    @Override
    public Void visitFunction(Function d, P p) {
        d.parameters().forEach(par -> par.accept(this, p));
        Utils.nestedDeclarationFor(d.type().returnType())
                .ifPresent(r -> r.accept(this, p));
        return null;
    }

    @Override
    public Void visitVariable(Variable d, P p) {
        Utils.nestedDeclarationFor(d.type())
                .ifPresent(r -> r.accept(this, p));
        return null;
    }

    @Override
    public Void visitTypedef(Typedef d, P p) {
        Utils.nestedDeclarationFor(d.type())
                .ifPresent(r -> r.accept(this, p));
        return null;
    }

    @Override
    public Void visitDeclaration(Declaration d, P p) {
        return null;
    }
}
