package org.metaborg.spg.sentence.antlr.grammar;

public class Plus extends Element {
    private final Element element;

    public Plus(Element element) {
        this.element = element;
    }

    public Element getElement() {
        return element;
    }

    @Override
    public String toString() {
        return element + "+";
    }
}
