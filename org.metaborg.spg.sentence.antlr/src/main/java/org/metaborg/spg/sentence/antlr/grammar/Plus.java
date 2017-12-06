package org.metaborg.spg.sentence.antlr.grammar;

import java.util.Collections;
import java.util.Set;

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
        return 1 + element.size();
    }

    @Override
    public Set<Element> nonterminals() {
        return Collections.singleton(this);
    }

    @Override
    public String toString() {
        return "(" + element + ")+";
    }
}
