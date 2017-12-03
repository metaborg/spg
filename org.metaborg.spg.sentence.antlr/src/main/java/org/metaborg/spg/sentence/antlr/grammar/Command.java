package org.metaborg.spg.sentence.antlr.grammar;

public class Command implements Element {
    private final Element element;
    private final String name;

    public Command(Element element, String name) {
        this.element = element;
        this.name = name;
    }

    @Override
    public int size() {
        return 1 + element.size();
    }
}
