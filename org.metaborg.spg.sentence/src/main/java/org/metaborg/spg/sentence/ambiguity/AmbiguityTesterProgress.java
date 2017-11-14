package org.metaborg.spg.sentence.ambiguity;

public interface AmbiguityTesterProgress {
    void sentenceGenerated(String text);

    void sentenceShrinked(String text);
}
