package org.metaborg.spg.sentence.sdf3.symbol;

public class Nonterminal extends Symbol {
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
