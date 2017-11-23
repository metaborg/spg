package org.metaborg.spg.sentence.antlr.grammar;

public class Nonterminal extends Element {
    private final String name;

    public Nonterminal(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    @Override
    public String toString() {
        return name;
    }
}
