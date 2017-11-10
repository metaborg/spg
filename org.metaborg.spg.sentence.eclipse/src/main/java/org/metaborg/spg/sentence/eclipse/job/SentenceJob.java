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
import org.metaborg.core.language.ILanguageImpl;
import org.metaborg.core.project.IProject;
import org.metaborg.spg.sentence.*;
import org.metaborg.spg.sentence.eclipse.config.SentenceHandlerConfig;
import org.metaborg.spoofax.core.terms.ITermFactoryService;
import org.metaborg.spoofax.eclipse.util.ConsoleUtils;
import org.spoofax.interpreter.terms.IStrategoTerm;

import java.util.List;
import java.util.Optional;

public class SentenceJob extends Job {
    private final MessageConsole console = ConsoleUtils.get("Spoofax console");
    private final MessageConsoleStream stream = console.newMessageStream();
    private final ParseService parseService;
    private final GeneratorFactory generatorFactory;
    private final ShrinkerFactory shrinkerFactory;
    private final ITermFactoryService termFactoryService;

    private IProject project;
    private ILanguageImpl language;
    private SentenceHandlerConfig config;

    @Inject
    public SentenceJob(
            ParseService parseService,
            GeneratorFactory generatorFactory,
            ShrinkerFactory shrinkerFactory,
            ITermFactoryService termFactoryService,
            @Assisted IProject project,
            @Assisted ILanguageImpl language,
            @Assisted SentenceHandlerConfig config) {
        super("Generate");

        this.parseService = parseService;
        this.generatorFactory = generatorFactory;
        this.shrinkerFactory = shrinkerFactory;
        this.termFactoryService = termFactoryService;

        this.project = project;
        this.language = language;
        this.config = config;
    }

    @Override
    protected IStatus run(IProgressMonitor monitor) {
        try {
            final SubMonitor subMonitor = SubMonitor.convert(monitor, config.getLimit());

            Generator generator = generatorFactory.create(language, project);
            Shrinker shrinker = shrinkerFactory.create(language, project, generator, termFactoryService.getGeneric());

            for (int i = 0; i < config.getLimit(); i++) {
                Optional<IStrategoTerm> termOpt = generator.generate(config.getMaxSize());

                if (termOpt.isPresent()) {
                    processTerm(subMonitor, shrinker, termOpt.get());
                }
            }

            return Status.OK_STATUS;
        } catch (Exception e) {
            return Status.CANCEL_STATUS;
        }
    }

    private void processTerm(SubMonitor subMonitor, Shrinker shrinker, IStrategoTerm term) {
        progress(subMonitor, term.toString(1));

        if (parseService.isAmbiguous(term)) {
            shrink(shrinker, term);
        }
    }

    protected void progress(SubMonitor monitor, String program) {
        stream.println("=== Program ===");
        stream.println(program);

        monitor.split(1);
    }

    protected void shrink(Shrinker shrinker, IStrategoTerm term) {
        List<IStrategoTerm> shrunken = shrinker.shrink(term);

        // TODO: Recursively shrink, show progress.
    }
}
