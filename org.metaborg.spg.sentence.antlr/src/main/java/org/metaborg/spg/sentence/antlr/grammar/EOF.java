package org.metaborg.spg.sentence.antlr.grammar;

public class EOF implements Element {
    @Override
    public int size() {
        return 0;
    }

    @Override
    public String toString() {
        return "EOF";
    }
}
