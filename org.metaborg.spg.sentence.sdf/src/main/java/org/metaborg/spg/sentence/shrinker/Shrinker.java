package org.metaborg.spg.sentence.shrinker;

import static java.util.stream.Stream.concat;
import static java.util.stream.Stream.empty;
import static java.util.stream.Stream.of;
import static org.metaborg.spg.sentence.shared.stream.FlatMappingSpliterator.flatMap;
import static org.metaborg.spg.sentence.shared.utils.StreamUtils.cons;
import static org.metaborg.spg.sentence.shared.utils.StreamUtils.o2s;
import static org.metaborg.spg.sentence.shared.utils.StreamUtils.zipWith;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.apache.commons.lang3.ArrayUtils;
import org.metaborg.parsetable.grammar.ISymbol;
import org.metaborg.sdf2table.grammar.ContextFreeSymbol;
import org.metaborg.sdf2table.grammar.IterStarSymbol;
import org.metaborg.sdf2table.grammar.IterSymbol;
import org.metaborg.sdf2table.grammar.LexicalSymbol;
import org.metaborg.sdf2table.grammar.OptionalSymbol;
import org.metaborg.sdf2table.grammar.Symbol;
import org.metaborg.spg.sentence.generator.Generator;
import org.metaborg.spg.sentence.generator.GeneratorAttachment;
import org.metaborg.spg.sentence.random.IRandom;
import org.metaborg.spg.sentence.signature.Signature;
import org.metaborg.spg.sentence.signature.Sort;
import org.metaborg.spg.sentence.terms.GeneratorTermFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spoofax.interpreter.terms.IStrategoAppl;
import org.spoofax.interpreter.terms.IStrategoList;
import org.spoofax.interpreter.terms.IStrategoString;
import org.spoofax.interpreter.terms.IStrategoTerm;

public class Shrinker {
    private static final Logger logger = LoggerFactory.getLogger(Shrinker.class);

    private final IRandom random;
    private final GeneratorTermFactory termFactory;
    private final Generator generator;
    private final Signature signature;

    public Shrinker(IRandom random, GeneratorTermFactory termFactory, Generator generator, Signature signature) {
        this.random = random;
        this.termFactory = termFactory;
        this.generator = generator;
        this.signature = signature;
    }

    public Stream<IStrategoTerm> shrink(IStrategoTerm term) {
        List<IStrategoTerm> subTerms = subTerms(term).collect(Collectors.toList());

        return random.shuffle(subTerms).stream().flatMap(subTerm ->
                shrink(term, subTerm)
        );
    }

    private Stream<IStrategoTerm> shrink(IStrategoTerm haystack, IStrategoTerm needle) {
        logger.trace("Shrink term: " + needle);

        if (needle instanceof IStrategoList) {
            return shrinkList(haystack, (IStrategoList) needle);
        } else if (needle instanceof IStrategoAppl) {
            return concat(shrinkGenerate(haystack, needle), shrinkRecursive(haystack, (IStrategoAppl) needle));
        } else {
            return shrinkGenerate(haystack, needle);
        }
    }

    private Stream<IStrategoTerm> shrinkGenerate(IStrategoTerm haystack, IStrategoTerm needle) {
        logger.trace("Shrink generate: " + needle);

        ISymbol symbol = getSymbol(needle);

        Optional<IStrategoTerm> generatedTermOpt = generator
                .generateSymbol(symbol, size(needle) - 1);

        Optional<IStrategoTerm> replacedTermOpt = generatedTermOpt
                .map(term -> replaceTerm(haystack, needle, term));

        return o2s(replacedTermOpt);
    }

    private Stream<IStrategoTerm> shrinkList(IStrategoTerm haystack, IStrategoList list) {
        logger.trace("Shrink list: " + list);

        if (!isEmptyAllowed(list) && list.size() < 2) {
            return empty();
        }

        return combinations(list).map(shrunkList ->
                replaceTerm(haystack, list, shrunkList)
        );
    }

    private boolean isEmptyAllowed(IStrategoList list) {
        ISymbol symbol = getSymbol(list);

        if (symbol instanceof IterSymbol) {
            return false;
        } else if (symbol instanceof IterStarSymbol) {
            return true;
        }

        throw new IllegalArgumentException("Cannot compute if empty is allowed for list: " + list);
    }

