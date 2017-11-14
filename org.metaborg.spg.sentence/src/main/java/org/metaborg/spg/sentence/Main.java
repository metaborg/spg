package org.metaborg.spg.sentence;

import org.metaborg.core.MetaborgException;
import org.metaborg.core.language.ILanguageImpl;
import org.metaborg.core.project.IProject;
import org.metaborg.spg.sentence.ambiguity.*;
import org.metaborg.spg.sentence.guice.SentenceModule;
import org.metaborg.spoofax.core.Spoofax;

import java.io.File;

import static org.metaborg.spg.sentence.Utils.getOrCreateProject;
import static org.metaborg.spg.sentence.Utils.loadLanguage;

public class Main {
    public static void main(String[] args) throws Exception {
        try (final Spoofax spoofax = new Spoofax(new SentenceModule())) {
            ILanguageImpl language = loadLanguage(spoofax, new File(args[0]));
            IProject project = getOrCreateProject(spoofax, new File(args[1]));

            AmbiguityTesterFactory ambiguityTesterFactory = spoofax.injector.getInstance(AmbiguityTesterFactory.class);
            AmbiguityTester ambiguityTester = ambiguityTesterFactory.create();
            AmbiguityTesterProgress progress = new AmbiguityTesterProgressDefault();
            AmbiguityTesterConfig config = new AmbiguityTesterConfig(1000, 100);
            AmbiguityTesterResult result = ambiguityTester.findAmbiguity(language, project, config, progress);
            
            if (result.foundAmbiguity()) {
                System.out.println("Found ambiguous sentence after " + result.getTerms() + " terms (" + result.getDuration() + " ms).");
            } else {
                System.out.println("No sentence found after " + result.getTerms() + " terms (" + result.getDuration() + " ms).");
            }
        } catch (MetaborgException e) {
            e.printStackTrace();
        }
    }
}
