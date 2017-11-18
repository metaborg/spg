package org.metaborg.spg.sentence.evaluation;

import org.metaborg.spg.sentence.ambiguity.AmbiguityTesterProgress;

public class AmbiguityTesterProgressNoop implements AmbiguityTesterProgress {
    @Override
    public void sentenceGenerated(String text) {
    }

    @Override
    public void sentenceShrinked(String text) {
    }
}
