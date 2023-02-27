package org.metaborg.spg.sentence.antlr.grammar;

import java.util.Collection;
import java.util.Collections;
import java.util.Set;

public interface EmptyElement {
    int size();

    default Set<Element> nonterminals() {
        return Collections.emptySet();
    }

    default Collection<EmptyElement> toList() {
        return Collections.singletonList(this);
    }
}
