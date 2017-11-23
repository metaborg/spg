package org.metaborg.spg.sentence.antlr.grammar;

public class CharacterClass extends Element {
    private final Ranges ranges;

    public CharacterClass(Ranges ranges) {
        this.ranges = ranges;
    }

    public Ranges getRanges() {
        return ranges;
    }
}
