package org.metaborg.spg.sentence.generator;

import org.metaborg.sdf2table.grammar.ISymbol;
import org.spoofax.interpreter.terms.IStrategoTerm;
import org.spoofax.interpreter.terms.ITermFactory;
import org.spoofax.terms.attachments.AbstractTermAttachment;
import org.spoofax.terms.attachments.TermAttachmentType;

public class GeneratorAttachment extends AbstractTermAttachment {
    public static final TermAttachmentType<GeneratorAttachment> TYPE = new TermAttachmentType<GeneratorAttachment>(GeneratorAttachment.class, "GeneratorAttachment", 1) {
        @Override
        protected IStrategoTerm[] toSubterms(ITermFactory factory, GeneratorAttachment attachment) {
            return new IStrategoTerm[0];
        }

        @Override
        protected GeneratorAttachment fromSubterms(IStrategoTerm[] subterms) {
            return null;
        }
    };

    private final ISymbol symbol;

    public GeneratorAttachment(ISymbol symbol) {
        this.symbol = symbol;
    }

    public ISymbol getSymbol() {
        return symbol;
    }

    @Override
    public TermAttachmentType<?> getAttachmentType() {
        return TYPE;
    }
}
