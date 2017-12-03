package org.metaborg.spg.sentence.antlr.grammar;

public class EOF implements Element {
    @Override
    public int size() {
        return 1;
    }

    @Override
    public String toString() {
        return "EOF";
    }
}
