package org.metaborg.spg.sentence.antlr.term;

import org.metaborg.spg.sentence.antlr.grammar.ElementOpt;

public class Appl implements Term {
    private final ElementOpt element;
    private final Term[] children;

    public Appl(ElementOpt element, Term[] children) {
        this.element = element;
        this.children = children;
    }

    public ElementOpt getElementOpt() {
        return element;
    }

    @Override
    public Term[] getChildren() {
        return children;
    }

    @Override
    public String toString() {
        return toString(true);
    }

    @Override
    public String toString(boolean whitespace) {
        StringBuilder stringBuilder = new StringBuilder();

        for (Term child : children) {
            stringBuilder.append(child.toString(whitespace));

            if (whitespace) {
                stringBuilder.append(" ");
            }
        }

        return stringBuilder.toString();
    }
}
