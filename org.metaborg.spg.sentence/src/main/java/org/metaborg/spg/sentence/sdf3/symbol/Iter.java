package org.metaborg.spg.sentence.sdf3.symbol;

public class Iter extends Symbol {
    private final Symbol symbol;

    public Iter(Symbol symbol) {
        this.symbol = symbol;
    }

    public Symbol getSymbol() {
        return symbol;
    }

    @Override
    public String toString() {
        return "{" + symbol + "}+";
    }
}
