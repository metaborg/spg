package org.metaborg.spg.sentence.guice;

import com.google.inject.AbstractModule;
import com.google.inject.assistedinject.FactoryModuleBuilder;
import org.metaborg.spg.sentence.IRandom;
import org.metaborg.spg.sentence.Random;
import org.metaborg.spg.sentence.ambiguity.AmbiguityTesterFactory;
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
