package org.metaborg.spg.sentence.eclipse.job;

import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;
import org.eclipse.core.runtime.*;
import org.metaborg.core.language.ILanguageImpl;
import org.metaborg.core.project.IProject;
import org.metaborg.spg.sentence.ambiguity.*;
import org.metaborg.spg.sentence.ambiguity.result.FindResult;
import org.metaborg.spg.sentence.ambiguity.result.ShrinkResult;
import org.metaborg.spg.sentence.ambiguity.result.TestResult;
import org.metaborg.spg.sentence.eclipse.Activator;

public class AmbiguityJob extends SentenceJob {
    private final TesterFactory testerFactory;
    private final TesterConfig config;
    private final IProject project;
    private final ILanguageImpl language;

    @Inject
    public AmbiguityJob(
            TesterFactory testerFactory,
            @Assisted TesterConfig config,
            @Assisted IProject project,
            @Assisted ILanguageImpl language) {
        super("Ambiguity test");

        this.testerFactory = testerFactory;
        this.config = config;
        this.project = project;
        this.language = language;
    }

    @Override
    protected IStatus run(IProgressMonitor monitor) {
        try {
            final SubMonitor subMonitor = SubMonitor.convert(monitor, config.getMaxNumberOfTerms());

            Tester tester = testerFactory.create(language, project);

            TesterProgress progress = new TesterProgress() {
                @Override
                public void sentenceGenerated(String text) {
                    stream.println("=== Program ===");
                    stream.println(text);

                    try {
                        subMonitor.split(1);
                    } catch (OperationCanceledException e) {
                        throw new TesterCancelledException(e);
                    }
                }

                @Override
                public void sentenceShrinked(String text) {
                    stream.println("=== Shrink ===");
                    stream.println(text);

                    try {
                        subMonitor.setWorkRemaining(50).split(1);
                    } catch (OperationCanceledException e) {
                        throw new TesterCancelledException(e);
                    }
                }
            };

            TestResult testResult = tester.test(config, progress);
            FindResult findResult = testResult.getFindResult();
            ShrinkResult shrinkResult = testResult.getShrinkResult();

            if (findResult.found()) {
                print("Found ambiguous sentence after %d terms (%d ms). ", findResult.terms(), findResult.duration());

                if (shrinkResult != null && shrinkResult.success()) {
                    print("Shrunk from %d to %d characters (%d ms).\n", findResult.text().length(), shrinkResult.text().length(), shrinkResult.duration());
                } else {
                    print("Unable to shrink (%d ms).\n", shrinkResult.duration());
                }
            } else {
                print("No ambiguous sentence found after %d terms (%d ms).\n", findResult.terms(), findResult.duration());
            }

            return Status.OK_STATUS;
        } catch (Exception e) {
            Activator.logError("An unexpected error occurred.", e);

            return Status.CANCEL_STATUS;
        }
    }

    private void print(String format, Object... arguments) {
        stream.print(String.format(format, arguments));
    }
}
