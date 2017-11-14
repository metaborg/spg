package org.metaborg.spg.sentence;

import org.metaborg.sdf2table.grammar.CharacterClassConc;
import org.metaborg.sdf2table.grammar.CharacterClassNumeric;
import org.metaborg.sdf2table.grammar.CharacterClassRange;
import org.metaborg.sdf2table.grammar.Symbol;
import org.metaborg.spg.sentence.functional.CheckedConsumer;
import org.metaborg.spg.sentence.functional.CheckedPredicate;

import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.stream.Stream.concat;
import static java.util.stream.Stream.empty;
import static java.util.stream.Stream.of;
import static org.metaborg.spg.sentence.generator.Generator.MAXIMUM_PRINTABLE;
import static org.metaborg.spg.sentence.generator.Generator.MINIMUM_PRINTABLE;

public class Utils {
    public static <T> Predicate<T> uncheckPredicate(CheckedPredicate<T, Exception> function) {
        return element -> {
            try {
                return function.test(element);
            } catch (Exception ex) {
                if (ex instanceof RuntimeException) {
                    throw (RuntimeException) ex;
                } else {
                    throw new RuntimeException(ex);
                }
            }
        };
    }

    public static <T> Consumer<T> uncheckConsumer(CheckedConsumer<T, Exception> function) {
        return element -> {
            try {
                function.accept(element);
            } catch (Exception ex) {
                if (ex instanceof RuntimeException) {
                    throw (RuntimeException) ex;
                } else {
                    throw new RuntimeException(ex);
                }
            }
        };
    }

    public static <T> Iterable<T> shuffle(List<T> list) {
        Collections.shuffle(list);

        return list;
    }

    public static Symbol toPrintable(CharacterClassConc characterClassConc) {
        List<Symbol> symbols = characterClassToList(characterClassConc).collect(Collectors.toList());
        List<Symbol> printableSymbols = toPrintable(symbols).collect(Collectors.toList());

        return listToCharacterClass(printableSymbols);
    }

    public static Stream<Symbol> toPrintable(List<Symbol> symbols) {
        if (symbols.size() == 0) {
            return empty();
        } else if (symbols.size() == 1) {
            CharacterClassRange characterClassRange = (CharacterClassRange) symbols.get(0);

            return of(new CharacterClassRange(
                    new CharacterClassNumeric(Math.max(MINIMUM_PRINTABLE, characterClassRange.minimum())),
                    new CharacterClassNumeric(Math.min(MAXIMUM_PRINTABLE, characterClassRange.maximum()))
            ));
        } else {
            CharacterClassRange head = (CharacterClassRange) symbols.get(0);
            Stream<Symbol> tail = toPrintable(tail(symbols));

            if (head.minimum() < MINIMUM_PRINTABLE) {
                if (head.maximum() < MINIMUM_PRINTABLE) {
                    return tail;
                } else {
                    Symbol nhead = range(MINIMUM_PRINTABLE, head.maximum());

                    return concat(of(nhead), tail);
                }
            } else {
                if (head.maximum() > MAXIMUM_PRINTABLE) {
                    return empty();
                } else {
                    return concat(of(head), tail);
                }
            }
        }
    }

    private static Stream<Symbol> characterClassToList(Symbol characterClass) {
        if (characterClass instanceof CharacterClassConc) {
            CharacterClassConc characterClassConc = (CharacterClassConc) characterClass;
            Symbol first = characterClassConc.first();
            Symbol second = characterClassConc.second();

            return concat(of(first), characterClassToList(second));
        } else if (characterClass instanceof CharacterClassRange) {
            return of(characterClass);
        }

        throw new IllegalStateException("Unsupported character class symbol: " + characterClass);
    }

    private static Symbol listToCharacterClass(List<Symbol> symbols) {
        if (symbols.size() == 1) {
            return symbols.get(0);
        } else {
            return new CharacterClassConc(symbols.get(0), listToCharacterClass(tail(symbols)));
        }
    }

    private static Symbol range(int min, int max) {
        return new CharacterClassRange(
                new CharacterClassNumeric(min),
                new CharacterClassNumeric(max)
        );
    }

    private static <T> List<T> tail(List<T> list) {
        return list.subList(1, list.size());
    }
}
