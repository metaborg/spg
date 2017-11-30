package org.metaborg.spg.sentence.eclipse.job;

import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;
import org.antlr.v4.tool.Grammar;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;
import org.metaborg.core.language.ILanguageImpl;
import org.metaborg.core.project.IProject;
import org.metaborg.spg.sentence.eclipse.Activator;
import org.metaborg.spg.sentence.eclipse.config.DifferenceJobConfig;
import org.metaborg.spg.sentence.generator.Generator;
import org.metaborg.spg.sentence.generator.GeneratorFactory;
import org.metaborg.spg.sentence.printer.Printer;
import org.metaborg.spg.sentence.printer.PrinterFactory;
import org.metaborg.spg.sentence.shrinker.Shrinker;
import org.metaborg.spg.sentence.shrinker.ShrinkerFactory;
import org.metaborg.spoofax.core.syntax.ISpoofaxSyntaxService;
import org.metaborg.spoofax.core.syntax.JSGLRParserConfiguration;
import org.metaborg.spoofax.core.unit.ISpoofaxParseUnit;
import org.metaborg.spoofax.core.unit.ISpoofaxUnitService;
import org.spoofax.interpreter.terms.IStrategoTerm;

import java.util.Optional;

import static org.metaborg.spg.sentence.antlr.functional.Utils.uncheck;

public class LiberalDifferenceJob extends DifferenceJob {
    public static final JSGLRParserConfiguration PARSER_CONFIG = new JSGLRParserConfiguration(true, false, false, 30000, Integer.MAX_VALUE);
    private final PrinterFactory printerFactory;
    private final GeneratorFactory generatorFactory;
    private final ShrinkerFactory shrinkerFactory;
    private final DifferenceJobConfig config;

    @Inject
    public LiberalDifferenceJob(
            ISpoofaxUnitService unitService,
            ISpoofaxSyntaxService syntaxService,
            PrinterFactory printerFactory,
            GeneratorFactory generatorFactory,
            ShrinkerFactory shrinkerFactory,
            @Assisted DifferenceJobConfig config) {
        super(unitService, syntaxService, "Difference test (liberal)");

        this.printerFactory = printerFactory;
        this.generatorFactory = generatorFactory;
        this.shrinkerFactory = shrinkerFactory;
        this.config = config;
    }

    @Override
    protected IStatus run(IProgressMonitor progressMonitor) {
        IProject project = config.getProject();
        ILanguageImpl language = config.getLanguage();
        int maxNumberOfTerms = config.getMaxNumberOfTerms();
        int maxTermSize = config.getMaxTermSize();
        Grammar grammar = Grammar.load(config.getAntlrGrammar());
        String antlrStartSymbol = config.getAntlrStartSymbol();

        SubMonitor subMonitor = SubMonitor.convert(progressMonitor, maxNumberOfTerms);

        try {
            Printer printer = printerFactory.create(language, project);
            Generator generator = generatorFactory.create(language, project);
            Shrinker shrinker = shrinkerFactory.create(generator);

            for (int i = 0; i < maxNumberOfTerms; i++) {
                try {
                    // Generate
                    Optional<IStrategoTerm> termOpt = generator.generate(maxTermSize);

                    if (termOpt.isPresent()) {
                        IStrategoTerm term = termOpt.get();
                        String text = printer.print(term);

                        stream.println("=== Program ===");
                        stream.println(text);

                        if (!canParseAntlr(grammar, antlrStartSymbol, text)) {
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
                                                .filter(uncheck(shrunkText -> !canParseAntlr(grammar, antlrStartSymbol, shrunkText)))
                                                .filter(uncheck(shrunkText -> canParseSpoofax(language, shrunkText, PARSER_CONFIG)))
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
