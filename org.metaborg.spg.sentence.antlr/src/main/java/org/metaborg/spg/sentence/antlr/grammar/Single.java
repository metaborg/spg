package org.metaborg.spg.sentence.antlr.grammar;

public class Single implements Char {
    private final String s;

    public Single(String s) {
        this.s = s;
    }

    public String getText() {
        return s;
    }

    @Override
    public int size() {
        return 1;
    }

    @Override
    public char get(int index) {
        return s.charAt(0);
    }
}
