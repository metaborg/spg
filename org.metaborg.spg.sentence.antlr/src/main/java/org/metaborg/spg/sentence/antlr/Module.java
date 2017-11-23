package org.metaborg.spg.sentence.antlr;

import com.google.inject.AbstractModule;

import java.util.Random;

public class Module extends AbstractModule {
    private final Random random;

    public Module(long seed) {
        this.random = new Random(seed);
    }

    public Module() {
        this.random = new Random();
    }

    @Override
    protected void configure() {
        bind(Random.class).toInstance(random);
    }
}
