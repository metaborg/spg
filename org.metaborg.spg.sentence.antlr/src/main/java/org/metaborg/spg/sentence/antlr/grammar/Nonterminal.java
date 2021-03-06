package org.metaborg.spg.sentence.antlr.grammar;

import java.util.Collections;
import java.util.Objects;
import java.util.Set;

public class Nonterminal implements Element {
    private final String name;

    public Nonterminal(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
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
        return name;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        Nonterminal that = (Nonterminal) o;

        return Objects.equals(name, that.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name);
    }
}
