package org.metaborg.spg.sentence.eclipse.job;

import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;
import org.eclipse.core.runtime.*;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.ui.console.MessageConsole;
import org.eclipse.ui.console.MessageConsoleStream;
import org.metaborg.core.language.ILanguageImpl;
import org.metaborg.core.project.IProject;
import org.metaborg.spg.sentence.ambiguity.*;
import org.metaborg.spg.sentence.generator.GeneratorFactory;
import org.metaborg.spg.sentence.parser.ParseService;
import org.metaborg.spg.sentence.printer.PrinterFactory;
import org.metaborg.spg.sentence.shrinker.ShrinkerFactory;
import org.metaborg.spoofax.core.terms.ITermFactoryService;
import org.metaborg.spoofax.eclipse.util.ConsoleUtils;

public class SentenceJob extends Job {
    private final MessageConsole console = ConsoleUtils.get("Spoofax console");
    private final MessageConsoleStream stream = console.newMessageStream();
    private final ParseService parseService;
    private final GeneratorFactory generatorFactory;
    private final ShrinkerFactory shrinkerFactory;
    private final PrinterFactory printerFactory;
    private final ITermFactoryService termFactoryService;

    private final IProject project;
    private final ILanguageImpl language;
    private final AmbiguityTesterConfig config;

    @Inject
    public SentenceJob(
            ParseService parseService,
            GeneratorFactory generatorFactory,
            ShrinkerFactory shrinkerFactory,
            PrinterFactory printerFactory,
            ITermFactoryService termFactoryService,
            @Assisted IProject project,
            @Assisted ILanguageImpl language,
            @Assisted AmbiguityTesterConfig config) {
        super("Generate");

        this.parseService = parseService;
        this.generatorFactory = generatorFactory;
        this.shrinkerFactory = shrinkerFactory;
        this.printerFactory = printerFactory;
        this.termFactoryService = termFactoryService;

        this.project = project;
        this.language = language;
        this.config = config;
    }

    @Override
    protected IStatus run(IProgressMonitor monitor) {
        try {
            final SubMonitor subMonitor = SubMonitor.convert(monitor, config.getMaxNumberOfTerms());

            AmbiguityTester ambiguityTester = new AmbiguityTester(
                    parseService,
                    termFactoryService,
                    printerFactory,
                    generatorFactory,
                    shrinkerFactory
            );

            AmbiguityTesterProgress progress = new AmbiguityTesterProgress() {
                @Override
                public void sentenceGenerated(String text) {
                    stream.println("=== Program ===");
                    stream.println(text);

                    try {
                        subMonitor.split(1);
                    } catch (OperationCanceledException e) {
                        throw new AmbiguityTesterCancelledException(e);
                    }
                }

                @Override
                public void sentenceShrinked(String text) {
                    stream.println("=== Shrink ==");
                    stream.println(text);
                }
            };

            AmbiguityTesterResult ambiguityTesterResult = ambiguityTester.findAmbiguity(language, project, config, progress);

            if (ambiguityTesterResult.foundAmbiguity()) {
                stream.println("Found ambiguous sentence after " + ambiguityTesterResult.getTerms() + " terms (" + ambiguityTesterResult.getDuration() + " ms).");
            } else {
                stream.println("No sentence found after " + ambiguityTesterResult.getTerms() + " terms (" + ambiguityTesterResult.getDuration() + " ms).");
            }

            return Status.OK_STATUS;
        } catch (Exception e) {
            return Status.CANCEL_STATUS;
        }
    }
}
