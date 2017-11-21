package org.metaborg.spg.sentence.generator;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.metaborg.sdf2table.grammar.CharacterClassConc;
import org.metaborg.sdf2table.grammar.CharacterClassNumeric;
import org.metaborg.sdf2table.grammar.CharacterClassRange;
import org.metaborg.sdf2table.grammar.Symbol;
import org.metaborg.spg.sentence.Utils;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class GeneratorTest {
    @Test
    @DisplayName("Printable [0-9,11-33,35-255] = [32-33,35-126]")
    public void testToPrintable() {
        CharacterClassConc characterClassConc = conc(
                range(0, 9),
                conc(
                        range(11, 33),
                        range(35, 255)
                )
        );

        CharacterClassConc expectedCharacterClass = conc(
                range(32, 33),
                range(35, 126)
        );

        Symbol actualCharacterClass = Utils.toPrintable(characterClassConc);

        assertEquals(expectedCharacterClass, actualCharacterClass);
    }

    @Test
    @DisplayName("Size [a] = 1")
    public void testSizeNumeric() {
        assertEquals(1, Utils.size(number(94)));
    }

    @Test
    @DisplayName("Size [0-9] = 10")
    public void testSizeRange() {
        assertEquals(10, Utils.size(range(0, 9)));
    }

    @Test
    @DisplayName("Size [0-9A-Za-z] = 62")
    public void testSizeConcEndRange() {
        CharacterClassConc characterClass = conc(
                range(48, 57),
                conc(
                        range(65, 90),
                        range(97, 122)
                )
        );

        assertEquals(62, Utils.size(characterClass));
    }

    @Test
    @DisplayName("Size [0-9A-Za] = 37")
    public void testSizeConcEndNumber() {
        CharacterClassConc characterClass = conc(
                range(48, 57),
                conc(
                        range(65, 90),
                        number(97)
                )
        );

        assertEquals(37, Utils.size(characterClass));
    }

    @Test
    @DisplayName("Get 0th character from [65-90] = 'A'")
    public void testGetCharacterClassRange() {
        CharacterClassRange characterClassRange = range(65, 90);

        assertEquals('A', Utils.get(characterClassRange, 0));
    }

    @Test
    @DisplayName("Get 26th character from [65-90,97-122] = 'a'")
    public void testGetCharacterClassRangeNumber() {
        CharacterClassConc characterClassConc = conc(
                range(65, 90),
                number(97)
        );

        assertEquals('a', Utils.get(characterClassConc, 26));
    }

    @Test
    @DisplayName("Get 99th character from [65-90,97-122] = 'c'")
    public void testGetCharacterClassConc() {
        CharacterClassConc characterClassConc = conc(
                range(65, 90),
                range(97, 122)
        );

        assertEquals('c', Utils.get(characterClassConc, 28));
    }

    private CharacterClassConc conc(CharacterClassRange first, Symbol second) {
        return new CharacterClassConc(first, second);
    }

    private CharacterClassRange range(int min, int max) {
        return new CharacterClassRange(
                number(min),
                number(max)
        );
    }

    private CharacterClassNumeric number(int n) {
        return new CharacterClassNumeric(n);
    }
}
