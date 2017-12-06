package org.metaborg.spg.sentence.antlr.shrinker;

import org.apache.commons.lang3.ArrayUtils;
import org.metaborg.spg.sentence.antlr.generator.Generator;
import org.metaborg.spg.sentence.antlr.grammar.*;
import org.metaborg.spg.sentence.antlr.term.Appl;
import org.metaborg.spg.sentence.antlr.term.Term;
import org.metaborg.spg.sentence.antlr.term.TermList;
import org.metaborg.spg.sentence.antlr.term.Text;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static java.util.stream.Stream.concat;
import static java.util.stream.Stream.empty;
import static java.util.stream.Stream.of;
import static org.metaborg.spg.sentence.shared.stream.FlatMappingSpliterator.flatMap;
import static org.metaborg.spg.sentence.shared.utils.StreamUtils.o2s;
import static org.metaborg.spg.sentence.shared.utils.StreamUtils.snoc;

public class Shrinker {
    private final Random random;
    private final Generator generator;
    private final Grammar grammar;

    public Shrinker(Random random, Generator generator, Grammar grammar) {
        this.random = random;
        this.generator = generator;
        this.grammar = grammar;
    }

    public Stream<Term> shrink(Term term) {
        List<Term> subtrees = subterms(term).collect(Collectors.toList());

        Collections.shuffle(subtrees, random);

        return flatMap(subtrees.stream(), subTree ->
                shrink(term, subTree)
        );
    }

    private Stream<Term> shrink(Term term, Term subTerm) {
        if (subTerm instanceof Text) {
            return of(term);
        } else if (subTerm instanceof Appl) {
            Appl appl = (Appl) subTerm;

            return concat(shrinkAppl(term, appl), shrinkRecursive(term, appl));
        } else if (subTerm instanceof TermList) {
            return shrinkList(term, (TermList) subTerm);
        }

        throw new IllegalStateException("Unknown term: " + term);
    }

    private Stream<Term> shrinkRecursive(Term term, Appl appl) {
        if (appl.getEmptyElement() instanceof Nonterminal) {
            Nonterminal nonterminal = (Nonterminal) appl.getEmptyElement();
            Stream<Term> descendants = descendants(appl);
            Set<Nonterminal> injections = grammar.getInjections(nonterminal);

            return descendants
                    .filter(descendant -> isValidDescendant(descendant, injections))
                    .map(descendant -> replace(term, appl, descendant));
        }

        return empty();
    }

    private boolean isValidDescendant(Term descendant, Set<Nonterminal> injections) {
        if (descendant instanceof Appl) {
            EmptyElement emptyElement = ((Appl) descendant).getEmptyElement();

            if (emptyElement instanceof Nonterminal) {
                Nonterminal nonterminal = (Nonterminal) emptyElement;

                return injections.contains(nonterminal);
            }
        }

        return false;
    }

    private Stream<Term> shrinkAppl(Term term, Appl appl) {
        int size = size(appl);
        int newSize = size - 1;

        Optional<Term> newSubTerm = generator.forElement(appl.getEmptyElement(), newSize);

        return o2s(newSubTerm.map(replacement ->
                replace(term, appl, replacement)
        ));
    }

    private Stream<Term> shrinkList(Term term, TermList list) {
        if (!isLargeList(list)) {
            return empty();
        }

        return combinations(list).map(newList ->
                replace(term, list, newList)
        );
    }

    private boolean isLargeList(TermList list) {
        EmptyElement emptyElement = list.getEmptyElement();

        if (emptyElement instanceof Star && list.size() == 0) {
            return false;
        }

        if (emptyElement instanceof Plus && list.size() == 1) {
            return false;
        }

        return true;
    }

    private Stream<Term> combinations(TermList list) {
        return IntStream.range(0, list.size()).mapToObj(exclude ->
                without(list, exclude)
        );
    }

    private TermList without(TermList list, int exclude) {
        Term[] oldChildren = list.getChildren();
        Term[] newChildren = ArrayUtils.remove(oldChildren, exclude);

        return new TermList(list.getEmptyElement(), newChildren);
    }

    private Term replace(Term term, Term subTree, Term replacement) {
        if (term == subTree) {
            return replacement;
        }

        if (term instanceof Text) {
            return term;
        }

        Term[] children = Arrays
                .stream(term.getChildren())
                .map(child -> replace(child, subTree, replacement))
                .toArray(Term[]::new);

        if (term instanceof Appl) {
            Appl appl = (Appl) term;

            return new Appl(appl.getEmptyElement(), children);
        } else if (term instanceof TermList) {
            TermList list = (TermList) term;

            return new TermList(list.getEmptyElement(), children);
        }

        throw new IllegalArgumentException("Unexpected term (neither node nor leaf).");
    }

    private int size(Term subTerm) {
        return 1 + Arrays
                .stream(subTerm.getChildren())
                .mapToInt(this::size)
                .sum();
    }

    private Stream<Term> subterms(Term term) {
        if (term instanceof Text) {
            return empty();
        } else {
            Stream<Term> children = Arrays.stream(term.getChildren());

            return snoc(flatMap(children, this::subterms), term);
        }
    }

    private Stream<Term> descendants(Term term) {
        Stream<Term> children = Arrays.stream(term.getChildren());

        return flatMap(children, this::subterms);
    }
}
