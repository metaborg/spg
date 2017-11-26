package org.metaborg.spg.sentence.antlr.generator;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Multimap;
import org.metaborg.spg.sentence.antlr.grammar.*;
import org.metaborg.spg.sentence.antlr.tree.Leaf;
import org.metaborg.spg.sentence.antlr.tree.Node;
import org.metaborg.spg.sentence.antlr.tree.Tree;

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

    public Optional<Tree> generate(String startSymbol, int size) {
        Nonterminal start = new Nonterminal(startSymbol);

        return generateNonterminal(start, size);
    }

    public Optional<Tree> generateNonterminal(Nonterminal nonterminal, int size) {
        if (size <= 0) {
            return empty();
        }

        assert(cache.get(nonterminal.getName()).size() > 0);

        for (Rule rule : cache.get(nonterminal.getName())) {
            Optional<Tree> treeOpt = forRule(rule, size);

            if (treeOpt.isPresent()) {
                return treeOpt;
            }
        }

        return empty();
    }

    public Optional<Tree> forRule(Rule rule, int size) {
        Optional<Tree> treeOpt = forElement(rule.getElement(), size);

        if (rule.isLexical()) {
            return treeOpt.map(this::join);
        } else {
            return treeOpt;
        }
    }

    public Optional<Tree> forElement(ElementOpt element, int size) {
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

    private Optional<Tree> generateEmpty(Empty element, int size) {
        return of(Leaf.EMPTY);
    }

    private Optional<Tree> generateLiteral(Literal element, int size) {
        return of(leaf(element.getText()));
    }

    private Optional<Tree> generateConc(Conc element, int size) {
        int headSize = (int) (1.0 / element.size() * size);
        int tailSize = size - headSize;

        Optional<Tree> headOpt = forElement(element.getFirst(), headSize);

        if (headOpt.isPresent()) {
            Optional<Tree> tailOpt = forElement(element.getSecond(), tailSize);

            if (tailOpt.isPresent()) {
                return of(node(element, headOpt.get(), tailOpt.get()));
            }
        }

        return empty();
    }

    private Optional<Tree> generateAlt(Alt element, int size) {
        if (random.nextInt(element.size()) == 0) {
            Optional<Tree> firstResultOpt = forElement(element.getFirst(), size);

            if (!firstResultOpt.isPresent()) {
                return forElement(element.getSecond(), size);
            } else {
                return firstResultOpt;
            }
        } else {
            Optional<Tree> secondResultOpt = forElement(element.getSecond(), size);

            if (!secondResultOpt.isPresent()) {
                return forElement(element.getFirst(), size);
            } else {
                return secondResultOpt;
            }
        }
    }

    private Optional<Tree> generateOpt(Opt element, int size) {
        if (random.nextInt(2) == 0) {
            return of(Leaf.EMPTY);
        } else {
            return forElement(element.getElement(), size);
        }
    }

    private Optional<Tree> generateStar(Star element, int size) {
        if (random.nextInt(2) == 0) {
            return of(Leaf.EMPTY);
        } else {
            int headSize = size / 4;
            int tailSize = size - headSize;

            Optional<Tree> tailOpt = forElement(element, tailSize);

            return tailOpt.map(tail -> prepend(element.getElement(), tail, headSize));
        }
    }

    private Optional<Tree> generatePlus(Plus element, int size) {
        int headSize = size / 4;
        int tailSize = size - headSize;

        Optional<Tree> tailOpt = generateStar(new Star(element.getElement()), tailSize);

        return tailOpt.map(tail -> prepend(element.getElement(), tail, headSize));
    }

    private Tree prepend(Element element, Tree tail, int size) {
        Optional<Tree> headOpt = forElement(element, size);

        if (headOpt.isPresent()) {
            return node(element, headOpt.get(), tail);
        } else {
            return tail;
        }
    }

    private Optional<Tree> generateCharacterClass(CharacterClass element, int size) {
        return generateRanges(element.getRanges(), size);
    }

    private Optional<Tree> generateEof(EOF element, int size) {
        return of(Leaf.EMPTY);
    }

    private Optional<Tree> generateRanges(Ranges ranges, int size) {
        if (ranges instanceof RangesConc) {
            return generateRangesConc((RangesConc) ranges);
        } else if (ranges instanceof Range) {
            return generateRange((Range) ranges, size);
        }

        throw new IllegalStateException("Unknown ranges: " + ranges);
    }

    private Optional<Tree> generateRangesConc(RangesConc ranges) {
        int size = ranges.size();
        int rand = random.nextInt(size);

        return of(leaf(String.valueOf(ranges.get(rand))));
    }

    private Optional<Tree> generateRange(Range range, int size) {
        if (range instanceof CharRange) {
            return generateCharRange((CharRange) range);
        } else if (range instanceof Char) {
            return generateChar((Char) range, size);
        }

        throw new IllegalStateException("Unknown range: " + range);
    }

    private Optional<Tree> generateCharRange(CharRange range) {
        int size = range.size();
        int rand = random.nextInt(size);

        return of(leaf(String.valueOf(range.get(rand))));
    }

    private Optional<Tree> generateChar(Char c, int size) {
        if (c instanceof Single) {
            return generateSingle((Single) c, size);
        }

        throw new IllegalStateException("Unknown char: " + c);
    }

    private Optional<Tree> generateSingle(Single s, int size) {
        return of(leaf(s.getText()));
    }

    private Tree join(Tree tree) {
        return leaf(tree.toString(false));
    }

    private Tree node(ElementOpt elementOpt, Tree... children) {
        return new Node(elementOpt, children);
    }

    private Tree leaf(String text) {
        return new Leaf(text);
    }
}
