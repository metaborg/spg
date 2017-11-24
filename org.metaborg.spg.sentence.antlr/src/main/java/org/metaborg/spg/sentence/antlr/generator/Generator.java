package org.metaborg.spg.sentence.antlr.generator;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Multimap;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.metaborg.spg.sentence.antlr.grammar.*;

import java.util.Arrays;
import java.util.Optional;
import java.util.Random;

import static java.util.Optional.empty;
import static java.util.Optional.of;

public class Generator {
    private final Random random;
    private final Multimap<String, Rule> cache;

    public Generator(Random random, Grammar grammar) {
        this.random = random;
        this.cache = createCache(grammar);
    }

    private Multimap<String, Rule> createCache(Grammar grammar) {
        ListMultimap<String, Rule> multimap = ArrayListMultimap.create();

        for (Rule rule : grammar.getRules()) {
            multimap.put(rule.getName(), rule);
        }

        return multimap;
    }

    public Optional<String> generate(String startSymbol, int size) {
        Nonterminal start = new Nonterminal(startSymbol);
        Optional<String[]> stringsOpt = generateNonterminal(start, size);

        return stringsOpt.map(StringUtils::join);
    }

    public Optional<String[]> generateNonterminal(Nonterminal nonterminal, int size) {
        if (size <= 0) {
            return empty();
        }

        assert(cache.get(nonterminal.getName()).size() > 0);

        for (Rule rule : cache.get(nonterminal.getName())) {
            Optional<String[]> sentenceOpt = forRule(rule, size);

            if (sentenceOpt.isPresent()) {
                return sentenceOpt;
            }
        }

        return empty();
    }

    public Optional<String[]> forRule(Rule rule, int size) {
        Optional<String[]> sentenceOpt = forElement(rule.getElement(), size);

        if (rule.isLexical()) {
            return sentenceOpt.map(this::join);
        } else {
            return sentenceOpt.map(this::intersperse).map(this::lift);
        }
    }

    private String[] join(String[] strings) {
        return new String[]{
                StringUtils.join(strings)
        };
    }

    private String intersperse(String[] strings) {
        return String.join(" ", strings);
    }

    public Optional<String[]> forElement(ElementOpt element, int size) {
        if (size <= 0) {
            return empty();
        }

        if (element instanceof Empty) {
            return generateEmpty((Empty) element, size);
        } else if (element instanceof Conc) {
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
        } else if (element instanceof EOF) {
            return generateEof((EOF) element, size);
        }

        throw new IllegalStateException("Unknown element: " + element);
    }

    private Optional<String[]> generateEmpty(Empty element, int size) {
        return of(new String[]{});
    }

    private Optional<String[]> generateLiteral(Literal element, int size) {
        return of(lift(element.getText()));
    }

    private Optional<String[]> generateConc(Conc element, int size) {
        int headSize = (int) (1.0 / element.size() * size);
        int tailSize = size - headSize;

        Optional<String[]> headOpt = forElement(element.getFirst(), headSize);

        if (headOpt.isPresent()) {
            Optional<String[]> tailOpt = forElement(element.getSecond(), tailSize);

            if (tailOpt.isPresent()) {
                return of(concat(headOpt.get(), tailOpt.get()));
            }
        }

        return empty();
    }

    private Optional<String[]> generateAlt(Alt element, int size) {
        if (random.nextInt(element.size()) == 0) {
            Optional<String[]> firstResultOpt = forElement(element.getFirst(), size);

            if (!firstResultOpt.isPresent()) {
                return forElement(element.getSecond(), size);
            } else {
                return firstResultOpt;
            }
        } else {
            Optional<String[]> secondResultOpt = forElement(element.getSecond(), size);

            if (!secondResultOpt.isPresent()) {
                return forElement(element.getFirst(), size);
            } else {
                return secondResultOpt;
            }
        }
    }

    private Optional<String[]> generateOpt(Opt element, int size) {
        if (random.nextInt(2) == 0) {
            return of(lift(""));
        } else {
            return forElement(element.getElement(), size);
        }
    }

    private Optional<String[]> generateStar(Star element, int size) {
        if (random.nextInt(2) == 0) {
            return of(new String[]{""});
        } else {
            int headSize = size / 4;
            int tailSize = size - headSize;

            Optional<String[]> tailOpt = forElement(element, tailSize);

            return tailOpt.map(tail -> prepend(element.getElement(), tail, headSize));
        }
    }

    private String[] prepend(Element element, String[] tail, int size) {
        Optional<String[]> headOpt = forElement(element, size);

        if (headOpt.isPresent()) {
            return concat(headOpt.get(), tail);
        } else {
            return tail;
        }
    }

    private Optional<String[]> generatePlus(Plus element, int size) {
        int headSize = size / 4;
        int tailSize = size - headSize;

        Optional<String[]> tailOpt = generateStar(new Star(element.getElement()), tailSize);

        return tailOpt.map(tail -> prepend(element.getElement(), tail, headSize));
    }

    private Optional<String[]> generateCharacterClass(CharacterClass element, int size) {
        return generateRanges(element.getRanges(), size);
    }

    private Optional<String[]> generateEof(EOF element, int size) {
        return of(new String[]{});
    }

    private Optional<String[]> generateRanges(Ranges ranges, int size) {
        if (ranges instanceof RangesConc) {
            return generateRangesConc((RangesConc) ranges);
        } else if (ranges instanceof Range) {
            return generateRange((Range) ranges, size);
        }

        throw new IllegalStateException("Unknown ranges: " + ranges);
    }

    private Optional<String[]> generateRangesConc(RangesConc ranges) {
        int size = ranges.size();
        int rand = random.nextInt(size);

        return of(lift(String.valueOf(ranges.get(rand))));
    }

    private Optional<String[]> generateRange(Range range, int size) {
        if (range instanceof CharRange) {
            return generateCharRange((CharRange) range);
        } else if (range instanceof Char) {
            return generateChar((Char) range, size);
        }

        throw new IllegalStateException("Unknown range: " + range);
    }

    private Optional<String[]> generateCharRange(CharRange range) {
        int size = range.size();
        int rand = random.nextInt(size);

        return of(lift(String.valueOf(range.get(rand))));
    }

    private Optional<String[]> generateChar(Char c, int size) {
        if (c instanceof Single) {
            return generateSingle((Single) c, size);
        }

        throw new IllegalStateException("Unknown char: " + c);
    }

    private Optional<String[]> generateSingle(Single s, int size) {
        return of(lift(s.getText()));
    }

    private String[] lift(String... strings) {
        return strings;
    }

    private String[] concat(String[] s1, String[] s2) {
        return ArrayUtils.addAll(s1, s2);
    }

    private int size(String[] strings) {
        return Arrays
                .stream(strings)
                .mapToInt(String::length)
                .sum();
    }
}
