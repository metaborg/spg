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
import org.metaborg.spg.sentence.terms.GeneratorTermFactory;
import org.metaborg.spoofax.core.syntax.ISpoofaxSyntaxService;
import org.metaborg.spoofax.core.syntax.JSGLRParserConfiguration;
import org.metaborg.spoofax.core.unit.ISpoofaxInputUnit;
import org.metaborg.spoofax.core.unit.ISpoofaxParseUnit;
import org.metaborg.spoofax.core.unit.ISpoofaxUnitService;
import org.metaborg.util.time.Timer;
import org.spoofax.interpreter.terms.IStrategoAppl;
import org.spoofax.interpreter.terms.IStrategoList;
import org.spoofax.interpreter.terms.IStrategoTerm;

import java.util.Arrays;
import java.util.Optional;
import java.util.stream.Stream;

import static java.util.stream.Stream.of;

public class Tester {
    private static final JSGLRParserConfiguration PARSER_CONFIG = new JSGLRParserConfiguration(false, false);
    private final GeneratorTermFactory termFactory;
    private final ISpoofaxUnitService unitService;
    private final ISpoofaxSyntaxService syntaxService;
    private final ILanguageImpl languageImpl;
    private final Printer printer;
    private final Generator generator;
    private final Shrinker shrinker;

    @jakarta.inject.Inject @javax.inject.Inject
    public Tester(
            GeneratorTermFactory termFactory,
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

        ShrinkResult shrinkResult = shrink(findResult.term(), progress);

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

                    if (isAmbiguous(text)) {
                        return new FindResult(timer, i, term, text);
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

    public ShrinkResult shrink(IStrategoTerm term, TesterProgress progress) {
        Timer timer = new Timer(true);

        return shrink(term, progress, timer);
    }

    protected ShrinkResult shrink(IStrategoTerm term, TesterProgress progress, Timer timer) {
        IStrategoTerm nonambiguous = disambiguate(term);
        String text = printer.print(nonambiguous);

        try {
            progress.sentenceShrinked(text);
        } catch (TesterCancelledException e) {
            return new ShrinkResult(timer, nonambiguous, text);
        }

        Optional<IStrategoTerm> shrunkOpt = shrink(nonambiguous).findAny();

        if (!shrunkOpt.isPresent()) {
            return new ShrinkResult(timer, nonambiguous, text);
        } else {
            return shrink(shrunkOpt.get(), progress, timer);
        }
    }

    public Stream<IStrategoTerm> shrink(IStrategoTerm nonambiguous) {
        Stream<IStrategoTerm> shrunkTerms = shrinker.shrink(nonambiguous);

        return shrunkTerms.filter(this::printAmbiguous);
    }

    protected boolean printAmbiguous(IStrategoTerm term) {
        String text = printer.print(term);

        try {
            ISpoofaxParseUnit parseUnit = parse(text);

            if (parseUnit.success()) {
                return isAmbiguous(parseUnit.ast());
            } else {
                return false;
            }
        } catch (ParseException e) {
            return false;
        }
    }

    protected ISpoofaxParseUnit parse(String text) throws ParseException {
        ISpoofaxInputUnit inputUnit = unitService.inputUnit(text, languageImpl, null, PARSER_CONFIG);

        return syntaxService.parse(inputUnit);
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

                return termFactory.replaceAppl(children, appl);
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
            IStrategoList list = (IStrategoList) term;

            Stream<IStrategoTerm> oldChildren = Arrays.stream(list.getAllSubterms());
            Stream<IStrategoTerm> newChildren = oldChildren.flatMap(this::flattenOne);
            IStrategoTerm[] children = newChildren.toArray(IStrategoTerm[]::new);

            return termFactory.replaceList(children, list);
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

    private boolean isAmbiguous(String text) throws ParseException {
        ISpoofaxParseUnit parseUnit = parse(text);

        return parseUnit.success() && isAmbiguous(parseUnit.ast());
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
