package org.metaborg.spg.sentence.antlr.grammar;

import com.google.common.collect.FluentIterable;
import com.google.common.collect.Iterables;
import com.google.common.collect.Sets;

import java.util.Set;

public class Conc implements Element {
    private final Element first;
    private final Element second;

    private Iterable<EmptyElement> list;
    private int literalsSize;
    private int elementsSize;

    public Conc(Element first, Element second) {
        this.first = first;
        this.second = second;
        this.literalsSize = -1;
        this.elementsSize = -1;
    }

    public Element getFirst() {
        return first;
    }

    public Element getSecond() {
        return second;
    }

    @Override
    public int size() {
        return first.size() + second.size();
    }

    @Override
    public Set<Element> nonterminals() {
        return Sets.union(first.nonterminals(), second.nonterminals());
    }

    public Iterable<EmptyElement> toList() {
        if (list == null) {
            list = Iterables.concat(first.toList(), second.toList());
        }

        return list;
    }

    public int divideSize(int size) {
        Element headElement = this.getFirst();

        if (headElement instanceof Literal) {
            return 1;
        } else {
            if (elementsSize == -1 && literalsSize == -1) {
                Iterable<EmptyElement> elements = this.toList();
                Iterable<Literal> literals = literals(elements);

                elementsSize = Iterables.size(elements);
                literalsSize = Iterables.size(literals);
            }
        }

        return (int) ((size - literalsSize) / (double) elementsSize);
    }

    private FluentIterable<Literal> literals(Iterable<EmptyElement> elements) {
        return FluentIterable
                .from(elements)
                .filter(Literal.class);
    }

    @Override
    public String toString() {
        return first + " " + second;
    }
}
