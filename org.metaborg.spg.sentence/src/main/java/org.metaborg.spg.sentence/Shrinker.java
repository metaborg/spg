package org.metaborg.spg.sentence;

import com.google.common.collect.Iterables;
import org.metaborg.core.MetaborgException;
import org.metaborg.core.syntax.ParseException;
import org.metaborg.sdf2table.grammar.ContextFreeSymbol;
import org.metaborg.sdf2table.grammar.Sort;
import org.metaborg.sdf2table.grammar.Symbol;
import org.metaborg.spg.sentence.signature.Constructor;
import org.metaborg.spg.sentence.signature.Signature;
import org.spoofax.interpreter.terms.*;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class Shrinker {
    private final ParseService parseService;
    private final Generator generator;
    private final ITermFactory termFactory;
    private final ShrinkerInput shrinkerInput;

    public Shrinker(ParseService parseService, Generator generator, ITermFactory termFactory, ShrinkerInput shrinkerInput) {
        this.parseService = parseService;
        this.generator = generator;
        this.termFactory = termFactory;
        this.shrinkerInput = shrinkerInput;
    }

    /**
     * Repeatedly shrink the term until a local minimum is found.
     *
     * @param term
     * @return
     */
    public Optional<IStrategoTerm> shrinkStar(IStrategoTerm term) {
        return null;
    }

    /**
     * Given an ambiguous term, try to generate a smaller yet still ambiguous term.
     *
     * TODO: Make this lazy
     *
     * @param term
     * @return
     */
    public List<IStrategoTerm> shrink(IStrategoTerm term) {
        IStrategoTerm nonambiguousTerm = disambiguate(term);

        return shrinkTerm(nonambiguousTerm)
                .stream()
                .filter(uncheckException(this::isAmbiguous))
                .collect(Collectors.toList());
    }

    /**
     * Check if the given term is ambiguous.
     *
     * The term is first pretty-printed, then parsed, and the resulting AST is tested.
     *
     * @param term
     * @return
     */
    private boolean isAmbiguous(IStrategoTerm term) throws MetaborgException {
        String shrunkenText = shrinkerInput.getPrettyPrinter().prettyPrint(term);
        IStrategoTerm parsedTerm = parseService.parse(shrinkerInput.getLanguage(), shrunkenText);

        return parseService.isAmbiguous(parsedTerm);
    }

    /**
     * Given a non-ambiguous term, create an iterable of smaller terms.
     *
     * A random sub-term from the given term is selected and a strictly smaller term of the same sort
     * is generated. If no such term can be generated, this sub-term cannot be made any smaller.
     *
     * TODO: Make this lazy
     *
     * @param nonambiguousTerm
     * @return
     */
    private List<IStrategoTerm> shrinkTerm(IStrategoTerm nonambiguousTerm) {
        List<IStrategoTerm> subTerms = subTerms(nonambiguousTerm);

        return subTerms
                .stream()
                .flatMap(subTerm -> shrinkTerm(nonambiguousTerm, subTerm).stream())
                .collect(Collectors.toList());
    }

    /**
     * Replace needle inside haystack by a smaller term, if possible.
     *
     * @param haystack
     * @param needle
     * @return
     */
    private List<IStrategoTerm> shrinkTerm(IStrategoTerm haystack, IStrategoTerm needle) {
        int size = size(needle);
        Sort sort = recoverSort(haystack, needle);
        Symbol symbol = new ContextFreeSymbol(sort);

        Optional<IStrategoTerm> generatedSubTerm = generator.generate(symbol, size - 1);

        if (generatedSubTerm.isPresent()) {
            return Collections.singletonList(replaceTerm(haystack, needle, generatedSubTerm.get()));
        }

        return Collections.emptyList();
    }

    /**
     * Replace needle by replacement in haystack.
     *
     * @param haystack
     * @param needle
     * @param replacement
     * @return
     */
    protected IStrategoTerm replaceTerm(IStrategoTerm haystack, IStrategoTerm needle, IStrategoTerm replacement) {
        if (haystack == needle) {
            return replacement;
        }

        IStrategoTerm[] children = Arrays
                .stream(haystack.getAllSubterms())
                .map(visitee -> replaceTerm(visitee, needle, replacement))
                .toArray(IStrategoTerm[]::new);

        if (haystack instanceof IStrategoAppl) {
            return termFactory.makeAppl(((IStrategoAppl) haystack).getConstructor(), children, termFactory.makeList());
        } else if (haystack instanceof IStrategoList) {
            return termFactory.makeList(children, termFactory.makeList());
        } else if (haystack instanceof IStrategoString) {
            return haystack;
        }

        throw new IllegalArgumentException("Unable to replace in haystack: " + haystack);
    }

    /**
     * Find the sort of needle within haystack.
     *
     * @param haystack
     * @param needle
     * @return
     */
    private Sort recoverSort(IStrategoTerm haystack, IStrategoTerm needle) {
        Optional<Sort> sortOptional = recoverSort(haystack, needle, new Sort(shrinkerInput.getRootSort()));

        if (!sortOptional.isPresent()) {
            throw new IllegalStateException("Cannot recover the sort of needle, because it was not found in haystack.");
        }

        return sortOptional.get();
    }

    /**
     * Find the sort of needle within haystack. The sort parameter is the sort of haystack.
     *
     * @param haystack
     * @param needle
     * @param sort
     * @return
     */
    private Optional<Sort> recoverSort(IStrategoTerm haystack, IStrategoTerm needle, Sort sort) {
        if (haystack == needle) {
            return Optional.of(sort);
        } else {
            if (haystack instanceof IStrategoAppl) {
                IStrategoAppl nonambiguousAppl = (IStrategoAppl) haystack;
                IStrategoConstructor constructor = nonambiguousAppl.getConstructor();
                Constructor operation = shrinkerInput.getSignature().getOperation(constructor);

                for (int i = 0; i < nonambiguousAppl.getSubtermCount(); i++) {
                    IStrategoTerm subTerm = nonambiguousAppl.getSubterm(i);
                    Sort subSort = operation.getArgument(i);

                    Optional<Sort> sortOptional = recoverSort(subTerm, needle, subSort);

                    if (sortOptional.isPresent()) {
                        return sortOptional;
                    }
                }
            }
        }

        return Optional.empty();
    }

    /**
     * Compute the size of the given term.
     *
     * @param term
     * @return
     */
    private int size(IStrategoTerm term) {
        if (term instanceof IStrategoString) {
            return 1;
        } else if (term instanceof IStrategoAppl || term instanceof IStrategoList) {
            return 1 + Arrays.stream(term.getAllSubterms()).mapToInt(this::size).sum();
        }

        throw new IllegalStateException("Unknown term: " + term);
    }

    /**
     * Get all subterms of a term (the "powerset" of a term).
     *
     * @param term
     * @return
     */
    private List<IStrategoTerm> subTerms(IStrategoTerm term) {
        if (term instanceof IStrategoString) {
            return Collections.singletonList(term);
        } else if (term instanceof IStrategoAppl) {
            List<IStrategoTerm> terms = Arrays
                    .stream(term.getAllSubterms())
                    .flatMap(subTerm -> subTerms(subTerm).stream())
                    .collect(Collectors.toList());

            // TODO: is there a nicer way to "cons" a Java list?
            terms.add(0, term);

            return terms;
        } else if (term instanceof IStrategoList) {
            return Arrays
                    .stream(term.getAllSubterms())
                    .flatMap(subTerm -> subTerms(subTerm).stream())
                    .collect(Collectors.toList());
        }

        throw new IllegalStateException("Unknown term: " + term);
    }

    /**
     * Disambiguate the ambiguous tree by picking the first alternative.
     *
     * @param term
     * @return
     */
    public IStrategoTerm disambiguate(IStrategoTerm term) {
        if (term instanceof IStrategoAppl) {
            IStrategoAppl appl = (IStrategoAppl) term;

            if ("amb".equals(appl.getConstructor().getName())) {
                IStrategoTerm alternatives = appl.getSubterm(0);

                return disambiguate(alternatives.getSubterm(0));
            } else {
                IStrategoTerm[] children = Arrays
                        .stream(appl.getAllSubterms())
                        .map(this::disambiguate)
                        .toArray(IStrategoTerm[]::new);

                return termFactory.makeAppl(appl.getConstructor(), children, termFactory.makeList());
            }
        }

        return term;
    }

    @FunctionalInterface
    interface CheckedPredicate<T, E extends Exception> {
        boolean test(T element) throws E;
    }

    <T> Predicate<T> uncheckException(CheckedPredicate<T, Exception> function) {
        return element -> {
            try {
                return function.test(element);
            } catch (Exception ex) {
                // thanks to Christian Schneider for pointing out
                // that unchecked exceptions need not be wrapped again
                if (ex instanceof RuntimeException)
                    throw (RuntimeException) ex;
                else
                    throw new RuntimeException(ex);
            }
        };
    }
}
