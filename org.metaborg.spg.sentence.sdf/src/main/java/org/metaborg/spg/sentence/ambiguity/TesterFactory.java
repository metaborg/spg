package org.metaborg.spg.sentence.ambiguity;

import com.google.inject.Inject;
import org.metaborg.core.language.ILanguageImpl;
import org.metaborg.core.project.IProject;
import org.metaborg.spg.sentence.generator.Generator;
import org.metaborg.spg.sentence.generator.GeneratorFactory;
import org.metaborg.spg.sentence.printer.Printer;
import org.metaborg.spg.sentence.printer.PrinterFactory;
import org.metaborg.spg.sentence.sdf3.Grammar;
import org.metaborg.spg.sentence.sdf3.GrammarFactory;
import org.metaborg.spg.sentence.shrinker.Shrinker;
import org.metaborg.spg.sentence.shrinker.ShrinkerFactory;
import org.metaborg.spg.sentence.signature.Signature;
import org.metaborg.spg.sentence.signature.SignatureFactory;
import org.metaborg.spg.sentence.terms.GeneratorTermFactory;
import org.metaborg.spoofax.core.syntax.ISpoofaxSyntaxService;
import org.metaborg.spoofax.core.unit.ISpoofaxUnitService;

public class TesterFactory {
    private final GeneratorTermFactory termFactory;
    private final ISpoofaxUnitService unitService;
    private final ISpoofaxSyntaxService syntaxService;
    private final PrinterFactory printerFactory;
    private final GeneratorFactory generatorFactory;
    private final GrammarFactory grammarFactory;
    private final SignatureFactory signatureFactory;
    private final ShrinkerFactory shrinkerFactory;

    @jakarta.inject.Inject
    public TesterFactory(
            GeneratorTermFactory termFactory,
            ISpoofaxUnitService unitService,
            ISpoofaxSyntaxService syntaxService,
            PrinterFactory printerFactory,
            GeneratorFactory generatorFactory,
            GrammarFactory grammarFactory,
            SignatureFactory signatureFactory,
            ShrinkerFactory shrinkerFactory) {
        this.termFactory = termFactory;
        this.unitService = unitService;
        this.syntaxService = syntaxService;
        this.printerFactory = printerFactory;
        this.generatorFactory = generatorFactory;
        this.grammarFactory = grammarFactory;
        this.signatureFactory = signatureFactory;
        this.shrinkerFactory = shrinkerFactory;
    }

    public Tester create(ILanguageImpl templateLanguageImpl, ILanguageImpl languageImpl, IProject project) throws Exception {
        Printer printer = printerFactory.create(languageImpl, project);
        Generator generator = generatorFactory.create(languageImpl, project);
        Grammar grammar = grammarFactory.create(templateLanguageImpl, project);
        Signature signature = signatureFactory.create(grammar);
        Shrinker shrinker = shrinkerFactory.create(generator, signature);

        return new Tester(termFactory, unitService, syntaxService, languageImpl, printer, generator, shrinker);
    }
}
