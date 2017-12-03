package org.metaborg.spg.sentence.guice;

import com.google.inject.AbstractModule;
import org.metaborg.spg.sentence.random.IRandom;
import org.metaborg.spg.sentence.random.Random;
import org.metaborg.spg.sentence.terms.GeneratorTermFactory;
import org.spoofax.interpreter.terms.ITermFactory;
import org.spoofax.jsglr.client.imploder.ImploderOriginTermFactory;
import org.spoofax.terms.TermFactory;

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
        ITermFactory termFactory = new TermFactory();
        ImploderOriginTermFactory imploderOriginTermFactory = new ImploderOriginTermFactory(termFactory);
        GeneratorTermFactory generatorTermFactory = new GeneratorTermFactory(imploderOriginTermFactory);

        bind(ITermFactory.class).toInstance(generatorTermFactory);
        bind(GeneratorTermFactory.class).toInstance(generatorTermFactory);

        bindRandom();
    }

    protected void bindRandom() {
        bind(IRandom.class).toInstance(random);
    }
}
