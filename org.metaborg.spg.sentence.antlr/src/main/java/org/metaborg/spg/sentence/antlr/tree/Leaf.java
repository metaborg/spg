package org.metaborg.spg.sentence.antlr.tree;

public class Leaf implements Tree {
    public static final Leaf EMPTY = new Leaf("");
    private final String text;

    public Leaf(String text) {
        this.text = text;
    }

    public String getText() {
        return text;
    }

    @Override
    public Tree[] getChildren() {
        return new Tree[0];
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
