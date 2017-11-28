package org.metaborg.spg.sentence.ambiguity;

import com.google.inject.Inject;
import org.metaborg.core.language.ILanguageImpl;
import org.metaborg.core.project.IProject;
import org.metaborg.spg.sentence.generator.Generator;
import org.metaborg.spg.sentence.generator.GeneratorFactory;
import org.metaborg.spg.sentence.parser.ParseService;
import org.metaborg.spg.sentence.printer.Printer;
import org.metaborg.spg.sentence.printer.PrinterFactory;
import org.metaborg.spg.sentence.shrinker.Shrinker;
import org.metaborg.spg.sentence.shrinker.ShrinkerFactory;
import org.spoofax.interpreter.terms.ITermFactory;

public class AmbiguityTesterFactory {
    private final ITermFactory termFactory;
    private final ParseService parseService;
    private final PrinterFactory printerFactory;
    private final GeneratorFactory generatorFactory;
    private final ShrinkerFactory shrinkerFactory;

    @Inject
    public AmbiguityTesterFactory(
            ITermFactory termFactory,
            ParseService parseService,
            PrinterFactory printerFactory,
            GeneratorFactory generatorFactory,
            ShrinkerFactory shrinkerFactory) {
        this.parseService = parseService;
        this.printerFactory = printerFactory;
        this.generatorFactory = generatorFactory;
        this.shrinkerFactory = shrinkerFactory;
        this.termFactory = termFactory;
    }

    public AmbiguityTester create(ILanguageImpl languageImpl, IProject project) throws Exception {
        Printer printer = printerFactory.create(languageImpl, project);
        Generator generator = generatorFactory.create(languageImpl, project);
        Shrinker shrinker = shrinkerFactory.create(languageImpl, printer, generator, termFactory);

        return new AmbiguityTester(parseService, termFactory, languageImpl, printer, generator, shrinker);
    }
}
