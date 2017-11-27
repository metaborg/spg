package org.metaborg.spg.sentence.antlr.generator;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Multimap;
import org.metaborg.spg.sentence.antlr.grammar.*;
import org.metaborg.spg.sentence.antlr.term.TermList;
import org.metaborg.spg.sentence.antlr.term.Text;
import org.metaborg.spg.sentence.antlr.term.Appl;
import org.metaborg.spg.sentence.antlr.term.Term;

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

    public Optional<Term> generate(String startSymbol, int size) {
        Nonterminal start = new Nonterminal(startSymbol);

        return generateNonterminal(start, size);
    }

    public Optional<Term> generateNonterminal(Nonterminal nonterminal, int size) {
        if (size <= 0) {
            return empty();
        }

        assert(cache.get(nonterminal.getName()).size() > 0);

        for (Rule rule : cache.get(nonterminal.getName())) {
            Optional<Term> treeOpt = forRule(rule, size);

            if (treeOpt.isPresent()) {
                return treeOpt;
            }
        }

        return empty();
    }

    public Optional<Term> forRule(Rule rule, int size) {
        Optional<Term> treeOpt = forElement(rule.getElement(), size);

        if (rule.isLexical()) {
            return treeOpt.map(this::join);
        } else {
            return treeOpt;
        }
    }

    public Optional<Term> forElement(ElementOpt element, int size) {
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

    private Optional<Term> generateEmpty(Empty element, int size) {
        return of(Text.EMPTY);
    }

    private Optional<Term> generateLiteral(Literal element, int size) {
        return of(leaf(element.getText()));
    }

    private Optional<Term> generateConc(Conc element, int size) {
        int headSize = (int) (1.0 / element.size() * size);
        int tailSize = size - headSize;

        Optional<Term> headOpt = forElement(element.getFirst(), headSize);

        if (headOpt.isPresent()) {
            Optional<Term> tailOpt = forElement(element.getSecond(), tailSize);

            if (tailOpt.isPresent()) {
                return of(node(element, headOpt.get(), tailOpt.get()));
            }
        }

        return empty();
    }

    private Optional<Term> generateAlt(Alt element, int size) {
        if (random.nextInt(element.size()) == 0) {
            Optional<Term> firstResultOpt = forElement(element.getFirst(), size);

            if (!firstResultOpt.isPresent()) {
                return forElement(element.getSecond(), size);
            } else {
                return firstResultOpt;
            }
        } else {
            Optional<Term> secondResultOpt = forElement(element.getSecond(), size);

            if (!secondResultOpt.isPresent()) {
                return forElement(element.getFirst(), size);
            } else {
                return secondResultOpt;
            }
        }
    }

    private Optional<Term> generateOpt(Opt element, int size) {
        if (random.nextInt(2) == 0) {
            return of(Text.EMPTY);
        } else {
            return forElement(element.getElement(), size);
        }
    }

    private Optional<Term> generateStar(Star element, int size) {
        if (random.nextInt(2) == 0) {
            return of(list(element));
        } else {
            int headSize = size / 4;
            int tailSize = size - headSize;

            Optional<Term> tailOpt = forElement(element, tailSize);

            return tailOpt.map(tail -> prepend(element, element.getElement(), tail, headSize));
        }
    }

    // TODO: This is wrong; it may return empty lists!
    private Optional<Term> generatePlus(Plus element, int size) {
        int headSize = size / 4;
        int tailSize = size - headSize;

        Optional<Term> tailOpt = generateStar(new Star(element.getElement()), tailSize);

        return tailOpt.map(tail -> prepend(element, element.getElement(), tail, headSize));
    }

    private Term prepend(Element operation, Element element, Term tail, int size) {
        Optional<Term> headOpt = forElement(element, size);

        if (headOpt.isPresent()) {
            TermList termList = (TermList) tail;

            return list(operation, headOpt.get(), termList);
        } else {
            return tail;
        }
    }

    private Optional<Term> generateCharacterClass(CharacterClass element, int size) {
        return generateRanges(element.getRanges(), size);
    }

    private Optional<Term> generateEof(EOF element, int size) {
        return of(Text.EMPTY);
    }

    private Optional<Term> generateRanges(Ranges ranges, int size) {
        if (ranges instanceof RangesConc) {
            return generateRangesConc((RangesConc) ranges);
        } else if (ranges instanceof Range) {
            return generateRange((Range) ranges, size);
        }

        throw new IllegalStateException("Unknown ranges: " + ranges);
    }

    private Optional<Term> generateRangesConc(RangesConc ranges) {
        int size = ranges.size();
        int rand = random.nextInt(size);

        return of(leaf(String.valueOf(ranges.get(rand))));
    }

    private Optional<Term> generateRange(Range range, int size) {
        if (range instanceof CharRange) {
            return generateCharRange((CharRange) range);
        } else if (range instanceof Char) {
            return generateChar((Char) range, size);
        }

        throw new IllegalStateException("Unknown range: " + range);
    }

    private Optional<Term> generateCharRange(CharRange range) {
        int size = range.size();
        int rand = random.nextInt(size);

        return of(leaf(String.valueOf(range.get(rand))));
    }

    private Optional<Term> generateChar(Char c, int size) {
        if (c instanceof Single) {
            return generateSingle((Single) c, size);
        }

        throw new IllegalStateException("Unknown char: " + c);
    }

    private Optional<Term> generateSingle(Single s, int size) {
        return of(leaf(s.getText()));
    }

    private Term join(Term term) {
        return leaf(term.toString(false));
    }

    private TermList list(Element element, Term term, Term[] tail) {
        return new TermList(element, term, tail);
    }

    private TermList list(Element element, Term term, TermList tail) {
        return new TermList(element, term, tail);
    }

    private TermList list(Element element, Term[] tail) {
        return new TermList(element, tail);
    }

    private TermList list(Star element) {
        return new TermList(element);
    }

    private Term node(ElementOpt elementOpt, Term... children) {
        return new Appl(elementOpt, children);
    }

    private Term leaf(String text) {
        return new Text(text);
    }
}
