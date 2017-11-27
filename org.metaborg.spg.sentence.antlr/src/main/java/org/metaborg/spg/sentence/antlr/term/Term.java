package org.metaborg.spg.sentence.antlr.term;

public interface Term {
    Term[] getChildren();

    String toString(boolean whitespace);
}
