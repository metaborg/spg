package org.metaborg.spg.sentence.evaluation;

import org.metaborg.spg.sentence.IRandom;
import org.metaborg.spg.sentence.Random;
import org.metaborg.spg.sentence.guice.SentenceModule;

public class EvaluationSentenceModule extends SentenceModule {
    private final long seed;

    public EvaluationSentenceModule(long seed) {
        this.seed = seed;
    }

    @Override
    public void bindRandom() {
        bind(IRandom.class)
                .toInstance(new Random(seed));
    }
}
