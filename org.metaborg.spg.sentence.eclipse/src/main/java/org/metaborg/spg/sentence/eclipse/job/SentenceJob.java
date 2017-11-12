package org.metaborg.spg.sentence.eclipse.job;

import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.ui.console.MessageConsole;
import org.eclipse.ui.console.MessageConsoleStream;
import org.metaborg.core.MetaborgException;
import org.metaborg.core.language.ILanguageImpl;
import org.metaborg.core.project.IProject;
import org.metaborg.spg.sentence.*;
import org.metaborg.spg.sentence.eclipse.config.SentenceHandlerConfig;
import org.metaborg.spg.sentence.generator.Generator;
import org.metaborg.spg.sentence.generator.GeneratorFactory;
import org.metaborg.spg.sentence.printer.Printer;
import org.metaborg.spg.sentence.printer.PrinterFactory;
import org.metaborg.spg.sentence.shrinker.Shrinker;
import org.metaborg.spg.sentence.shrinker.ShrinkerFactory;
import org.metaborg.spg.sentence.shrinker.ShrinkerUnit;
import org.metaborg.spoofax.core.terms.ITermFactoryService;
import org.metaborg.spoofax.eclipse.util.ConsoleUtils;
import org.spoofax.interpreter.terms.IStrategoTerm;
import org.spoofax.interpreter.terms.ITermFactory;

import java.util.Optional;

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
    private final ILanguageImpl strategoLanguage;
    private final SentenceHandlerConfig config;

    @Inject
    public SentenceJob(
            ParseService parseService,
            GeneratorFactory generatorFactory,
            ShrinkerFactory shrinkerFactory,
            PrinterFactory printerFactory,
            ITermFactoryService termFactoryService,
            @Assisted IProject project,
            @Assisted("language") ILanguageImpl language,
            @Assisted("strategoLanguage") ILanguageImpl strategoLanguage,
            @Assisted SentenceHandlerConfig config) {
        super("Generate");

        this.parseService = parseService;
        this.generatorFactory = generatorFactory;
        this.shrinkerFactory = shrinkerFactory;
        this.printerFactory = printerFactory;
        this.termFactoryService = termFactoryService;

        this.project = project;
        this.language = language;
        this.strategoLanguage = strategoLanguage;
        this.config = config;
    }

    @Override
    protected IStatus run(IProgressMonitor monitor) {
        try {
            final SubMonitor subMonitor = SubMonitor.convert(monitor, config.getLimit());

            ITermFactory termFactory = termFactoryService.getGeneric();
            Printer printer = printerFactory.create(language, project);
            Generator generator = generatorFactory.create(language, project, printer);
            Shrinker shrinker = shrinkerFactory.create(language, project, printer, generator, termFactory, strategoLanguage);

            for (int i = 0; i < config.getLimit(); i++) {
                Optional<String> textOpt = generator.generate(config.getMaxSize());
                
                textOpt.ifPresent(Utils.uncheckConsumer(text ->
                    process(subMonitor, shrinker, text)
                ));
            }

            return Status.OK_STATUS;
        } catch (Exception e) {
            return Status.CANCEL_STATUS;
        }
    }

    private void process(SubMonitor subMonitor, Shrinker shrinker, String text) throws MetaborgException {
        progress(subMonitor, text);
        
        IStrategoTerm term = parseService.parse(language, text);
        
        if (parseService.isAmbiguous(term)) {
            shrink(shrinker, new ShrinkerUnit(term, text));
        }
    }

    protected void progress(SubMonitor monitor, String text) {
        stream.println("=== Program ===");
        stream.println(text);

        monitor.split(1);
    }

    protected void shrink(Shrinker shrinker, ShrinkerUnit shrinkerUnit) {
        stream.println("=== Shrink ==");
        stream.println(shrinkerUnit.getText());

        shrinker.shrink(shrinkerUnit)
                .findAny()
                .ifPresent(shrunkUnit -> shrink(shrinker, shrunkUnit));
    }
}
