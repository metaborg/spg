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

    private CharacterClassConc conc(CharacterClassRange first, Symbol second) {
        return new CharacterClassConc(first, second);
    }

    private CharacterClassRange range(int min, int max) {
        return new CharacterClassRange(
                new CharacterClassNumeric(min),
                new CharacterClassNumeric(max)
        );
    }
}
