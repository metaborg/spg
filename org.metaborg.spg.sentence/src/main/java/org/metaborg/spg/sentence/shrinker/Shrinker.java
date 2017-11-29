package org.metaborg.spg.sentence.shrinker;

import org.apache.commons.lang3.ArrayUtils;
import org.metaborg.sdf2table.grammar.*;
import org.metaborg.spg.sentence.IRandom;
import org.metaborg.spg.sentence.generator.Generator;
import org.spoofax.interpreter.terms.*;
import org.spoofax.jsglr.client.imploder.ImploderAttachment;
import org.spoofax.terms.attachments.OriginAttachment;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static java.util.stream.Stream.concat;
import static java.util.stream.Stream.of;

public class Shrinker {
    private final IRandom random;
    private final Generator generator;
    private final ITermFactory termFactory;

    public Shrinker(IRandom random, Generator generator, ITermFactory termFactory) {
        this.random = random;
        this.generator = generator;
        this.termFactory = termFactory;
    }

    public Stream<IStrategoTerm> shrink(IStrategoTerm term) {
        List<IStrategoTerm> subTerms = subTerms(term).collect(Collectors.toList());

        return random.shuffle(subTerms).stream().flatMap(subTerm ->
                shrink(term, subTerm)
        );
    }

    protected Stream<IStrategoTerm> shrink(IStrategoTerm haystack, IStrategoTerm needle) {
        Symbol symbol = getRealSymbol(needle);

        if (needle instanceof IStrategoList) {
            return shrinkList(haystack, (IStrategoList) needle);
        }

        // TODO: This is naive; we can shrink recursive patterns (replace ancestor by descendant). But we need the signatures for this.
        Optional<IStrategoTerm> generatedSubTerm = generator.generateSymbol(symbol, size(needle) - 1);

        return generatedSubTerm
                .map(term -> of(replaceTerm(haystack, needle, term)))
                .orElseGet(Stream::empty);
    }

    // TODO: Depending on the kind of list (Iter or IterStar), we may not be allowed to shrink this list! We need the productions for this?
    private Stream<IStrategoTerm> shrinkList(IStrategoTerm haystack, IStrategoList list) {
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

    protected Symbol getRealSymbol(IStrategoTerm term) {
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

    protected Sort getSort(IStrategoTerm term) {
        OriginAttachment originAttachment = term.getAttachment(OriginAttachment.TYPE);

        if (originAttachment != null) {
            return getSort(originAttachment.getOrigin());
        }

        ImploderAttachment attachment = term.getAttachment(ImploderAttachment.TYPE);
        String sort = attachment.getElementSort();

        return new Sort(sort);
    }

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

    protected int size(IStrategoTerm term) {
        if (term instanceof IStrategoString) {
            return 1;
        } else if (term instanceof IStrategoAppl || term instanceof IStrategoList) {
            return 1 + Arrays.stream(term.getAllSubterms()).mapToInt(this::size).sum();
        }

        throw new IllegalStateException("Unknown term: " + term);
    }

    protected Stream<IStrategoTerm> subTerms(IStrategoTerm term) {
        if (term instanceof IStrategoString) {
            return of(term);
        } else if (term instanceof IStrategoAppl) {
            return concat(of(term), subTerms(term.getAllSubterms()));
        } else if (term instanceof IStrategoList) {
            return concat(of(term), subTerms(term.getAllSubterms()));
        }

        throw new IllegalStateException("Unknown term: " + term);
    }

    protected Stream<IStrategoTerm> subTerms(IStrategoTerm[] terms) {
        return Arrays
                .stream(terms)
                .flatMap(this::subTerms);
    }
}
