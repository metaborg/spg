package org.metaborg.spg.sentence.guice;

import com.google.inject.AbstractModule;
import org.metaborg.spg.sentence.random.IRandom;
import org.metaborg.spg.sentence.random.Random;
import org.spoofax.interpreter.terms.ITermFactory;

public class SentenceModule extends AbstractModule {
    private Random random;

    public SentenceModule() {
        this.random = new Random();
    }

    public SentenceModule(long seed) {
        this.random = new Random(seed);
    }

    @Override
    protected void configure() {
        bind(ITermFactory.class).toProvider(TermFactoryProvider.class);

        bindRandom();
    }

    protected void bindRandom() {
        bind(IRandom.class).toInstance(random);
    }
}
