package org.metaborg.spg.sentence.ambiguity;

import com.google.inject.Inject;
import org.metaborg.core.language.ILanguageImpl;
import org.metaborg.core.syntax.ParseException;
import org.metaborg.spg.sentence.ambiguity.result.FindResult;
import org.metaborg.spg.sentence.ambiguity.result.ShrinkResult;
import org.metaborg.spg.sentence.ambiguity.result.TestResult;
import org.metaborg.spg.sentence.generator.Generator;
import org.metaborg.spg.sentence.printer.Printer;
import org.metaborg.spg.sentence.printer.PrinterRuntimeException;
import org.metaborg.spg.sentence.shrinker.Shrinker;
import org.metaborg.spoofax.core.syntax.ISpoofaxSyntaxService;
import org.metaborg.spoofax.core.unit.ISpoofaxInputUnit;
import org.metaborg.spoofax.core.unit.ISpoofaxParseUnit;
import org.metaborg.spoofax.core.unit.ISpoofaxUnitService;
import org.metaborg.util.time.Timer;
import org.spoofax.interpreter.terms.IStrategoAppl;
import org.spoofax.interpreter.terms.IStrategoList;
import org.spoofax.interpreter.terms.IStrategoTerm;
import org.spoofax.interpreter.terms.ITermFactory;

import java.util.Arrays;
import java.util.Optional;
import java.util.stream.Stream;

import static java.util.stream.Stream.empty;
import static java.util.stream.Stream.of;

public class Tester {
    private final ITermFactory termFactory;
    private final ISpoofaxUnitService unitService;
    private final ISpoofaxSyntaxService syntaxService;
    private final ILanguageImpl languageImpl;
    private final Printer printer;
    private final Generator generator;
    private final Shrinker shrinker;

    @Inject
    public Tester(
            ITermFactory termFactory,
            ISpoofaxUnitService unitService,
            ISpoofaxSyntaxService syntaxService,
            ILanguageImpl languageImpl,
            Printer printer,
            Generator generator,
            Shrinker shrinker
    ) {
        this.termFactory = termFactory;
        this.unitService = unitService;
        this.syntaxService = syntaxService;
        this.languageImpl = languageImpl;
        this.printer = printer;
        this.generator = generator;
        this.shrinker = shrinker;
    }

    public TestResult test(TesterConfig config, TesterProgress progress) {
        FindResult findResult = find(config, progress);

        if (!findResult.found()) {
            return new TestResult(findResult);
        }

        ShrinkResult shrinkResult = shrink(findResult, progress);

        return new TestResult(findResult, shrinkResult);
    }

    public FindResult find(TesterConfig config, TesterProgress progress) {
        Timer timer = new Timer(true);

        int terms = config.getMaxNumberOfTerms();

        for (int i = 0; i < terms; i++) {
            try {
                Optional<IStrategoTerm> termOpt = generator.generate(config.getMaxTermSize());

                if (termOpt.isPresent()) {
                    IStrategoTerm term = termOpt.get();
                    String text = printer.print(term);

                    progress.sentenceGenerated(text);

                    ISpoofaxParseUnit parseUnit = parse(text);

                    if (parseUnit.success()) {
                        IStrategoTerm parsedTerm = parseUnit.ast();

                        if (isAmbiguous(parsedTerm)) {
                            return new FindResult(timer, i, parsedTerm, text);
                        }
                    }
                }
            } catch (TesterCancelledException e) {
                return new FindResult(timer, i);
            } catch (PrinterRuntimeException | ParseException e) {
                e.printStackTrace();
            }
        }

        return new FindResult(timer, terms);
    }

    public ShrinkResult shrink(FindResult findResult, TesterProgress progress) {
        Timer timer = new Timer(true);
        IStrategoTerm shrunk = shrink(findResult.term(), progress);

        try {
            return new ShrinkResult(timer, shrunk, printer.print(shrunk));
        } catch (TesterCancelledException e) {
            return new ShrinkResult(timer);
        }
    }

    protected IStrategoTerm shrink(IStrategoTerm term, TesterProgress progress) {
        // TODO: This is printing an ambiguous term, but Spoofax' pretty-printer sometimes fails to pretty-print an ambiguous term.
        progress.sentenceShrinked(printer.print(term));

        Optional<IStrategoTerm> shrunkOpt = shrink(term).findAny();

        if (!shrunkOpt.isPresent()) {
            return term;
        } else {
            return shrink(shrunkOpt.get(), progress);
        }
    }

    public Stream<IStrategoTerm> shrink(IStrategoTerm ambiguous) {
        IStrategoTerm nonambiguous = disambiguate(ambiguous);

        return shrinker
                .shrink(nonambiguous)
                .flatMap(this::printParse)
                .filter(this::isAmbiguous);
    }

    protected ISpoofaxParseUnit parse(String text) throws ParseException {
        ISpoofaxInputUnit inputUnit = unitService.inputUnit(text, languageImpl, null);

        return syntaxService.parse(inputUnit);
    }

    protected Stream<IStrategoTerm> printParse(IStrategoTerm term) {
        String text = printer.print(term);

        try {
            ISpoofaxParseUnit parseUnit = parse(text);

            if (parseUnit.success()) {
                return of(parseUnit.ast());
            }
        } catch (ParseException e) {
            return empty();
        }

        return empty();
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
                return flatten(replaceList(children, list));
            } else {
                return replaceList(children, list);
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

            return makeList(newChildren.toArray(IStrategoTerm[]::new), term);
        } else {
            return term;
        }
    }

    private Stream<IStrategoTerm> flattenOne(IStrategoTerm term) {
        if (term instanceof IStrategoList) {
            return Arrays.stream(term.getAllSubterms());
        } else {
            return of(term);
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

    /**
     * Spoofax' default OriginTermFactory does not copy attachments when replacing a list.
     *
     * @param children
     * @param oldList
     * @return
     */
    private IStrategoTerm replaceList(IStrategoTerm[] children, IStrategoList oldList) {
        IStrategoList newList = termFactory.replaceList(children, oldList);

        return termFactory.copyAttachments(oldList, newList);
    }

    /**
     * Spoofax' default OriginTermFactory does not copy attachments when replacing a list.
     *
     * @param children
     * @param oldList
     * @return
     */
    private IStrategoTerm makeList(IStrategoTerm[] children, IStrategoTerm oldList) {
        IStrategoList newList = termFactory.makeList(children);

        return termFactory.copyAttachments(oldList, newList);
    }
}
