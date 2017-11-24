package org.metaborg.spg.sentence.antlr.grammar;

public class Alt implements Element {
    private final Element first;
    private final Element second;

    public Alt(Element first, Element second) {
        this.first = first;
        this.second = second;
    }

    public Element getFirst() {
        return first;
    }

    public Element getSecond() {
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
