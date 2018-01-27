package org.metaborg.spg.sentence.utils;

import static java.util.stream.Stream.empty;
import static java.util.stream.Stream.of;
import static org.metaborg.spg.sentence.generator.Generator.MAXIMUM_PRINTABLE;
import static org.metaborg.spg.sentence.generator.Generator.MINIMUM_PRINTABLE;
import static org.metaborg.spg.sentence.shared.utils.StreamUtils.cons;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.metaborg.sdf2table.grammar.CharacterClass;
import org.metaborg.sdf2table.grammar.Symbol;
import org.metaborg.sdf2table.io.ParseTableGenerator;

public class SymbolUtils {
    public static Symbol toPrintable(CharacterClass characterClassConc) {
        List<Symbol> symbols = characterClassToList(characterClassConc).collect(Collectors.toList());
        List<Symbol> printableSymbols = toPrintable(symbols).collect(Collectors.toList());

        return listToCharacterClass(printableSymbols);
    }

    public static Stream<Symbol> toPrintable(List<Symbol> symbols) {
        // FIXME reimplement this according to the new character class representation

        if(symbols.size() == 0) {
            return empty();
        } else if(symbols.size() == 1) {
            CharacterClass characterClass = (CharacterClass) symbols.get(0);

            return of(new CharacterClass(ParseTableGenerator.getCharacterClassFactory().fromRange(
                Math.max(MINIMUM_PRINTABLE, characterClass.min()), Math.min(MAXIMUM_PRINTABLE, characterClass.max()))));
        } else {
            Symbol head = symbols.get(0);
            int headMinimum = getMinimum(head);
            int headMaximum = getMaximum(head);

            Stream<Symbol> tail = toPrintable(tail(symbols));

            if(headMinimum < MINIMUM_PRINTABLE) {
                if(headMaximum < MINIMUM_PRINTABLE) {
                    return tail;
                } else {
                    Symbol nhead = range(MINIMUM_PRINTABLE, headMaximum);

                    return cons(nhead, tail);
                }
            } else {
                if(headMaximum > MAXIMUM_PRINTABLE) {
                    return empty();
                } else {
                    return cons(head, tail);
                }
            }
        }
    }

    private static int getMinimum(Symbol symbol) {
        if(symbol instanceof CharacterClass) {
            return ((CharacterClass) symbol).min();
        }

        throw new IllegalStateException();
    }

    private static int getMaximum(Symbol symbol) {
        if(symbol instanceof CharacterClass) {
            return ((CharacterClass) symbol).max();
        }

        throw new IllegalStateException();
    }

    private static Stream<Symbol> characterClassToList(Symbol characterClass) {
        if(characterClass instanceof CharacterClass) {
            return of(characterClass);
        }

        throw new IllegalStateException("Unsupported character class symbol: " + characterClass);
    }

    private static Symbol listToCharacterClass(List<Symbol> symbols) {
        if(symbols.size() == 1) {
            return symbols.get(0);
        } else {
            return CharacterClass.union((CharacterClass) symbols.get(0),
                (CharacterClass) listToCharacterClass(tail(symbols)));
        }
    }

    public static char get(Symbol symbol, int index) {
        if(symbol instanceof CharacterClass) {
            // FIXME reimplement this according to the new character class representation


        }

        // if(symbol instanceof CharacterClassConc) {
        // CharacterClassConc characterClassConc = (CharacterClassConc) symbol;
        // int characterClassConcSize = size(characterClassConc.first());
        //
        // if(index < characterClassConcSize) {
        // return get(characterClassConc.first(), index);
        // } else {
        // return get(characterClassConc.second(), index - characterClassConcSize);
        // }
        // } else if(symbol instanceof CharacterClassRange) {
        // CharacterClassRange characterClassRange = (CharacterClassRange) symbol;
        //
        // return (char) (number(characterClassRange.start()) + index);
        // } else if(symbol instanceof CharacterClassNumeric) {
        // CharacterClassNumeric characterClassNumeric = (CharacterClassNumeric) symbol;
        //
        // return (char) number(characterClassNumeric);
        // }

        throw new IllegalStateException("Unknown symbol: " + symbol);
    }

    public static int size(Symbol symbol) {
        // FIXME reimplement this according to the new character class representation

        // if(symbol instanceof CharacterClassConc) {
        // CharacterClassConc characterClassConc = (CharacterClassConc) symbol;
        //
        // return size(characterClassConc.first()) + size(characterClassConc.second());
        // } else if(symbol instanceof CharacterClassRange) {
        // CharacterClassRange characterClassRange = (CharacterClassRange) symbol;
        //
        // return number(characterClassRange.end()) - number(characterClassRange.start()) + 1;
        // } else if(symbol instanceof CharacterClassNumeric) {
        // return 1;
        // }

        throw new IllegalStateException("Unexpected symbol: " + symbol);
    }

    public static int number(Symbol symbol) {
        // FIXME reimplement this according to the new character class representation
        
        // if(symbol instanceof CharacterClassNumeric) {
        // CharacterClassNumeric characterClassNumeric = (CharacterClassNumeric) symbol;
        //
        // return characterClassNumeric.getCharacter();
        // }

        throw new IllegalStateException("Unexpected symbol: " + symbol);
    }

    private static Symbol range(int min, int max) {
        return new CharacterClass(ParseTableGenerator.getCharacterClassFactory().fromRange(min, max));
    }

    private static <T> List<T> tail(List<T> list) {
        return list.subList(1, list.size());
    }
}
