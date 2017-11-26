package org.metaborg.spg.sentence.antlr.tree;

import org.metaborg.spg.sentence.antlr.grammar.ElementOpt;

public class Node implements Tree {
    private final ElementOpt element;
    private final Tree[] children;

    public Node(ElementOpt element, Tree[] children) {
        this.element = element;
        this.children = children;
    }

    public ElementOpt getElementOpt() {
        return element;
    }

    public Tree[] getChildren() {
        return children;
    }

    @Override
    public String toString() {
        return toString(true);
    }

    @Override
    public String toString(boolean whitespace) {
        StringBuilder stringBuilder = new StringBuilder();

        for (Tree child : children) {
            stringBuilder.append(child.toString(whitespace));

            if (whitespace) {
                stringBuilder.append(" ");
            }
        }

        return stringBuilder.toString();
    }
}
