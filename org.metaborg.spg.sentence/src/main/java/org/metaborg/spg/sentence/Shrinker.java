package org.metaborg.spg.sentence;

import org.metaborg.core.MetaborgException;
import org.metaborg.sdf2table.grammar.ContextFreeSymbol;
import org.metaborg.sdf2table.grammar.Sort;
import org.metaborg.sdf2table.grammar.Symbol;
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
        Stream<IStrategoTerm> shrunkTerms = shrink(term);

        return shrunkTerms
                .findAny()
                .map(this::shrinkStar)
                .orElse(Optional.of(term));
    }

    /**
     * Given an ambiguous term, try to generate a smaller yet still ambiguous term.
     *
     * @param term
     * @return
     */
    public Stream<IStrategoTerm> shrink(IStrategoTerm term) {
        IStrategoTerm nonambiguousTerm = disambiguate(term);

        return shrinkTerm(nonambiguousTerm).filter(Utils.uncheckPredicate(this::isAmbiguous));
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
     * @param nonambiguousTerm
     * @return
     */
    private Stream<IStrategoTerm> shrinkTerm(IStrategoTerm nonambiguousTerm) {
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
    private Stream<IStrategoTerm> shrinkTerm(IStrategoTerm haystack, IStrategoTerm needle) {
        int size = size(needle);
        Sort sort = recoverSort(haystack, needle);
        Symbol symbol = new ContextFreeSymbol(sort);

        Optional<IStrategoTerm> generatedSubTerm = generator.generate(symbol, size - 1);

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
    private Stream<IStrategoTerm> subTerms(IStrategoTerm term) {
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
}
