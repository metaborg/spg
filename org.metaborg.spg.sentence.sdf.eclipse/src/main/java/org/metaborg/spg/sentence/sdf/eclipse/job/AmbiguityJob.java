package org.metaborg.spg.sentence.sdf.eclipse.job;

import static org.metaborg.spg.sentence.shared.utils.SpoofaxUtils.getLanguage;

import java.text.NumberFormat;
import java.util.Locale;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;
import org.metaborg.core.MetaborgException;
import org.metaborg.core.language.ILanguageImpl;
import org.metaborg.core.project.IProject;
import org.metaborg.spg.sentence.ambiguity.StatisticsTesterProgress;
import org.metaborg.spg.sentence.ambiguity.Tester;
import org.metaborg.spg.sentence.ambiguity.TesterCancelledException;
import org.metaborg.spg.sentence.ambiguity.TesterConfig;
import org.metaborg.spg.sentence.ambiguity.TesterFactory;
import org.metaborg.spg.sentence.ambiguity.result.FindResult;
import org.metaborg.spg.sentence.ambiguity.result.ShrinkResult;
import org.metaborg.spg.sentence.ambiguity.result.TestResult;
import org.metaborg.spg.sentence.sdf.eclipse.Activator;
import org.metaborg.spg.sentence.statistics.Histogram;
import org.metaborg.spoofax.core.SpoofaxConstants;
import org.metaborg.spoofax.eclipse.SpoofaxPlugin;

import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;

public class AmbiguityJob extends SentenceJob {
    private static final NumberFormat numberFormat = NumberFormat.getNumberInstance(Locale.US);

    private final TesterFactory testerFactory;
    private final TesterConfig config;
    private final IProject project;
    private final ILanguageImpl language;
    private final ILanguageImpl templateLanguage;

    @jakarta.inject.Inject @javax.inject.Inject
    public AmbiguityJob(
            TesterFactory testerFactory,
            @Assisted TesterConfig config,
            @Assisted IProject project,
            @Assisted ILanguageImpl language) throws MetaborgException {
        super("Ambiguity test");

        this.testerFactory = testerFactory;
        this.config = config;
        this.project = project;
        this.language = language;
        this.templateLanguage = getLanguage(SpoofaxPlugin.spoofax(), SpoofaxConstants.LANG_SDF3_NAME);
    }

    @Override
    protected IStatus run(IProgressMonitor monitor) {
        try {
            final SubMonitor subMonitor = SubMonitor.convert(monitor, config.getMaxNumberOfTerms());

            Tester tester = testerFactory.create(templateLanguage, language, project);

            StatisticsTesterProgress progress = new StatisticsTesterProgress() {
                @Override
                public void sentenceGenerated(String text) {
                    super.sentenceGenerated(text);

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
                    stream.println("=== Shrunk to " + text.length() + " characters ===");
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
                print("Found ambiguous sentence after %s terms (%s ms). ",
                        numberFormat.format(findResult.terms()),
                        numberFormat.format(findResult.duration()));

                if (shrinkResult != null) {
                    if (shrinkResult.success()) {
                        print("Shrunk from %s to %s characters (%s ms).\n\n",
                                numberFormat.format(findResult.text().length()),
                                numberFormat.format(shrinkResult.text().length()),
                                numberFormat.format(shrinkResult.duration()));
                    } else {
                        print("Unable to shrink (%s ms).\n\n",
                                numberFormat.format(shrinkResult.duration()));
                    }
                }
            } else {
                print("No ambiguous sentence found after %s terms (%s ms).\n\n",
                        numberFormat.format(findResult.terms()),
                        numberFormat.format(findResult.duration()));
            }

            print("### Statistics ###\n");
            print("%s", new Histogram(progress.getLengths()));

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
