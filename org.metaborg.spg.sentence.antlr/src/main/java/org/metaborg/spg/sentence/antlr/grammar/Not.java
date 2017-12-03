package org.metaborg.spg.sentence.antlr.grammar;

public class Not implements Element {
    private final Element element;

    public Not(Element element) {
        this.element = element;
    }

    public Element getElement() {
        return element;
    }

    @Override
    public int size() {
        return 1 + element.size();
    }

    @Override
    public String toString() {
        return "~(" + element + ")?";
    }
}
