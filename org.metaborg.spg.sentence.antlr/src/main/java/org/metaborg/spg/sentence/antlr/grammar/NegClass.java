package org.metaborg.spg.sentence.antlr.grammar;

import java.util.Set;

public class NegClass implements CharacterClass {
    private final CharacterClass characterClass;

    public NegClass(CharacterClass characterClass) {
        this.characterClass = characterClass;
    }

    public CharacterClass getCharacterClass() {
        return characterClass;
    }

    @Override
    public int size() {
        return 1 + characterClass.size();
    }

    @Override
    public Set<Element> nonterminals() {
        return characterClass.nonterminals();
    }

    @Override
    public String toString() {
        return "~" + characterClass;
    }
}
