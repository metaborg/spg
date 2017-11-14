package org.metaborg.spg.sentence.ambiguity;

import org.metaborg.core.language.ILanguageImpl;
import org.metaborg.core.project.IProject;
import org.metaborg.spg.sentence.generator.Generator;
import org.metaborg.spg.sentence.generator.GeneratorFactory;
import org.metaborg.spg.sentence.parser.ParseService;
import org.metaborg.spg.sentence.printer.Printer;
import org.metaborg.spg.sentence.printer.PrinterFactory;
import org.metaborg.spg.sentence.shrinker.Shrinker;
import org.metaborg.spg.sentence.shrinker.ShrinkerFactory;
import org.metaborg.spg.sentence.shrinker.ShrinkerUnit;
import org.metaborg.spoofax.core.terms.ITermFactoryService;
import org.spoofax.interpreter.terms.IStrategoTerm;
import org.spoofax.interpreter.terms.ITermFactory;

import java.util.Optional;

public class AmbiguityTester {
    private final ParseService parseService;
    private final ITermFactoryService termFactoryService;
    private final PrinterFactory printerFactory;
    private final GeneratorFactory generatorFactory;
    private final ShrinkerFactory shrinkerFactory;

    public AmbiguityTester(
            ParseService parseService,
            ITermFactoryService termFactoryService,
            PrinterFactory printerFactory,
            GeneratorFactory generatorFactory,
            ShrinkerFactory shrinkerFactory) {
        this.parseService = parseService;
        this.termFactoryService = termFactoryService;
        this.printerFactory = printerFactory;
        this.generatorFactory = generatorFactory;
        this.shrinkerFactory = shrinkerFactory;
    }

    public AmbiguityTesterResult findAmbiguity(
            ILanguageImpl language,
            IProject project,
            AmbiguityTesterConfig config,
            AmbiguityTesterProgress progress) throws Exception {
        ITermFactory termFactory = termFactoryService.getGeneric();
        Printer printer = printerFactory.create(language, project);
        Generator generator = generatorFactory.create(language, project, printer);
        Shrinker shrinker = shrinkerFactory.create(language, project, printer, generator, termFactory);

        long startTime = System.currentTimeMillis();

        for (int i = 0; i < config.getMaxNumberOfTerms(); i++) {
            try {
                Optional<String> textOpt = generator.generate(config.getMaxTermSize());

                if (textOpt.isPresent()) {
                    String text = textOpt.get();
                    progress.sentenceGenerated(text);

                    IStrategoTerm term = parseService.parse(language, text);

                    if (parseService.isAmbiguous(term)) {
                        long duration = System.currentTimeMillis() - startTime;

                        shrink(shrinker, new ShrinkerUnit(term, text), progress);

                        return new AmbiguityTesterResult(i, duration, text);
                    }
                }
            } catch (AmbiguityTesterCancelledException e) {
                long duration = System.currentTimeMillis() - startTime;

                return new AmbiguityTesterResult(i, duration);
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
