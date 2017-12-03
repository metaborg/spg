package org.metaborg.spg.sentence.antlr.grammar;

public class Literal implements Element {
    private final String text;

    public Literal(String text) {
        this.text = text;
    }

    public String getText() {
        return text;
    }

    @Override
    public int size() {
        return 1;
    }

    @Override
    public String toString() {
        return "'" + text + "'";
    }
}
