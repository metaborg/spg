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
        Term flatTerm = term; // flattenInjections(term);

        List<Term> subtrees = subterms(flatTerm)
                .filter(this::isLargeList)
                .collect(Collectors.toList());

        Collections.shuffle(subtrees, random);

        return flatMap(subtrees.stream(), subTree ->
                shrink(flatTerm, subTree)
        );
    }

    // TODO: Can we prevent creating injections in the parse tree to begin with?
    private Term flattenInjections(Term term) {
        if (isNonterminal(term)) {
            Term[] children = term.getChildren();

            if (children.length == 1 && isNonterminal(children[0])) {
                return flattenInjections(children[0]);
            }
        }

        // TODO: replaceChildren method on Term?
        if (term instanceof Appl) {
            Appl appl = (Appl) term;
            EmptyElement emptyElement = appl.getEmptyElement();
            Term[] children = Arrays
                    .stream(appl.getChildren())
                    .map(this::flattenInjections)
                    .toArray(Term[]::new);

            return new Appl(emptyElement, children);
        } else if (term instanceof TermList) {
            TermList list = (TermList) term;
            EmptyElement emptyElement = list.getEmptyElement();
            Term[] children = Arrays
                    .stream(list.getChildren())
                    .map(this::flattenInjections)
                    .toArray(Term[]::new);

            return new TermList(emptyElement, children);
        } else {
            return term;
        }
    }

    private boolean isNonterminal(Term term) {
        if (term instanceof Appl) {
            Appl appl = (Appl) term;

            if (appl.getEmptyElement() instanceof Nonterminal) {
                return true;
            }
        }

        return false;
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
        // TODO: An advantage of shrinkAppl is that it potentially shrinks a lot more than shrinkRecursive.
        // TODO: Since parsing is the bottleneck, we want to shrink large parts soon.
        // TODO: We could, of course, have shrinkRecursive pull up lowest trees first?

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
        return combinations(list).map(newList ->
                replace(term, list, newList)
        );
    }

    private boolean isLargeList(Term term) {
        if (term instanceof TermList) {
            TermList list = (TermList) term;
            EmptyElement emptyElement = list.getEmptyElement();

            if (emptyElement instanceof Star && list.size() == 0) {
                return false;
            }

            if (emptyElement instanceof Plus && list.size() == 1) {
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
