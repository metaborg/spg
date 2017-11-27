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

import static java.util.stream.Stream.*;
import static org.metaborg.spg.sentence.antlr.functional.FlatMappingSpliterator.flatMap;
import static org.metaborg.spg.sentence.antlr.functional.Utils.lift;

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
        List<Term> subtrees = subterms(term)
                .filter(this::isLargeList)
                .collect(Collectors.toList());

        Collections.shuffle(subtrees, random);

        return flatMap(subtrees.stream(), subTree ->
                shrink(term, subTree)
        );
    }

    private Stream<Term> shrink(Term term, Term subTerm) {
        if (subTerm instanceof Text) {
            return of(term);
        } else if (subTerm instanceof Appl) {
            return shrinkRecursive(term, (Appl) subTerm);
        } else if (subTerm instanceof TermList) {
            return shrinkList(term, (TermList) subTerm);
        }

        // TODO: Do we still want/need shrinkAppl? shrinkAppl(term, (Appl) subTerm);

        throw new IllegalStateException("Unknown term: " + term);
    }

    private Stream<Term> shrinkRecursive(Term term, Appl appl) {
        if (appl.getElementOpt() instanceof Nonterminal) {
            Nonterminal nonterminal = (Nonterminal) appl.getElementOpt();
            List<Term> descendants = descendants(appl).collect(Collectors.toList());
            Set<Nonterminal> injections = grammar.getInjections(nonterminal);

            Collections.shuffle(descendants, random);

            return descendants.stream()
                    .filter(descendant -> isValidDescendant(descendant, injections))
                    .map(descendant -> replace(term, appl, descendant));
        }

        return empty();
    }

    private boolean isValidDescendant(Term descendant, Set<Nonterminal> injections) {
        if (descendant instanceof Appl) {
            ElementOpt elementOpt = ((Appl) descendant).getElementOpt();

            if (elementOpt instanceof Nonterminal) {
                Nonterminal nonterminal = (Nonterminal) elementOpt;

                return injections.contains(nonterminal);
            }
        }

        return false;
    }

    private Stream<Term> shrinkAppl(Term term, Appl appl) {
        int size = size(appl);
        int newSize = size - 1;

        Optional<Term> newSubTerm = generator.forElement(appl.getElementOpt(), newSize);

        return lift(newSubTerm.map(replacement ->
                replace(term, appl, replacement)
        ));
    }

    private Stream<Term> shrinkList(Term term, TermList list) {
        return combinations(list).map(newList ->
                replace(term, list, newList)
        );
    }

    private boolean isLargeList(Term term) {
        if (term instanceof TermList) {
            TermList list = (TermList) term;
            ElementOpt element = list.getElement();

            if (element instanceof Star && list.size() == 0) {
                return false;
            }

            if (element instanceof Plus && list.size() == 1) {
                return false;
            }
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

        return new TermList(list.getElement(), newChildren);
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

            return new Appl(appl.getElementOpt(), children);
        } else if (term instanceof TermList) {
            TermList list = (TermList) term;

            return new TermList(list.getElement(), children);
        }

        throw new IllegalArgumentException("Unexpected term (neither node nor leaf).");
    }

    private int size(Term subTerm) {
        return 1 + Arrays
                .stream(subTerm.getChildren())
                .mapToInt(this::size)
                .sum();
    }

    public Stream<Term> subterms(Term term) {
        if (term instanceof Text) {
            return empty();
        } else {
            Stream<Term> children = Arrays
                    .stream(term.getChildren())
                    .flatMap(this::subterms);

            return concat(of(term), children);
        }
    }

    public Stream<Term> descendants(Term term) {
        if (term instanceof Text) {
            return empty();
        } else {
            return Arrays
                    .stream(term.getChildren())
                    .flatMap(this::subterms);
        }
    }
}
