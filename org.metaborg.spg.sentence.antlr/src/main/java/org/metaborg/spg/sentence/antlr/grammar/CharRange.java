package org.metaborg.spg.sentence.antlr.grammar;

public class CharRange implements Range {
    private final String start;
    private final String end;

    public CharRange(String start, String end) {
        this.start = start;
        this.end = end;
    }

    public String getStart() {
        return start;
    }

    public String getEnd() {
        return end;
    }

    @Override
    public int size() {
        return end.charAt(0) - start.charAt(0) + 1;
    }

    @Override
    public char get(int index) {
        return (char) (start.charAt(0) + index);
    }
}
