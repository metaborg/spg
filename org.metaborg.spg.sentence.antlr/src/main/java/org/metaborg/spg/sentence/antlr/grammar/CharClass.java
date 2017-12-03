package org.metaborg.spg.sentence.antlr.grammar;

public class CharClass implements CharacterClass {
    private final Ranges ranges;

    public CharClass(Ranges ranges) {
        this.ranges = ranges;
    }

    public Ranges getRanges() {
        return ranges;
    }

    @Override
    public int size() {
        return 1;
    }
}
