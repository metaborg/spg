package org.metaborg.spg.sentence.antlr.tree;

public interface Tree {
    Tree[] getChildren();

    String toString(boolean whitespace);
}
