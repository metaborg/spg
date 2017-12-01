package org.metaborg.spg.sentence.ambiguity.result;

import org.metaborg.util.time.Timer;
import org.spoofax.interpreter.terms.IStrategoTerm;

public class ShrinkResult {
    private final long duration;
    private final IStrategoTerm term;
    private final String text;

    public ShrinkResult(Timer timer) {
        this(timer, null, null);
    }

    public ShrinkResult(Timer timer, IStrategoTerm term, String text) {
        this.duration = timer.stop();
        this.term = term;
        this.text = text;
    }

    public long duration() {
        return duration / 1000000;
    }

    public IStrategoTerm term() {
        return term;
    }

    public String text() {
        return text;
    }

    public boolean success() {
        return term != null;
    }
}
