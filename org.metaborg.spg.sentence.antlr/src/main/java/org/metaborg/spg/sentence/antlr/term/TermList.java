package org.metaborg.spg.sentence.antlr.term;

import org.metaborg.spg.sentence.antlr.grammar.ElementOpt;

public class TermList implements Term {
    private final ElementOpt element;
    private final Term[] children;

    public TermList(ElementOpt element, Term[] children) {
        this.element = element;
        this.children = children;
    }

    public TermList(ElementOpt element) {
        this(element, new Term[0]);
    }

    public TermList(ElementOpt element, Term term, Term[] tail) {
        this(element, arrayOf(term, tail));
    }

    public TermList(ElementOpt element, Term term, TermList tail) {
        this(element, term, tail.getChildren());
    }

    public ElementOpt getElement() {
        return element;
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
