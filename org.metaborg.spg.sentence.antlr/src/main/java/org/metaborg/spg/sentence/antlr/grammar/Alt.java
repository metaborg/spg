package org.metaborg.spg.sentence.antlr.grammar;

public class Alt implements Element {
    private final EmptyElement first;
    private final EmptyElement second;

    public Alt(EmptyElement first, EmptyElement second) {
        this.first = first;
        this.second = second;
    }

    public EmptyElement getFirst() {
        return first;
    }

    public EmptyElement getSecond() {
        return second;
    }

    @Override
    public int size() {
        return first.size() + second.size();
    }

    @Override
    public String toString() {
        return first + " | " + second;
    }
}
