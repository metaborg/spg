package org.metaborg.spg.sentence.antlr.grammar;

public class Plus implements Element {
    private final Element element;

    public Plus(Element element) {
        this.element = element;
    }

    public Element getElement() {
        return element;
    }

    @Override
    public int size() {
        return 1;
    }

    @Override
    public String toString() {
        return element + "+";
    }
}
