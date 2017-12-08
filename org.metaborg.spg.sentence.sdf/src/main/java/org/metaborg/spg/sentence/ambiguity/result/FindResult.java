package org.metaborg.spg.sentence.ambiguity.result;

import org.metaborg.util.time.Timer;
import org.spoofax.interpreter.terms.IStrategoTerm;

public class FindResult {
    private final long duration;
    private final int terms;
    private final IStrategoTerm term;
    private final String text;

    public FindResult(Timer timer, int terms) {
        this(timer, terms, null, null);
    }

    public FindResult(Timer timer, int terms, IStrategoTerm term, String text) {
        this.duration = timer.stop();
        this.terms = terms;
        this.term = term;
        this.text = text;
    }

    public long duration() {
        return duration / 1000000;
    }

    public int terms() {
        return terms;
    }

    public IStrategoTerm term() {
        return term;
    }

    public String text() {
        return text;
    }

    public boolean found() {
        return term != null;
    }
}
