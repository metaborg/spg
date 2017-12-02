package org.metaborg.spg.sentence.shrinker;

import com.google.inject.Inject;
import org.metaborg.spg.sentence.random.IRandom;
import org.metaborg.spg.sentence.generator.Generator;
import org.metaborg.spg.sentence.signature.Signature;
import org.spoofax.interpreter.terms.ITermFactory;

public class ShrinkerFactory {
    private final IRandom random;
    private final ITermFactory termFactory;

    @Inject
    public ShrinkerFactory(IRandom random, ITermFactory termFactory) {
        this.random = random;
        this.termFactory = termFactory;
    }

    public Shrinker create(Generator generator, Signature signature) {
        return new Shrinker(random, termFactory, generator, signature);
    }
}
