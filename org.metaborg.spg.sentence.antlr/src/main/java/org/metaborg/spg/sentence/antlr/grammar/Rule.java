package org.metaborg.spg.sentence.antlr.grammar;

import org.apache.commons.lang3.StringUtils;

public class Rule {
    private final String name;
    private final Element element;

    public Rule(String name, Element element) {
        this.name = name;
        this.element = element;
    }

    public String getName() {
        return name;
    }

    public Element getElement() {
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
