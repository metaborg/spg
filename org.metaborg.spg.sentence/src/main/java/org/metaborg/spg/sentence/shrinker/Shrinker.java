package org.metaborg.spg.sentence.shrinker;

import org.apache.commons.lang3.ArrayUtils;
import org.metaborg.sdf2table.grammar.*;
import org.metaborg.spg.sentence.generator.Generator;
import org.metaborg.spg.sentence.random.IRandom;
import org.metaborg.spg.sentence.signature.Signature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spoofax.interpreter.terms.*;
import org.spoofax.jsglr.client.imploder.ImploderAttachment;
import org.spoofax.terms.attachments.OriginAttachment;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static java.util.stream.Stream.*;
import static org.metaborg.spg.sentence.shared.stream.FlatMappingSpliterator.flatMap;
import static org.metaborg.spg.sentence.shared.utils.StreamUtils.*;

public class Shrinker {
    private static final Logger logger = LoggerFactory.getLogger(Shrinker.class);

    private final IRandom random;
    private final ITermFactory termFactory;
    private final Generator generator;
    private final Signature signature;

    public Shrinker(IRandom random, ITermFactory termFactory, Generator generator, Signature signature) {
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

        Symbol symbol = getRealSymbol(needle);

        Optional<IStrategoTerm> generatedTermOpt = generator
                .generateSymbol(symbol, size(needle) - 1);

        Optional<IStrategoTerm> replacedTermOpt = generatedTermOpt
                .map(term -> replaceTerm(haystack, needle, term));

        return o2s(replacedTermOpt);
    }

    // TODO: Check if empty list is allowed (list is of sort IterStar), and shrink further.
      // TODO: For this we need the generator to attach the sort to the term.
        // TODO: And use the generated term instead of the parsed term.
          // TODO: And no longer use getSort
            // TODO: And no longer implode & check the CST for ambiguities instead of the AST
    private Stream<IStrategoTerm> shrinkList(IStrategoTerm haystack, IStrategoList list) {
        logger.trace("Shrink list: " + list);

        if (list.size() < 2) {
            return empty();
        }

        return combinations(list).map(shrunkList ->
                replaceTerm(haystack, list, shrunkList)
        );
    }

    private Stream<IStrategoTerm> combinations(IStrategoList list) {
        return IntStream.range(0, list.size()).mapToObj(exclude ->
                without(list, exclude)
        );
    }

    private IStrategoTerm without(IStrategoList list, int exclude) {
        IStrategoTerm[] oldChildren = list.getAllSubterms();
        IStrategoTerm[] newChildren = ArrayUtils.remove(oldChildren, exclude);

        return termFactory.makeList(newChildren);
    }

    private Stream<IStrategoTerm> shrinkRecursive(IStrategoTerm haystack, IStrategoAppl appl) {
        logger.trace("Shrink recursive: " + appl);

        org.metaborg.spg.sentence.signature.Sort sort = signature.getSort(haystack, appl);
        Set<org.metaborg.spg.sentence.signature.Sort> injections = signature.getInjections(sort);
        Stream<IStrategoTerm> descendants = descendants(appl);

        return zipWith(descendants, descendant -> signature.getSort(appl, descendant))
                .filter(pair -> injections.contains(pair.getValue()))
                .map(pair -> replaceTerm(haystack, appl, pair.getKey()));
    }

    private Symbol getRealSymbol(IStrategoTerm term) {
        Sort sort = getSort(term);

        if (term instanceof IStrategoAppl) {
            IStrategoAppl appl = (IStrategoAppl) term;

            if ("None".equals(appl.getConstructor().getName()) || "Some".equals(appl.getConstructor().getName())) {
                return new ContextFreeSymbol(new OptionalSymbol(sort));
            }
        } else if (term instanceof IStrategoList) {
            // TODO: This does not correctly identify Iter and IterStar (it just defaults to Iter)
            return new ContextFreeSymbol(new IterSymbol(sort));
        }

        return new ContextFreeSymbol(sort);
    }

    private Sort getSort(IStrategoTerm term) {
        OriginAttachment originAttachment = term.getAttachment(OriginAttachment.TYPE);

        if (originAttachment != null) {
            return getSort(originAttachment.getOrigin());
        }

        ImploderAttachment attachment = term.getAttachment(ImploderAttachment.TYPE);
        String sort = attachment.getElementSort();

        return new Sort(sort);
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
            return snoc(subTerms(term.getAllSubterms()), term);
        }

        throw new IllegalStateException("Unknown term: " + term);
    }

    private Stream<IStrategoTerm> subTerms(IStrategoTerm[] terms) {
        return flatMap(Arrays.stream(terms), this::subTerms);
    }
}
