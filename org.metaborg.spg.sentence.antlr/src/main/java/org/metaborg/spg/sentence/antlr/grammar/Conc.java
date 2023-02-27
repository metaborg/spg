package org.metaborg.spg.sentence.antlr.grammar;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.metaborg.util.collection.Sets;

public class Conc implements Element {
    private final Element first;
    private final Element second;

    private Collection<EmptyElement> list;
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

    public Collection<EmptyElement> toList() {
        if (list == null) {
            list = new ArrayList<>(first.toList());
            list.addAll(second.toList());
        }

        return list;
    }

    public int divideSize(int size) {
        Element headElement = this.getFirst();

        if (headElement instanceof Literal) {
            return 1;
        } else {
            if (elementsSize == -1 && literalsSize == -1) {
                Collection<EmptyElement> elements = this.toList();
                List<Literal> literals = literals(elements);

                elementsSize = elements.size();
                literalsSize = literals.size();
            }
        }

        return (int) ((size - literalsSize) / (double) elementsSize);
    }

    private List<Literal> literals(Collection<EmptyElement> elements) {
        return elements.stream().filter(e -> e instanceof Literal).map(e -> (Literal) e).collect(Collectors.toList());
    }

    @Override
    public String toString() {
        return first + " " + second;
    }
}
