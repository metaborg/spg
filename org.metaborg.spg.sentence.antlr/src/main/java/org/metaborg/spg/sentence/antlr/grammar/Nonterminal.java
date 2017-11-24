package org.metaborg.spg.sentence.antlr.grammar;

public class Nonterminal implements Element {
    private final String name;

    public Nonterminal(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    @Override
    public int size() {
        return 1;
    }

    @Override
    public String toString() {
        return name;
    }
}
