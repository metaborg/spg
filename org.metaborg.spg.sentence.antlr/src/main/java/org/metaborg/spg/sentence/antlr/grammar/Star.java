package org.metaborg.spg.sentence.antlr.grammar;

public class Star extends Element {
    private final Element element;

    public Star(Element element) {
        this.element = element;
    }

    public Element getElement() {
        return element;
    }

    @Override
    public String toString() {
        return element + "*";
    }
}
