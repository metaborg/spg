package org.metaborg.spg.sentence.shrinker;

import org.metaborg.sdf2table.grammar.ContextFreeSymbol;
import org.metaborg.sdf2table.grammar.Sort;
import org.metaborg.sdf2table.grammar.Symbol;
import org.metaborg.spg.sentence.ParseService;
import org.metaborg.spg.sentence.generator.Generator;
import org.metaborg.spg.sentence.signature.Constructor;
import org.spoofax.interpreter.terms.*;

import java.util.Arrays;
import java.util.Optional;
import java.util.stream.Stream;

import static java.util.stream.Stream.*;

public class Shrinker {
    private final ParseService parseService;
    private final Generator generator;
    private final ITermFactory termFactory;
    private final ShrinkerConfig shrinkerConfig;

    public Shrinker(ParseService parseService, Generator generator, ITermFactory termFactory, ShrinkerConfig shrinkerConfig) {
        this.parseService = parseService;
        this.generator = generator;
        this.termFactory = termFactory;
        this.shrinkerConfig = shrinkerConfig;
    }

    /**
     * Repeatedly shrink the term until a local minimum is found.
     *
     * @param shrinkerUnit
     * @return
     */
    public Optional<ShrinkerUnit> shrinkStar(ShrinkerUnit shrinkerUnit) {
        Stream<ShrinkerUnit> shrunkTerms = shrink(shrinkerUnit);

        return shrunkTerms
                .findAny()
                .map(this::shrinkStar)
                .orElse(Optional.of(shrinkerUnit));
    }

    /**
     * Given an ambiguous parse unit, try to generate a smaller yet ambiguous term.
     *
     * @param shrinkerUnit
     * @return
     */
    public Stream<ShrinkerUnit> shrink(ShrinkerUnit shrinkerUnit) {
        IStrategoTerm nonambiguousTerm = disambiguate(shrinkerUnit.getTerm());

        return shrink(nonambiguousTerm)
                .map(term -> shrinkerConfig.getPrinter().print(term))
                .map(term -> parseService.parseUnit(shrinkerConfig.getLanguage(), term))
                .map(ShrinkerUnit::new)
                .filter(shrunkenUnit -> parseService.isAmbiguous(shrunkenUnit.getTerm()));
    }

    /**
     * Given a non-ambiguous term, create an iterable of smaller terms.
     *
     * A random sub-term from the given term is selected and a strictly smaller term of the same sort
     * is generated. If no such term can be generated, this sub-term cannot be made any smaller.
     *
     * @param nonambiguousTerm
     * @return
     */
    public Stream<IStrategoTerm> shrink(IStrategoTerm nonambiguousTerm) {
        Stream<IStrategoTerm> subTerms = subTerms(nonambiguousTerm);

        return subTerms.flatMap(subTerm ->
                shrinkTerm(nonambiguousTerm, subTerm)
        );
    }

    /**
     * Replace needle inside haystack by a smaller term, if possible.
     *
     * @param haystack
     * @param needle
     * @return
     */
    protected Stream<IStrategoTerm> shrinkTerm(IStrategoTerm haystack, IStrategoTerm needle) {
        int size = size(needle);
        Sort sort = recoverSort(haystack, needle);
        Symbol symbol = new ContextFreeSymbol(sort);

        Optional<IStrategoTerm> generatedSubTerm = generator.generateTerm(symbol, size - 1);

        if (generatedSubTerm.isPresent()) {
            return of(replaceTerm(haystack, needle, generatedSubTerm.get()));
        }

        return empty();
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
    protected Sort recoverSort(IStrategoTerm haystack, IStrategoTerm needle) {
        Optional<Sort> sortOptional = recoverSort(haystack, needle, new Sort(shrinkerConfig.getRootSort()));

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
    protected Optional<Sort> recoverSort(IStrategoTerm haystack, IStrategoTerm needle, Sort sort) {
        if (haystack == needle) {
            return Optional.of(sort);
        } else {
            if (haystack instanceof IStrategoAppl) {
                IStrategoAppl nonambiguousAppl = (IStrategoAppl) haystack;
                IStrategoConstructor constructor = nonambiguousAppl.getConstructor();
                Constructor operation = shrinkerConfig.getSignature().getOperation(constructor);

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
    protected int size(IStrategoTerm term) {
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
    protected Stream<IStrategoTerm> subTerms(IStrategoTerm term) {
        if (term instanceof IStrategoString) {
            return of(term);
        } else if (term instanceof IStrategoAppl) {
            Stream<IStrategoTerm> terms = Arrays
                    .stream(term.getAllSubterms())
                    .flatMap(this::subTerms);

            return concat(of(term), terms);
        } else if (term instanceof IStrategoList) {
            return Arrays
                    .stream(term.getAllSubterms())
                    .flatMap(this::subTerms);
        }

        throw new IllegalStateException("Unknown term: " + term);
    }

    /**
     * Disambiguate the ambiguous tree by picking the first alternative.
     *
     * @param term
     * @return
     */
    protected IStrategoTerm disambiguate(IStrategoTerm term) {
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
}
