package org.metaborg.spg.sentence.ambiguity;

public abstract class AmbiguityTesterProgress {
    public abstract void sentenceGenerated(String text);

    public abstract void sentenceShrinked(String text);
}
