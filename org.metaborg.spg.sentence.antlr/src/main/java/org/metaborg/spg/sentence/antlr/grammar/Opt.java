package org.metaborg.spg.sentence.antlr.grammar;

public class Opt extends Element {
    private final Element element;

    public Opt(Element element) {
        this.element = element;
    }

    public Element getElement() {
        return element;
    }

    @Override
    public String toString() {
        return element + "?";
    }
}
