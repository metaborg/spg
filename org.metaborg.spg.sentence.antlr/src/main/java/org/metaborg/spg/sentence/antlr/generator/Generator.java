package org.metaborg.spg.sentence.antlr.generator;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Multimap;
import org.metaborg.spg.sentence.antlr.grammar.*;

import java.util.Optional;
import java.util.Random;

import static java.util.Optional.empty;
import static java.util.Optional.of;

public class Generator {
    private final Random random;
    private final Grammar grammar;
    private final Multimap<String, Rule> cache;

    public Generator(Random random, Grammar grammar) {
        this.random = random;
        this.grammar = grammar;
        this.cache = createCache(grammar);
    }

    private Multimap<String, Rule> createCache(Grammar grammar) {
        ListMultimap<String, Rule> multimap = ArrayListMultimap.create();

        for (Rule rule : grammar.getRules()) {
            multimap.put(rule.getName(), rule);
        }

        return multimap;
    }

    // TODO: Start from the start symbol, generate top-down, return string.
    // TODO: Use StringBuilder for better performance
    // TODO: Add size bounds, return and deal with optionals everywhere
    // TODO: How to deal with whitespace?

    public Optional<String> generate(int size) {
        Nonterminal start = grammar.getStart();

        return generateNonterminal(start, size);
    }

    public Optional<String> generateNonterminal(Nonterminal nonterminal, int size) {
        if (size <= 0) {
            return empty();
        }

        for (Rule rule : cache.get(nonterminal.getName())) {
            Optional<String> sentenceOpt = forRule(rule, size);

            if (sentenceOpt.isPresent()) {
                return sentenceOpt;
            }
        }

        return empty();
    }

    public Optional<String> forRule(Rule rule, int size) {
        Optional<String> sentenceOpt = forElement(rule.getElement(), size);
        
        return sentenceOpt;
    }

    public Optional<String> forElement(Element element, int size) {
        if (size <= 0) {
            return empty();
        }

        if (element instanceof Conc) {
            return generateConc((Conc) element, size);
        } else if (element instanceof Alt) {
            return generateAlt((Alt) element, size);
        } else if (element instanceof Star) {
            return generateStar((Star) element, size);
        } else if (element instanceof Opt) {
            return generateOpt((Opt) element, size);
        } else if (element instanceof Plus) {
            return generatePlus((Plus) element, size);
        } else if (element instanceof Nonterminal) {
            return generateNonterminal((Nonterminal) element, size);
        } else if (element instanceof Literal) {
            return generateLiteral((Literal) element, size);
        } else if (element instanceof CharacterClass) {
            return generateCharacterClass((CharacterClass) element, size);
        }

        throw new IllegalStateException("Unknown element: " + element);
    }

    private Optional<String> generateSingle(Single s, int size) {
        return Optional.ofNullable(s.getText());
    }

    private Optional<String> generateLiteral(Literal element, int size) {
        return of(element.getText());
    }

    private Optional<String> generateConc(Conc element, int size) {
        Optional<String> headOpt = forElement(element.getFirst(), size / 2);

        if (headOpt.isPresent()) {
            Optional<String> tailOpt = forElement(element.getSecond(), size / 2);

            if (tailOpt.isPresent()) {
                return of(headOpt.get() + tailOpt.get());
            }
        }

        return empty();
    }

    private Optional<String> generateAlt(Alt element, int size) {
        if (random.nextInt(2) == 0) {
            return forElement(element.getFirst(), size);
        } else {
            return forElement(element.getSecond(), size);
        }
    }

    private Optional<String> generateOpt(Opt element, int size) {
        if (random.nextInt(2) == 0) {
            return of("");
        } else {
            return forElement(element.getElement(), size);
        }
    }

    private Optional<String> generateStar(Star element, int size) {
        Optional<String> headOpt = forElement(element.getElement(), size / 2);

        if (headOpt.isPresent()) {
            Optional<String> tailOpt = forElement(element.getElement(), size / 2);

            if (tailOpt.isPresent()) {
                return of(headOpt.get() + " " + tailOpt.get());
            }
        }

        return empty();
    }

    private Optional<String> generatePlus(Plus element, int size) {
        Optional<String> headOpt = forElement(element.getElement(), size);

        if (headOpt.isPresent()) {
            Optional<String> tailOpt = generateStar(new Star(element.getElement()), size);

            if (tailOpt.isPresent()) {
                return of(headOpt.get() + " " + tailOpt.get());
            }
        }

        return empty();
    }

    private Optional<String> generateCharacterClass(CharacterClass element, int size) {
        return generateRanges(element.getRanges(), size);
    }

    private Optional<String> generateRanges(Ranges ranges, int size) {
        if (ranges instanceof RangesConc) {
            return generateRangesConc((RangesConc) ranges);
        } else if (ranges instanceof Range) {
            return generateRange((Range) ranges, size);
        }

        throw new IllegalStateException("Unknown ranges: " + ranges);
    }

    private Optional<String> generateRangesConc(RangesConc ranges) {
        int size = ranges.size();
        int rand = random.nextInt(size);

        return of(String.valueOf(ranges.get(rand)));
    }

    private Optional<String> generateRange(Range range, int size) {
        if (range instanceof CharRange) {
            return generateCharRange((CharRange) range);
        } else if (range instanceof Char) {
            return generateChar((Char) range, size);
        }

        throw new IllegalStateException("Unknown range: " + range);
    }

    private Optional<String> generateCharRange(CharRange range) {
        int size = range.size();
        int rand = random.nextInt(size);

        return of(String.valueOf(range.get(rand)));
    }

    private Optional<String> generateChar(Char c, int size) {
        if (c instanceof Single) {
            return generateSingle((Single) c, size);
        }

        throw new IllegalStateException("Unknown char: " + c);
    }
}
