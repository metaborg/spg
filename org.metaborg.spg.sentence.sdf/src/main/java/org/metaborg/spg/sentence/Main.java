package org.metaborg.spg.sentence;

import org.metaborg.core.MetaborgException;
import org.metaborg.core.language.ILanguageImpl;
import org.metaborg.core.project.IProject;
import org.metaborg.spg.sentence.ambiguity.Tester;
import org.metaborg.spg.sentence.ambiguity.TesterConfig;
import org.metaborg.spg.sentence.ambiguity.TesterFactory;
import org.metaborg.spg.sentence.ambiguity.TesterProgressDefault;
import org.metaborg.spg.sentence.ambiguity.result.FindResult;
import org.metaborg.spg.sentence.ambiguity.result.ShrinkResult;
import org.metaborg.spg.sentence.ambiguity.result.TestResult;
import org.metaborg.spg.sentence.guice.SentenceModule;
import org.metaborg.spg.sentence.statistics.Histogram;
import org.metaborg.spoofax.core.Spoofax;
import org.metaborg.spoofax.core.SpoofaxConstants;
import org.metaborg.spoofax.core.shell.CLIUtils;

import com.google.inject.Injector;

public class Main {

    public static void main(String[] args) throws Exception {
        try(final Spoofax spoofax = new Spoofax(new SentenceModule(0))) {
            final CLIUtils cli = new CLIUtils(spoofax);
            cli.loadLanguagesFromPath();
            final ILanguageImpl templateLanguage = cli.getLanguage(SpoofaxConstants.LANG_SDF3_NAME);
            final ILanguageImpl language = cli.loadLanguage(spoofax.resourceService.resolve(args[0]));
            final IProject project = cli.getOrCreateProject(spoofax.resourceService.resolve(args[1]));

            int maxNumberOfTerms = 1000;
            int maxTermSize = 10000;

            Injector injector = spoofax.injector;
            TesterFactory testerFactory = injector.getInstance(TesterFactory.class);
            Tester tester = testerFactory.create(templateLanguage, language, project);
            TesterProgressDefault progress = new TesterProgressDefault();
            TesterConfig config = new TesterConfig(maxNumberOfTerms, maxTermSize);

            TestResult result = tester.test(config, progress);
            FindResult findResult = result.getFindResult();
            ShrinkResult shrinkResult = result.getShrinkResult();

            if(findResult.found()) {
                print("Found ambiguous sentence after %d terms (%d ms). ", findResult.terms(), findResult.duration());

                if(shrinkResult != null) {
                    print("Shrunk from %d to %d characters (%d ms).\n\n", findResult.text().length(),
                            shrinkResult.text().length(), shrinkResult.duration());
                } else {
                    print("Unable to shrink.\n\n");
                }
            } else {
                print("No ambiguous sentence found after %d terms (%d ms).\n\n", findResult.terms(),
                        findResult.duration());
            }

            print("### Statistics ###\n");
            print("%s", new Histogram(progress.getLengths()));
        } catch(MetaborgException e) {
            e.printStackTrace();
        }
    }

    private static void print(String format, Object... arguments) {
        System.out.print(String.format(format, arguments));
    }
}
