package org.metaborg.spg.sentence.antlr.grammar;

import java.util.Collections;
import java.util.Set;

public class Star implements Element {
    private final Element element;

    public Star(Element element) {
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
    public Set<Element> nonterminals() {
        return Collections.singleton(this);
    }

    @Override
    public String toString() {
        return "(" + element + ")*";
    }
}
