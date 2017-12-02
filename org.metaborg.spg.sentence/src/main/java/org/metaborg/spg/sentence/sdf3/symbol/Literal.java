package org.metaborg.spg.sentence.sdf3.symbol;

public class Literal extends Symbol {
    private final String text;

    public Literal(String text) {
        this.text = text;
    }

    @Override
    public String toString() {
        return "\"" + text + "\"";
    }
}
