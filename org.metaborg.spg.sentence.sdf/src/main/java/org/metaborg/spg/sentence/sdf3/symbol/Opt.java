package org.metaborg.spg.sentence.sdf3.symbol;

public class Opt extends Symbol {
    private final Symbol symbol;

    public Opt(Symbol symbol) {
        this.symbol = symbol;
    }

    public Symbol getSymbol() {
        return symbol;
    }

    @Override
    public String toString() {
        return symbol + "?";
    }
}
