package org.metaborg.spg.sentence.antlr.grammar;

public class Rule {
    private final String name;
    private final EmptyElement emptyElement;

    public Rule(String name, EmptyElement emptyElement) {
        this.name = name;
        this.emptyElement = emptyElement;
    }

    public String getName() {
        return name;
    }

    public EmptyElement getEmptyElement() {
        return emptyElement;
    }

    public boolean isLexical() {
        return java.lang.Character.isUpperCase(name.charAt(0));
    }

    @Override
    public String toString() {
        return name + ": " + emptyElement + ";";
    }
}
