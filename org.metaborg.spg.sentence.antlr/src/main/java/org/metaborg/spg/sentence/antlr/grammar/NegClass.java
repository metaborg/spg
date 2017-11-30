package org.metaborg.spg.sentence.antlr.grammar;

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
}
