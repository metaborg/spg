package org.metaborg.spg.sentence.antlr.grammar;

public class Alt extends Element {
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
    public String toString() {
        return first + " | " + second;
    }
}
