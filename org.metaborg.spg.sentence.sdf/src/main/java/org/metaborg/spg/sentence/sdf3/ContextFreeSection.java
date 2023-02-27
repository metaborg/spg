package org.metaborg.spg.sentence.sdf3;

import java.util.Collection;

public class ContextFreeSection extends Section {
    private final Collection<Production> productions;

    public ContextFreeSection(Collection<Production> productions) {
        this.productions = productions;
    }

    @Override
    public Collection<Production> getProductions() {
        return productions;
    }
}
