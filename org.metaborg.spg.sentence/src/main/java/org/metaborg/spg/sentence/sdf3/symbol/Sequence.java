package org.metaborg.spg.sentence.sdf3.symbol;

public class Sequence extends Symbol {
    private final Symbol head;
    private final Iterable<Symbol> tail;

    public Sequence(Symbol head, Iterable<Symbol> tail) {
        this.head = head;
        this.tail = tail;
    }
}
