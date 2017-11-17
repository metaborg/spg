package org.metaborg.spg.sentence.ambiguity;

import com.google.inject.Inject;
import org.metaborg.core.language.ILanguageImpl;
import org.metaborg.core.project.IProject;
import org.metaborg.spg.sentence.generator.Generator;
import org.metaborg.spg.sentence.generator.GeneratorFactory;
import org.metaborg.spg.sentence.parser.ParseRuntimeException;
import org.metaborg.spg.sentence.parser.ParseService;
import org.metaborg.spg.sentence.printer.Printer;
import org.metaborg.spg.sentence.printer.PrinterFactory;
import org.metaborg.spg.sentence.shrinker.Shrinker;
import org.metaborg.spg.sentence.shrinker.ShrinkerFactory;
import org.metaborg.spg.sentence.shrinker.ShrinkerUnit;
import org.spoofax.interpreter.terms.IStrategoTerm;
import org.spoofax.interpreter.terms.ITermFactory;

import java.util.Optional;

public class AmbiguityTester {
    private final ParseService parseService;
    private final ITermFactory termFactory;
    private final PrinterFactory printerFactory;
    private final GeneratorFactory generatorFactory;
    private final ShrinkerFactory shrinkerFactory;

    @Inject
    public AmbiguityTester(
            ParseService parseService,
            ITermFactory termFactory,
            PrinterFactory printerFactory,
            GeneratorFactory generatorFactory,
            ShrinkerFactory shrinkerFactory) {
        this.parseService = parseService;
        this.termFactory = termFactory;
        this.printerFactory = printerFactory;
        this.generatorFactory = generatorFactory;
        this.shrinkerFactory = shrinkerFactory;
    }

    public AmbiguityTesterResult findAmbiguity(
            ILanguageImpl language,
            IProject project,
            AmbiguityTesterConfig config,
            AmbiguityTesterProgress progress) throws Exception {
        Printer printer = printerFactory.create(language, project);
        Generator generator = generatorFactory.create(language, project);
        Shrinker shrinker = shrinkerFactory.create(language, printer, generator, termFactory);

        long startTime = System.currentTimeMillis();

        for (int i = 0; i < config.getMaxNumberOfTerms(); i++) {
            try {
                Optional<IStrategoTerm> termOpt = generator.generate(config.getMaxTermSize());

                if (termOpt.isPresent()) {
                    IStrategoTerm term = termOpt.get();
                    String text = printer.print(term);

                    progress.sentenceGenerated(text);

                    IStrategoTerm parsedTerm = parseService.parse(language, text);

                    if (parseService.isAmbiguous(parsedTerm)) {
                        long duration = System.currentTimeMillis() - startTime;

                        shrink(shrinker, new ShrinkerUnit(parsedTerm, text), progress);

                        return new AmbiguityTesterResult(i, duration, text);
                    }
                }
            } catch (AmbiguityTesterCancelledException e) {
                long duration = System.currentTimeMillis() - startTime;

                return new AmbiguityTesterResult(i, duration);
            } catch (ParseRuntimeException e) {
                e.printStackTrace();
            }
        }

        long duration = System.currentTimeMillis() - startTime;

        return new AmbiguityTesterResult(config.getMaxNumberOfTerms(), duration);
    }

    protected void shrink(Shrinker shrinker, ShrinkerUnit shrinkerUnit, AmbiguityTesterProgress progress) {
        progress.sentenceShrinked(shrinkerUnit.getText());

        shrinker.shrink(shrinkerUnit)
                .findAny()
                .ifPresent(shrunkUnit -> shrink(shrinker, shrunkUnit, progress));
    }
}
