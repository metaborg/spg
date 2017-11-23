package org.metaborg.spg.sentence.antlr.grammar;

public class Conc extends Element {
    private final Element first;
    private final Element second;

    public Conc(Element first, Element second) {
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
        return first + " " + second;
    }
}
