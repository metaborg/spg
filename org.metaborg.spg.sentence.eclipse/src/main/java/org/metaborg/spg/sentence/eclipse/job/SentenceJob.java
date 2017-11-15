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
import org.metaborg.spg.sentence.eclipse.Activator;
import org.metaborg.spoofax.eclipse.util.ConsoleUtils;

public class SentenceJob extends Job {
    private final MessageConsole console = ConsoleUtils.get("Spoofax console");
    private final MessageConsoleStream stream = console.newMessageStream();

    private final AmbiguityTesterFactory ambiguityTesterFactory;
    private final AmbiguityTesterConfig config;
    private final IProject project;
    private final ILanguageImpl language;

    @Inject
    public SentenceJob(
            AmbiguityTesterFactory ambiguityTesterFactory,
            @Assisted AmbiguityTesterConfig config,
            @Assisted IProject project,
            @Assisted ILanguageImpl language) {
        super("Generate");

        this.ambiguityTesterFactory = ambiguityTesterFactory;
        this.config = config;
        this.project = project;
        this.language = language;
    }

    @Override
    protected IStatus run(IProgressMonitor monitor) {
        try {
            final SubMonitor subMonitor = SubMonitor.convert(monitor, config.getMaxNumberOfTerms());

            AmbiguityTester ambiguityTester = ambiguityTesterFactory.create();

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

                    try {
                        subMonitor.setWorkRemaining(50).split(1);
                    } catch (OperationCanceledException e) {
                        throw new AmbiguityTesterCancelledException(e);
                    }
                }
            };

            AmbiguityTesterResult ambiguityTesterResult = ambiguityTester.findAmbiguity(language, project, config, progress);

            if (ambiguityTesterResult.foundAmbiguity()) {
                stream.println("Found ambiguous sentence after " + ambiguityTesterResult.getTerms() + " terms (" + ambiguityTesterResult.getDuration() + " ms).");
            } else {
                stream.println("No ambiguous sentence found after " + ambiguityTesterResult.getTerms() + " terms (" + ambiguityTesterResult.getDuration() + " ms).");
            }

            return Status.OK_STATUS;
        } catch (Exception e) {
            Activator.logError("An unexpected error occurred.", e);

            return Status.CANCEL_STATUS;
        }
    }
}
