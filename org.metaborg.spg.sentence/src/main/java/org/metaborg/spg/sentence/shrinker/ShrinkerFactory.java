package org.metaborg.spg.sentence.shrinker;

import com.google.inject.Inject;
import org.metaborg.core.language.ILanguageImpl;
import org.metaborg.spg.sentence.generator.Generator;
import org.metaborg.spg.sentence.parser.ParseService;
import org.metaborg.spg.sentence.printer.Printer;
import org.spoofax.interpreter.terms.ITermFactory;

public class ShrinkerFactory {
    private final ParseService parseService;

    @Inject
    public ShrinkerFactory(ParseService parseService) {
        this.parseService = parseService;
    }

    public Shrinker create(ILanguageImpl language, Printer printer, Generator generator, ITermFactory termFactory) {
        ShrinkerConfig shrinkerConfig = new ShrinkerConfig(language, printer);

        return new Shrinker(parseService, generator, termFactory, shrinkerConfig);
    }
}