    private Stream<IStrategoTerm> combinations(IStrategoList list) {
        return IntStream.range(0, list.size()).mapToObj(exclude ->
                without(list, exclude)
        );
    }

    private IStrategoTerm without(IStrategoList list, int exclude) {
        IStrategoTerm[] oldChildren = list.getAllSubterms();
        IStrategoTerm[] newChildren = ArrayUtils.remove(oldChildren, exclude);

        return termFactory.replaceList(newChildren, list);
    }

    private Stream<IStrategoTerm> shrinkRecursive(IStrategoTerm haystack, IStrategoAppl appl) {
        logger.trace("Shrink recursive: " + appl);

        org.metaborg.spg.sentence.signature.Sort sort = getSort(appl);
        Set<org.metaborg.spg.sentence.signature.Sort> injections = signature.getInjections(sort);
        Stream<IStrategoTerm> descendants = descendants(appl);

        return zipWith(descendants, this::getSort)
                .filter(pair -> injections.contains(pair.getValue()))
                .map(pair -> replaceTerm(haystack, appl, pair.getKey()));
    }

    private Sort getSort(IStrategoTerm term) {
        return getSort(getSymbol(term));
    }

    private Sort getSort(ISymbol symbol) {
        if (symbol instanceof org.metaborg.sdf2table.grammar.Sort) {
            org.metaborg.sdf2table.grammar.Sort sort = (org.metaborg.sdf2table.grammar.Sort) symbol;

            return new Sort(sort.name());
        } else if (symbol instanceof IterStarSymbol) {
            IterStarSymbol iterStarSymbol = (IterStarSymbol) symbol;

            return new Sort("IterStar", getSort(iterStarSymbol.getSymbol()));
        } else if (symbol instanceof IterSymbol) {
            IterSymbol iterSymbol = (IterSymbol) symbol;

            return new Sort("Iter", getSort(iterSymbol.getSymbol()));
        } else if (symbol instanceof OptionalSymbol) {
            OptionalSymbol optionalSymbol = (OptionalSymbol) symbol;

            return new Sort("Option", getSort(optionalSymbol.getSymbol()));
        } else if (symbol instanceof ContextFreeSymbol) {
            ContextFreeSymbol contextFreeSymbol = (ContextFreeSymbol) symbol;

            return getSort(contextFreeSymbol.getSymbol());
        } else if (symbol instanceof LexicalSymbol) {
            return new Sort("String");
        }

        throw new IllegalArgumentException("Unable to convert symbol " + symbol + " to a sort.");
    }

    private ISymbol getSymbol(IStrategoTerm term) {
        GeneratorAttachment generatorAttachment = term.getAttachment(GeneratorAttachment.TYPE);

        return generatorAttachment.getSymbol();
    }

    private IStrategoTerm replaceTerm(IStrategoTerm haystack, IStrategoTerm needle, IStrategoTerm replacement) {
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

    private int size(IStrategoTerm term) {
        if (term instanceof IStrategoString) {
            return 1;
        } else if (term instanceof IStrategoAppl || term instanceof IStrategoList) {
            return 1 + Arrays.stream(term.getAllSubterms()).mapToInt(this::size).sum();
        }

        throw new IllegalStateException("Unknown term: " + term);
    }

    private Stream<IStrategoTerm> descendants(IStrategoTerm term) {
        Stream<IStrategoTerm> children = Arrays.stream(term.getAllSubterms());

        return flatMap(children, this::subTerms);
    }

    private Stream<IStrategoTerm> subTerms(IStrategoTerm term) {
        if (term instanceof IStrategoString) {
            return of(term);
        } else if (term instanceof IStrategoAppl || term instanceof IStrategoList) {
            return cons(term, subTerms(term.getAllSubterms()));
        }

        throw new IllegalStateException("Unknown term: " + term);
    }

    private Stream<IStrategoTerm> subTerms(IStrategoTerm[] terms) {
        return flatMap(Arrays.stream(terms), this::subTerms);
    }
}
