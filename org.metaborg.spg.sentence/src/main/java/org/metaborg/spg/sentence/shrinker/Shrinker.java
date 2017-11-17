package org.metaborg.spg.sentence.shrinker;

import org.metaborg.sdf2table.grammar.*;
import org.metaborg.spg.sentence.IRandom;
import org.metaborg.spg.sentence.generator.Generator;
import org.metaborg.spg.sentence.parser.ParseService;
import org.spoofax.interpreter.terms.*;
import org.spoofax.jsglr.client.imploder.ImploderAttachment;
import org.spoofax.terms.attachments.OriginAttachment;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.stream.Stream.*;

public class Shrinker {
    private final IRandom random;
    private final ParseService parseService;
    private final Generator generator;
    private final ITermFactory termFactory;
    private final ShrinkerConfig shrinkerConfig;

    public Shrinker(IRandom random, ParseService parseService, Generator generator, ITermFactory termFactory, ShrinkerConfig shrinkerConfig) {
        this.random = random;
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
                .map(text -> parseService.parseUnit(shrinkerConfig.getLanguage(), text))
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
        List<IStrategoTerm> subTerms = subTerms(nonambiguousTerm).collect(Collectors.toList());

        return random.shuffle(subTerms).stream().flatMap(subTerm ->
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
        Symbol symbol = getRealSymbol(needle);
        Optional<IStrategoTerm> generatedSubTerm = generator.generateSymbol(symbol, size(needle) - 1);

        if (generatedSubTerm.isPresent()) {
            return of(replaceTerm(haystack, needle, generatedSubTerm.get()));
        }

        return empty();
    }

    /**
     * If the term is a Some/None term, then its sort is actually Optional.
     *
     * @param term
     * @return
     */
    protected Symbol getRealSymbol(IStrategoTerm term) {
        if (term instanceof IStrategoAppl) {
            Sort sort = getSort(term);
            IStrategoAppl appl = (IStrategoAppl) term;

            if ("None".equals(appl.getConstructor().getName()) || "Some".equals(appl.getConstructor().getName())) {
                return new ContextFreeSymbol(new OptionalSymbol(sort));
            }
        } else if (term instanceof IStrategoList) {
            IStrategoList list = (IStrategoList) term;
            Sort sort = getSort(list.head());

            return new ContextFreeSymbol(new IterSymbol(sort));
        }

        Sort sort = getSort(term);

        return new ContextFreeSymbol(sort);
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
            IStrategoAppl appl = (IStrategoAppl) haystack;

            return termFactory.replaceAppl(appl.getConstructor(), children, appl);
        } else if (haystack instanceof IStrategoList) {
            IStrategoList list = (IStrategoList) haystack;

            return termFactory.replaceList(children, list);
        } else if (haystack instanceof IStrategoString) {
            return haystack;
        }

        throw new IllegalArgumentException("Unable to replace in haystack: " + haystack);
    }

    /**
     * Get the sort of given term.
     *
     * @param term
     * @return
     */
    protected Sort getSort(IStrategoTerm term) {
        OriginAttachment originAttachment = term.getAttachment(OriginAttachment.TYPE);

        if (originAttachment != null) {
            return getSort(originAttachment.getOrigin());
        }

        ImploderAttachment attachment = term.getAttachment(ImploderAttachment.TYPE);

        String sort = attachment.getElementSort();

        return new Sort(sort);
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
            return concat(of(term), subTerms(term.getAllSubterms()));
        } else if (term instanceof IStrategoList) {
            IStrategoList list = (IStrategoList) term;

            if (list.isEmpty()) {
                return empty();
            } else {
                return concat(of(term), subTerms(term.getAllSubterms()));
            }
        }

        throw new IllegalStateException("Unknown term: " + term);
    }

    protected Stream<IStrategoTerm> subTerms(IStrategoTerm[] terms) {
        return Arrays
                .stream(terms)
                .flatMap(this::subTerms);
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
                IStrategoTerm alternative = alternatives.getSubterm(0);

                return disambiguate(alternative);
            } else {
                IStrategoTerm[] children = disambiguateChildren(appl);

                return termFactory.replaceAppl(appl.getConstructor(), children, appl);
            }
        } else if (term instanceof IStrategoList) {
            IStrategoList list = (IStrategoList) term;
            IStrategoTerm[] children = disambiguateChildren(list);

            if (isAmbiguousList(list)) {
                return flatten(termFactory.replaceList(children, list));
            } else {
                return termFactory.replaceList(children, list);
            }
        }

        return term;
    }

    private IStrategoTerm[] disambiguateChildren(IStrategoTerm term) {
        return Arrays
                .stream(term.getAllSubterms())
                .map(this::disambiguate)
                .toArray(IStrategoTerm[]::new);
    }

    /**
     * Check if the given list contains an amb-node.
     *
     * @param term
     * @return
     */
    private boolean isAmbiguousList(IStrategoTerm term) {
        return Arrays
                .stream(term.getAllSubterms())
                .anyMatch(this::isAmbiguousNode);
    }

    /**
     * Check if the given term is an amb-node.
     *
     * @param term
     * @return
     */
    private boolean isAmbiguousNode(IStrategoTerm term) {
        if (term instanceof IStrategoAppl) {
            IStrategoAppl appl = (IStrategoAppl) term;

            if ("amb".equals(appl.getConstructor().getName())) {
                return true;
            }
        }

        return false;
    }

    /**
     * Flatten a list of a list to a single list (one level).
     *
     * @param term
     * @return
     */
    private IStrategoTerm flatten(IStrategoTerm term) {
        if (term instanceof IStrategoList) {
            Stream<IStrategoTerm> children = Arrays.stream(term.getAllSubterms()).flatMap(subTerm -> {
                if (subTerm instanceof IStrategoList) {
                    return Arrays.stream(subTerm.getAllSubterms());
                } else {
                    return Stream.of(subTerm);
                }
            });

            return termFactory.makeList(children.collect(Collectors.toList()));
        } else {
            return term;
        }
    }
}
