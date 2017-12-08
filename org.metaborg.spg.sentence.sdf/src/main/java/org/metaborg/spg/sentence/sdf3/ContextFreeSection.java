package org.metaborg.spg.sentence.sdf3;

public class ContextFreeSection extends Section {
    private final Iterable<Production> productions;

    public ContextFreeSection(Iterable<Production> productions) {
        this.productions = productions;
    }

    @Override
    public Iterable<Production> getProductions() {
        return productions;
    }
}
