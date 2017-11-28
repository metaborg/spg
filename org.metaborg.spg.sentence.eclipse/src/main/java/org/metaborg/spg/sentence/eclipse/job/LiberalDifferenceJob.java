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
import org.metaborg.spg.sentence.antlr.functional.Utils;
import org.metaborg.spg.sentence.generator.Generator;
import org.metaborg.spg.sentence.generator.GeneratorFactory;
import org.metaborg.spg.sentence.printer.Printer;
import org.metaborg.spg.sentence.printer.PrinterFactory;
import org.metaborg.spg.sentence.shrinker.Shrinker;
import org.metaborg.spg.sentence.shrinker.ShrinkerFactory;
import org.metaborg.spoofax.core.syntax.ISpoofaxSyntaxService;
import org.metaborg.spoofax.core.unit.ISpoofaxParseUnit;
import org.metaborg.spoofax.core.unit.ISpoofaxUnitService;
import org.spoofax.interpreter.terms.IStrategoTerm;
import org.spoofax.interpreter.terms.ITermFactory;

import java.util.Optional;

import static org.metaborg.spg.sentence.antlr.functional.Utils.uncheck;

public class LiberalDifferenceJob extends DifferenceJob {
    private final ITermFactory termFactory;
    private final PrinterFactory printerFactory;
    private final GeneratorFactory generatorFactory;
    private final ShrinkerFactory shrinkerFactory;

    private final IProject project;
    private final ILanguageImpl language;

    @Inject
    public LiberalDifferenceJob(
            ISpoofaxUnitService unitService,
            ISpoofaxSyntaxService syntaxService,
            ITermFactory termFactory,
            PrinterFactory printerFactory,
            GeneratorFactory generatorFactory,
            ShrinkerFactory shrinkerFactory,
            @Assisted IProject project,
            @Assisted ILanguageImpl language) {
        super(unitService, syntaxService, "Difference test (liberal)");

        this.termFactory = termFactory;
        this.printerFactory = printerFactory;
        this.generatorFactory = generatorFactory;
        this.shrinkerFactory = shrinkerFactory;

        this.project = project;
        this.language = language;
    }

    @Override
    protected IStatus run(IProgressMonitor progressMonitor) {
        int maxNumberOfTerms = 10000;
        int maxTermSize = 1000;
        Grammar grammar = Grammar.load("/Users/martijn/Projects/metaborg-antlr/org.metaborg.lang.antlr.examples/java.g");
        String antlrStartSymbol = "compilationUnit";

        SubMonitor subMonitor = SubMonitor.convert(progressMonitor, maxNumberOfTerms);

        try {
            Printer printer = printerFactory.create(language, project);
            Generator generator = generatorFactory.create(language, project);
            Shrinker shrinker = shrinkerFactory.create(language, printer, generator, termFactory);

            for (int i = 0; i < maxNumberOfTerms; i++) {
                try {
                    Optional<IStrategoTerm> termOpt = generator.generate(maxTermSize);

                    if (termOpt.isPresent()) {
                        IStrategoTerm term = termOpt.get();
                        String text = printer.print(term);

                        stream.println("=== Program ===");
                        stream.println(text);

                        if (!canParseAntlr(grammar, antlrStartSymbol, text)) {
                            ISpoofaxParseUnit parseUnit = parseSpoofax(language, text);

                            if (parseUnit.success()) {
                                stream.println("=== Legal SDF3 illegal ANTLRv4 sentence ===");
                                stream.println(text);
                                
                                // Shrink
                                try {
                                    while (true) {
                                        Optional<String> shrunkTextOpt = shrinker
                                                .shrink(parseUnit.ast())
                                                .map(printer::print)
                                                .filter(uncheck(shrunkText -> !canParseAntlr(grammar, antlrStartSymbol, shrunkText)))
                                                .filter(uncheck(shrunkText -> canParseSpoofax(language, shrunkText)))
                                                .findAny();

                                        if (shrunkTextOpt.isPresent()) {
                                            stream.println("=== Shrunk sentence ===");
                                            stream.println(shrunkTextOpt.get());

                                            parseUnit = parseSpoofax(language, shrunkTextOpt.get());
                                        } else {
                                            break;
                                        }
                                    }
                                } catch (Exception e) {
                                    e.printStackTrace();
                                } finally {
                                    break;
                                }
                            }
                        }
                    }

                    subMonitor.split(1);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            return Status.OK_STATUS;
        } catch (Exception e) {
            return Status.CANCEL_STATUS;
        }
    }
}
