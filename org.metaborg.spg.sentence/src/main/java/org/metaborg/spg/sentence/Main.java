package org.metaborg.spg.sentence;

import com.google.inject.Injector;
import org.metaborg.core.MetaborgException;
import org.metaborg.core.language.ILanguageImpl;
import org.metaborg.core.project.IProject;
import org.metaborg.spg.sentence.ambiguity.*;
import org.metaborg.spg.sentence.ambiguity.result.FindResult;
import org.metaborg.spg.sentence.ambiguity.result.ShrinkResult;
import org.metaborg.spg.sentence.ambiguity.result.TestResult;
import org.metaborg.spg.sentence.guice.SentenceModule;
import org.metaborg.spoofax.core.Spoofax;

import java.io.File;

import static org.metaborg.spg.sentence.utils.SpoofaxUtils.getOrCreateProject;
import static org.metaborg.spg.sentence.utils.SpoofaxUtils.loadLanguage;

public class Main {
    public static void main(String[] args) throws Exception {
        try (final Spoofax spoofax = new Spoofax(new SentenceModule(28))) {
            ILanguageImpl language = loadLanguage(spoofax, new File(args[0]));
            IProject project = getOrCreateProject(spoofax, new File(args[1]));

            int maxNumberOfTerms = 1000;
            int maxTermSize = 10000;

            Injector injector = spoofax.injector;
            TesterFactory testerFactory = injector.getInstance(TesterFactory.class);
            Tester tester = testerFactory.create(language, project);
            TesterProgress progress = new TesterProgressDefault();
            TesterConfig config = new TesterConfig(maxNumberOfTerms, maxTermSize);

            TestResult result = tester.test(config, progress);
            FindResult findResult = result.getFindResult();
            ShrinkResult shrinkResult = result.getShrinkResult();

            if (findResult.found()) {
                print("Found ambiguous sentence after %d terms (%d ms). ",
                        findResult.terms(),
                        findResult.duration());

                if (shrinkResult != null) {
                    print("Shrunk from %d to %d characters (%d ms).",
                            findResult.text().length(),
                            shrinkResult.text().length(),
                            shrinkResult.duration());
                }
            } else {
                print("No ambiguous sentence found after %d terms (%d ms).",
                        findResult.terms(),
                        findResult.duration());
            }
        } catch (MetaborgException e) {
            e.printStackTrace();
        }
    }

    private static void print(String format, Object... arguments) {
        System.out.print(String.format(format, arguments));
    }
}
