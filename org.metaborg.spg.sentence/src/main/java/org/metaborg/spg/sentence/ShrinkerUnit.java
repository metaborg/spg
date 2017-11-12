package org.metaborg.spg.sentence;

import org.metaborg.spoofax.core.unit.ISpoofaxParseUnit;
import org.spoofax.interpreter.terms.IStrategoTerm;

public class ShrinkerUnit {
    private final IStrategoTerm term;
    private final String text;

    public ShrinkerUnit(IStrategoTerm term, String text) {
        this.term = term;
        this.text = text;
    }

    public ShrinkerUnit(ISpoofaxParseUnit parseUnit) {
        this.term = parseUnit.ast();
        this.text = parseUnit.input().text();
    }

    public IStrategoTerm getTerm() {
        return term;
    }

    public String getText() {
        return text;
    }
}
