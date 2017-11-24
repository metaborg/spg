package org.metaborg.spg.sentence.antlr.grammar;

public class Rule {
    private final String name;
    private final ElementOpt element;

    public Rule(String name, ElementOpt element) {
        this.name = name;
        this.element = element;
    }

    public String getName() {
        return name;
    }

    public ElementOpt getElement() {
        return element;
    }

    public boolean isLexical() {
        return Character.isUpperCase(name.charAt(0));
    }

    @Override
    public String toString() {
        return name + ": " + element + ";";
    }
}
