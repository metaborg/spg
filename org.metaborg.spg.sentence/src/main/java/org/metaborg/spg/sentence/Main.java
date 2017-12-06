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

import static org.metaborg.spg.sentence.shared.utils.SpoofaxUtils.getOrCreateProject;
import static org.metaborg.spg.sentence.shared.utils.SpoofaxUtils.loadLanguage;

public class Main {
    private static final String TEMPLATE_LANG = "/Users/martijn/Projects/spoofax-releng/sdf/org.metaborg.meta.lang.template/target/org.metaborg.meta.lang.template-2.4.0-SNAPSHOT.spoofax-language";

    public static void main(String[] args) throws Exception {
        try (final Spoofax spoofax = new Spoofax(new SentenceModule(0))) {
            ILanguageImpl templateLanguage = loadLanguage(spoofax, TEMPLATE_LANG);
            ILanguageImpl language = loadLanguage(spoofax, args[0]);
            IProject project = getOrCreateProject(spoofax, args[1]);

            int maxNumberOfTerms = 1000;
            int maxTermSize = 10000;

            Injector injector = spoofax.injector;
            TesterFactory testerFactory = injector.getInstance(TesterFactory.class);
            Tester tester = testerFactory.create(templateLanguage, language, project);
            TesterProgress progress = new TesterProgressDefault();
            TesterConfig config = new TesterConfig(maxNumberOfTerms, maxTermSize);

            TestResult result = tester.test(config, progress);
            FindResult findResult = result.getFindResult();
            ShrinkResult shrinkResult = result.getShrinkResult();

            if (findResult.found()) {
                print("Found ambiguous sentence after %d terms (%d ms). ", findResult.terms(), findResult.duration());

                if (shrinkResult != null) {
                    print("Shrunk from %d to %d characters (%d ms).\n", findResult.text().length(), shrinkResult.text().length(), shrinkResult.duration());
                } else {
                    print("Unable to shrink.\n");
                }
            } else {
                print("No ambiguous sentence found after %d terms (%d ms).", findResult.terms(), findResult.duration());
            }
        } catch (MetaborgException e) {
            e.printStackTrace();
        }
    }

    private static void print(String format, Object... arguments) {
        System.out.print(String.format(format, arguments));
    }
}
