package org.metaborg.spg.sentence.antlr.grammar;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import java.util.Set;

public class Conc implements Element {
    private final Element first;
    private final Element second;

    public Conc(Element first, Element second) {
        this.first = first;
        this.second = second;
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
        return Iterables.concat(first.toList(), second.toList());
    }

    @Override
    public String toString() {
        return first + " " + second;
    }
}
