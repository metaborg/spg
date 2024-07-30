package org.metaborg.spg.sentence.antlr.generator;

import com.google.inject.Inject;
import org.metaborg.spg.sentence.antlr.grammar.Grammar;

import java.util.Random;

public class GeneratorFactory {
    private final Random random;

    @jakarta.inject.Inject
    public GeneratorFactory(Random random) {
        this.random = random;
    }

    public Generator create(Grammar grammar) {
        return new Generator(random, grammar);
    }
}
