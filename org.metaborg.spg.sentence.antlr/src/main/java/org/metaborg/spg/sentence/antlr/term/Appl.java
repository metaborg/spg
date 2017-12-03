package org.metaborg.spg.sentence.antlr.term;

import org.metaborg.spg.sentence.antlr.grammar.EmptyElement;

public class Appl implements Term {
    private final EmptyElement emptyElement;
    private final Term[] children;

    public Appl(EmptyElement emptyElement, Term[] children) {
        this.emptyElement = emptyElement;
        this.children = children;
    }

    public EmptyElement getEmptyElement() {
        return emptyElement;
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
