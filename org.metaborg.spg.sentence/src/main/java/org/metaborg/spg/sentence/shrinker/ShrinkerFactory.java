package org.metaborg.spg.sentence.shrinker;

import com.google.inject.Inject;
import org.metaborg.core.language.ILanguageImpl;
import org.metaborg.spg.sentence.IRandom;
import org.metaborg.spg.sentence.generator.Generator;
import org.metaborg.spg.sentence.parser.ParseService;
import org.metaborg.spg.sentence.printer.Printer;
import org.spoofax.interpreter.terms.ITermFactory;

public class ShrinkerFactory {
    private final IRandom random;
    private final ParseService parseService;

    @Inject
    public ShrinkerFactory(IRandom random, ParseService parseService) {
        this.random = random;
        this.parseService = parseService;
    }

    public Shrinker create(ILanguageImpl language, Printer printer, Generator generator, ITermFactory termFactory) {
        ShrinkerConfig shrinkerConfig = new ShrinkerConfig(language, printer);

        return new Shrinker(random, generator, termFactory);
    }
}
