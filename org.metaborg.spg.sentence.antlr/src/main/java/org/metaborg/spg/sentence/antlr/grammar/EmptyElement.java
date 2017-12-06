package org.metaborg.spg.sentence.antlr.grammar;

import java.util.Collections;
import java.util.Set;

public interface EmptyElement {
    int size();

    default Set<Element> nonterminals() {
        return Collections.emptySet();
    }

    default Iterable<EmptyElement> toList() {
        return Collections.singletonList(this);
    }
}
