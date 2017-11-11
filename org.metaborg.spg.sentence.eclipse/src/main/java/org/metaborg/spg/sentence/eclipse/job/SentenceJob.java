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
import org.metaborg.spoofax.core.terms.ITermFactoryService;
import org.metaborg.spoofax.eclipse.util.ConsoleUtils;
import org.spoofax.interpreter.terms.IStrategoTerm;

import java.util.Optional;

public class SentenceJob extends Job {
    private final MessageConsole console = ConsoleUtils.get("Spoofax console");
    private final MessageConsoleStream stream = console.newMessageStream();
    private final ParseService parseService;
    private final GeneratorFactory generatorFactory;
    private final ShrinkerFactory shrinkerFactory;
    private final PrettyPrinterFactory prettyPrinterFactory;
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
            PrettyPrinterFactory prettyPrinterFactory,
            ITermFactoryService termFactoryService,
            @Assisted IProject project,
            @Assisted("language") ILanguageImpl language,
            @Assisted("strategoLanguage") ILanguageImpl strategoLanguage,
            @Assisted SentenceHandlerConfig config) {
        super("Generate");

        this.parseService = parseService;
        this.generatorFactory = generatorFactory;
        this.shrinkerFactory = shrinkerFactory;
        this.prettyPrinterFactory = prettyPrinterFactory;
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

            PrettyPrinter prettyPrinter = prettyPrinterFactory.create(language, project);
            Generator generator = generatorFactory.create(language, project);
            Shrinker shrinker = shrinkerFactory.create(language, project, prettyPrinter, generator, termFactoryService.getGeneric(), strategoLanguage);

            for (int i = 0; i < config.getLimit(); i++) {
                Optional<IStrategoTerm> termOpt = generator.generate(config.getMaxSize());
                
                termOpt.ifPresent(Utils.uncheckConsumer(term ->
                    process(subMonitor, prettyPrinter, shrinker, term)
                ));
            }

            return Status.OK_STATUS;
        } catch (Exception e) {
            return Status.CANCEL_STATUS;
        }
    }

    private void process(SubMonitor subMonitor, PrettyPrinter prettyPrinter, Shrinker shrinker, IStrategoTerm term) throws MetaborgException {
        progress(subMonitor, term.toString());
        
        IStrategoTerm printParsedTerm = printParse(prettyPrinter, term);
        
        if (parseService.isAmbiguous(printParsedTerm)) {
            shrink(shrinker, term);
        }
    }
    
    protected IStrategoTerm printParse(PrettyPrinter prettyPrinter, IStrategoTerm term) throws MetaborgException {
    		String text = prettyPrinter.prettyPrint(term);
    		
    		return parseService.parse(language, text);
    }

    protected void progress(SubMonitor monitor, String program) {
        stream.println("=== Program ===");
        stream.println(program);

        monitor.split(1);
    }

    protected void shrink(Shrinker shrinker, IStrategoTerm term) {
        stream.println("=== Shrink ==");
        stream.println(term.toString());

        shrinker.shrink(term)
                .findAny()
                .ifPresent(shrunkTerm -> shrink(shrinker, shrunkTerm));
    }
}
