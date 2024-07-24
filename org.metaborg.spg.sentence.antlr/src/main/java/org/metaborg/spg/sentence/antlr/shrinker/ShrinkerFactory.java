package org.metaborg.spg.sentence.antlr.shrinker;

import com.google.inject.Inject;
import org.metaborg.spg.sentence.antlr.generator.Generator;
import org.metaborg.spg.sentence.antlr.grammar.Grammar;

import java.util.Random;

public class ShrinkerFactory {
    private final Random random;

    @jakarta.inject.Inject
    public ShrinkerFactory(Random random) {
        this.random = random;
    }

    public Shrinker create(Generator generator, Grammar grammar) {
        return new Shrinker(random, generator, grammar);
    }
}
