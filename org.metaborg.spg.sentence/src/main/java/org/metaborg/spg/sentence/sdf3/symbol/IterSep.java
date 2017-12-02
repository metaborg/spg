package org.metaborg.spg.sentence.sdf3.symbol;

public class IterSep extends Symbol {
    private final Symbol symbol;
    private final Symbol separator;

    public IterSep(Symbol symbol, Symbol separator) {
        this.symbol = symbol;
        this.separator = separator;
    }

    public Symbol getSymbol() {
        return symbol;
    }

    @Override
    public String toString() {
        return "{" + symbol + " " + separator + "}+";
    }
}
