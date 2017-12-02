package org.metaborg.spg.sentence.eclipse.job;

import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;
import org.antlr.v4.tool.Grammar;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;
import org.metaborg.core.MetaborgException;
import org.metaborg.core.language.ILanguageImpl;
import org.metaborg.core.project.IProject;
import org.metaborg.spg.sentence.eclipse.Activator;
import org.metaborg.spg.sentence.eclipse.config.DifferenceJobConfig;
import org.metaborg.spg.sentence.generator.Generator;
import org.metaborg.spg.sentence.generator.GeneratorFactory;
import org.metaborg.spg.sentence.printer.Printer;
import org.metaborg.spg.sentence.printer.PrinterFactory;
import org.metaborg.spg.sentence.sdf3.GrammarFactory;
import org.metaborg.spg.sentence.shrinker.Shrinker;
import org.metaborg.spg.sentence.shrinker.ShrinkerFactory;
import org.metaborg.spg.sentence.signature.Signature;
import org.metaborg.spg.sentence.signature.SignatureFactory;
import org.metaborg.spoofax.core.syntax.ISpoofaxSyntaxService;
import org.metaborg.spoofax.core.syntax.JSGLRParserConfiguration;
import org.metaborg.spoofax.core.unit.ISpoofaxParseUnit;
import org.metaborg.spoofax.core.unit.ISpoofaxUnitService;
import org.metaborg.spoofax.eclipse.SpoofaxPlugin;
import org.spoofax.interpreter.terms.IStrategoTerm;

import java.io.File;
import java.util.Optional;

import static org.metaborg.spg.sentence.shared.utils.FunctionalUtils.uncheckPredicate;
import static org.metaborg.spg.sentence.shared.utils.SpoofaxUtils.loadLanguage;

public class LiberalDifferenceJob extends DifferenceJob {
    public static final JSGLRParserConfiguration PARSER_CONFIG = new JSGLRParserConfiguration(true, false, false, 30000, Integer.MAX_VALUE);
    private final PrinterFactory printerFactory;
    private final GeneratorFactory generatorFactory;
    private final GrammarFactory grammarFactory;
    private final SignatureFactory signatureFactory;
    private final ShrinkerFactory shrinkerFactory;
    private final DifferenceJobConfig config;
    private final ILanguageImpl templateLanguage;

    @Inject
    public LiberalDifferenceJob(
            ISpoofaxUnitService unitService,
            ISpoofaxSyntaxService syntaxService,
            PrinterFactory printerFactory,
            GeneratorFactory generatorFactory,
            GrammarFactory grammarFactory,
            SignatureFactory signatureFactory,
            ShrinkerFactory shrinkerFactory,
            @Assisted DifferenceJobConfig config) throws MetaborgException {
        super(unitService, syntaxService, "Difference test (liberal)");

        this.printerFactory = printerFactory;
        this.generatorFactory = generatorFactory;
        this.grammarFactory = grammarFactory;
        this.signatureFactory = signatureFactory;
        this.shrinkerFactory = shrinkerFactory;
        this.config = config;
        this.templateLanguage = loadLanguage(SpoofaxPlugin.spoofax(), new File("/Users/martijn/Projects/spoofax-releng/sdf/org.metaborg.meta.lang.template/target/org.metaborg.meta.lang.template-2.4.0-SNAPSHOT.spoofax-language"));
    }

    @Override
    protected IStatus run(IProgressMonitor progressMonitor) {
        IProject project = config.getProject();
        ILanguageImpl language = config.getLanguage();
        int maxNumberOfTerms = config.getMaxNumberOfTerms();
        int maxTermSize = config.getMaxTermSize();
        Grammar antlrGrammar = Grammar.load(config.getAntlrGrammar());
        String antlrStartSymbol = config.getAntlrStartSymbol();

        SubMonitor subMonitor = SubMonitor.convert(progressMonitor, maxNumberOfTerms);

        try {
            Printer printer = printerFactory.create(language, project);
            Generator generator = generatorFactory.create(language, project);
            org.metaborg.spg.sentence.sdf3.Grammar grammar = grammarFactory.create(templateLanguage, project);
            Signature signature = signatureFactory.create(grammar);
            Shrinker shrinker = shrinkerFactory.create(generator, signature);

            for (int i = 0; i < maxNumberOfTerms; i++) {
                try {
                    // Generate
                    Optional<IStrategoTerm> termOpt = generator.generate(maxTermSize);

                    if (termOpt.isPresent()) {
                        IStrategoTerm term = termOpt.get();
                        String text = printer.print(term);

                        stream.println("=== Program ===");
                        stream.println(text);

                        if (!canParseAntlr(antlrGrammar, antlrStartSymbol, text)) {
                            ISpoofaxParseUnit parseUnit = parseSpoofax(config.getLanguage(), text, PARSER_CONFIG);

                            if (parseUnit.success()) {
                                stream.println("=== Legal SDF3 illegal ANTLRv4 sentence ===");
                                stream.println(text);

                                // Shrink
                                try {
                                    while (true) {
                                        subMonitor.setWorkRemaining(50).split(1);

                                        Optional<String> shrunkTextOpt = shrinker
                                                .shrink(parseUnit.ast())
                                                .map(printer::print)
                                                .filter(uncheckPredicate(shrunkText -> !canParseAntlr(antlrGrammar, antlrStartSymbol, shrunkText)))
                                                .filter(uncheckPredicate(shrunkText -> canParseSpoofax(language, shrunkText, PARSER_CONFIG)))
                                                .findAny();

                                        if (shrunkTextOpt.isPresent()) {
                                            stream.println("=== Shrunk ===");
                                            stream.println(shrunkTextOpt.get());

                                            parseUnit = parseSpoofax(language, shrunkTextOpt.get(), PARSER_CONFIG);
                                        } else {
                                            break;
                                        }
                                    }
                                } catch (Exception e) {
                                    Activator.logError("An unexpected error occurred.", e);
                                }

                                break;
                            }
                        }
                    }

                    subMonitor.split(1);
                } catch (Exception e) {
                    Activator.logError("An unexpected error occurred.", e);
                }
            }

            return Status.OK_STATUS;
        } catch (Exception e) {
            return Status.CANCEL_STATUS;
        }
    }
}
