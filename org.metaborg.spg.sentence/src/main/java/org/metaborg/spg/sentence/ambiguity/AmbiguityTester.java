package org.metaborg.spg.sentence.ambiguity;

import com.google.inject.Inject;
import org.metaborg.core.language.ILanguageImpl;
import org.metaborg.spg.sentence.generator.Generator;
import org.metaborg.spg.sentence.parser.ParseRuntimeException;
import org.metaborg.spg.sentence.parser.ParseService;
import org.metaborg.spg.sentence.printer.Printer;
import org.metaborg.spg.sentence.printer.PrinterRuntimeException;
import org.metaborg.spg.sentence.shrinker.Shrinker;
import org.spoofax.interpreter.terms.IStrategoAppl;
import org.spoofax.interpreter.terms.IStrategoList;
import org.spoofax.interpreter.terms.IStrategoTerm;
import org.spoofax.interpreter.terms.ITermFactory;

import java.util.Arrays;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class AmbiguityTester {
    private final ParseService parseService;
    private final ITermFactory termFactory;
    private final ILanguageImpl languageImpl;
    private final Printer printer;
    private final Generator generator;
    private final Shrinker shrinker;

    @Inject
    public AmbiguityTester(
            ParseService parseService,
            ITermFactory termFactory,
            ILanguageImpl languageImpl,
            Printer printer,
            Generator generator,
            Shrinker shrinker
    ) {
        this.parseService = parseService;
        this.termFactory = termFactory;
        this.languageImpl = languageImpl;
        this.printer = printer;
        this.generator = generator;
        this.shrinker = shrinker;
    }

    public AmbiguityTesterResult findAmbiguity(AmbiguityTesterConfig config, AmbiguityTesterProgress progress) throws Exception {
        long startTime = System.currentTimeMillis();

        for (int i = 0; i < config.getMaxNumberOfTerms(); i++) {
            try {
                Optional<IStrategoTerm> termOpt = generator.generate(config.getMaxTermSize());

                if (termOpt.isPresent()) {
                    IStrategoTerm term = termOpt.get();
                    String text = printer.print(term);

                    progress.sentenceGenerated(text);

                    IStrategoTerm parsedTerm = parseService.parse(languageImpl, text);

                    if (isAmbiguous(parsedTerm)) {
                        long duration = System.currentTimeMillis() - startTime;

                        shrink(parsedTerm, progress);

                        return new AmbiguityTesterResult(i, duration, text);
                    }
                }
            } catch (AmbiguityTesterCancelledException e) {
                long duration = System.currentTimeMillis() - startTime;

                return new AmbiguityTesterResult(i, duration);
            } catch (ParseRuntimeException | PrinterRuntimeException e) {
                e.printStackTrace();
            }
        }

        long duration = System.currentTimeMillis() - startTime;

        return new AmbiguityTesterResult(config.getMaxNumberOfTerms(), duration);
    }

    protected IStrategoTerm shrink(IStrategoTerm term, AmbiguityTesterProgress progress) {
        progress.sentenceShrinked(printer.print(term));

        Optional<IStrategoTerm> shrunkOpt = shrink(term).findAny();

        if (!shrunkOpt.isPresent()) {
            return term;
        } else {
            return shrink(shrunkOpt.get(), progress);
        }
    }

    public Stream<IStrategoTerm> shrink(IStrategoTerm term) {
        IStrategoTerm nonambiguousTerm = disambiguate(term);

        return shrinker
                .shrink(nonambiguousTerm)
                .map(printer::print)
                .map(this::parse)
                .filter(this::isAmbiguous);
    }

    protected IStrategoTerm parse(String text) {
        return parseService.parse(languageImpl, text);
    }

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

    private IStrategoTerm flatten(IStrategoTerm term) {
        if (term instanceof IStrategoList) {
            Stream<IStrategoTerm> oldChildren = Arrays.stream(term.getAllSubterms());
            Stream<IStrategoTerm> newChildren = oldChildren.flatMap(this::flattenOne);

            return termFactory.makeList(newChildren.collect(Collectors.toList()));
        } else {
            return term;
        }
    }

    private Stream<IStrategoTerm> flattenOne(IStrategoTerm term) {
        if (term instanceof IStrategoList) {
            return Arrays.stream(term.getAllSubterms());
        } else {
            return Stream.of(term);
        }
    }

    private boolean isAmbiguous(IStrategoTerm term) {
        if (isAmbNode(term)) {
            return true;
        }

        for (IStrategoTerm subterm : term.getAllSubterms()) {
            if (isAmbiguous(subterm)) {
                return true;
            }
        }

        return false;
    }

    private boolean isAmbiguousList(IStrategoTerm term) {
        for (IStrategoTerm subTerm : term.getAllSubterms()) {
            if (isAmbNode(subTerm)) {
                return true;
            }
        }

        return false;
    }

    private boolean isAmbNode(IStrategoTerm term) {
        if (term instanceof IStrategoAppl) {
            IStrategoAppl appl = (IStrategoAppl) term;

            if ("amb".equals(appl.getConstructor().getName())) {
                return true;
            }
        }

        return false;
    }
}
