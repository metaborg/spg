package org.metaborg.spg.sentence.antlr.grammar;

public class Literal extends Element {
    private final String text;

    public Literal(String text) {
        this.text = text;
    }

    public String getText() {
        return text;
    }

    @Override
    public String toString() {
        return "'" + text + "'";
    }
}
