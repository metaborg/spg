package org.metaborg.spg.sentence.antlr.term;

import org.metaborg.spg.sentence.antlr.grammar.EmptyElement;

public class TermList implements Term {
    private final EmptyElement emptyElement;
    private final Term[] children;

    public TermList(EmptyElement emptyElement, Term[] children) {
        this.emptyElement = emptyElement;
        this.children = children;
    }

    public TermList(EmptyElement emptyElement) {
        this(emptyElement, new Term[0]);
    }

    public TermList(EmptyElement emptyElement, Term term, Term[] tail) {
        this(emptyElement, arrayOf(term, tail));
    }

    public TermList(EmptyElement emptyElement, Term term, TermList tail) {
        this(emptyElement, term, tail.getChildren());
    }

    public EmptyElement getEmptyElement() {
        return emptyElement;
    }

    public int size() {
        return children.length;
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

    private static Term[] arrayOf(Term term, Term[] children) {
        Term[] terms = new Term[children.length + 1];

        // Put term at index 0
        terms[0] = term;

        // Copy children to terms at index 1, ..., n+1
        System.arraycopy(children, 0, terms, 1, children.length);

        return terms;
    }
}
