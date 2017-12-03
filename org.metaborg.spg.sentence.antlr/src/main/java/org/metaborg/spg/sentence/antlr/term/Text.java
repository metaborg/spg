package org.metaborg.spg.sentence.antlr.term;

public class Text implements Term {
    public static final Text EMPTY = new Text("");
    private final String text;

    public Text(String text) {
        this.text = text;
    }

    public String getText() {
        return text;
    }

    @Override
    public Term[] getChildren() {
        return new Term[0];
    }

    @Override
    public String toString() {
        return text;
    }

    public String toString(boolean whitespace) {
        if (whitespace) {
            return toString();
        }

        return text;
    }
}
