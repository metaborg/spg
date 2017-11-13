package org.metaborg.spg.sentence.shrinker;

import com.google.inject.Inject;
import org.metaborg.core.language.ILanguageImpl;
import org.metaborg.core.project.IProject;
import org.metaborg.core.syntax.ParseException;
import org.metaborg.spg.sentence.parser.ParseService;
import org.metaborg.spg.sentence.generator.Generator;
import org.metaborg.spg.sentence.printer.Printer;
import org.spoofax.interpreter.terms.ITermFactory;

import java.io.IOException;

public class ShrinkerFactory {
    private final ParseService parseService;

    @Inject
    public ShrinkerFactory(ParseService parseService) {
        this.parseService = parseService;
    }

    // TODO: Reduce number of arguments.
    public Shrinker create(ILanguageImpl language, IProject project, Printer printer, Generator generator, ITermFactory termFactory) throws IOException, ParseException {
        ShrinkerConfig shrinkerConfig = new ShrinkerConfig(language, printer);

        return new Shrinker(parseService, generator, termFactory, shrinkerConfig);
    }
}
