package org.metaborg.spg.sentence.evaluation;

import org.metaborg.core.MetaborgException;
import org.metaborg.core.language.ILanguageImpl;
import org.metaborg.core.project.IProject;
import org.metaborg.spg.sentence.Utils;
import org.metaborg.spg.sentence.ambiguity.*;
import org.metaborg.spg.sentence.guice.SentenceModule;
import org.metaborg.spoofax.core.Spoofax;

import java.util.Arrays;
import java.util.Collection;

public class Main {
    public static void main(String[] args) throws Exception {
        try (Spoofax spoofax = new Spoofax(new SentenceModule())) {
            Collection<Subject> subjects = Arrays.asList(
                    new Subject(
                            "units.appfun",
                            "/Users/martijn/Projects/metaborg-units/org.metaborg.lang.appfunc/target/org.metaborg.lang.appfunc-0.1.0-SNAPSHOT.spoofax-language",
                            "/Users/martijn/Projects/metaborg-units/org.metaborg.lang.appfunc"
                    ),

                    new Subject(
                            "units.mixml",
                            "/Users/martijn/Projects/metaborg-units/org.metaborg.lang.mixml/target/org.metaborg.lang.mixml-0.1.0-SNAPSHOT.spoofax-language",
                            "/Users/martijn/Projects/metaborg-units/org.metaborg.lang.mixml"
                    ),

                    new Subject(
                            "units.sml",
                            "/Users/martijn/Projects/metaborg-units/org.metaborg.lang.sml/target/org.metaborg.lang.sml-0.1.0-SNAPSHOT.spoofax-language",
                            "/Users/martijn/Projects/metaborg-units/org.metaborg.lang.sml"
                    ),
                    new Subject(
                            "units.stsrm",
                            "/Users/martijn/Projects/metaborg-units/org.metaborg.lang.stsrm/target/org.metaborg.lang.stsrm-0.1.0-SNAPSHOT.spoofax-language",
                            "/Users/martijn/Projects/metaborg-units/org.metaborg.lang.stsrm"
                    ),

                    new Subject(
                            "units.units",
                            "/Users/martijn/Projects/metaborg-units/org.metaborg.lang.units/target/org.metaborg.lang.units-0.1.0-SNAPSHOT.spoofax-language",
                            "/Users/martijn/Projects/metaborg-units/org.metaborg.lang.units"
                    ),

                    new Subject(
                            "sl",
                            "/Users/martijn/Projects/metaborg-sl/org.metaborg.lang.sl/target/org.metaborg.lang.sl-2.4.0-SNAPSHOT.spoofax-language",
                            "/Users/martijn/Projects/metaborg-sl/org.metaborg.lang.sl"
                    ),

                    new Subject(
                            "sdf3 demo",
                            "/Users/martijn/Projects/sdf3-demo/sdf3-demo/target/sdf_demo-0.1.0-SNAPSHOT.spoofax-language",
                            "/Users/martijn/Projects/sdf3-demo/sdf3-demo"
                    ),

                    new Subject(
                            "metaborg.calc",
                            "/Users/martijn/Projects/metaborg-calc/org.metaborg.lang.calc/target/org.metaborg.lang.calc-0.1.0-SNAPSHOT.spoofax-language",
                            "/Users/martijn/Projects/metaborg-calc/org.metaborg.lang.calc"
                    ),

                    new Subject(
                            "metaborg.tiger",
                            "/Users/martijn/Projects/metaborg-tiger/org.metaborg.lang.tiger/correct/target/org.metaborg.lang.tiger-0.1.0-SNAPSHOT.spoofax-language",
                            "/Users/martijn/Projects/metaborg-tiger/org.metaborg.lang.tiger/correct"
                    ),

                    new Subject(
                            "metaborg.while",
                            "/Users/martijn/Projects/Whilelang/target/Whilelang-0.1.0-SNAPSHOT.spoofax-language",
                            "/Users/martijn/Projects/Whilelang"
                    ),

                    new Subject(
                            "metaborg.typescript",
                            "/Users/martijn/Projects/metaborg-typescript/typescript/target/typescript-0.1.0-SNAPSHOT.spoofax-language",
                            "/Users/martijn/Projects/metaborg-typescript/typescript"
                    ),

                    new Subject(
                            "metaborg.pascal",
                            "/Users/martijn/Projects/metaborg-pascal/org.metaborg.lang.pascal/target/org.metaborg.lang.pascal-0.1.0-SNAPSHOT.spoofax-language",
                            "/Users/martijn/Projects/metaborg-pascal/org.metaborg.lang.pascal"
                    ),

                    new Subject(
                            "metaborg.js",
                            "/Users/martijn/Projects/metaborg-js/spoofaxJS/target/spoofaxJS-0.1.0-SNAPSHOT.spoofax-language",
                            "/Users/martijn/Projects/metaborg-js/spoofaxJS"
                    ),

                    new Subject(
                            "metaborg.jasmin",
                            "/Users/martijn/Projects/spoofax-jasmin/jasmin/target/jasmin-1.0.0-SNAPSHOT.spoofax-language",
                            "/Users/martijn/Projects/spoofax-jasmin/jasmin"
                    )
            );

            for (Subject subject : subjects) {
                System.out.println("Evaluate " + subject);

                // Warm-up the JVM, then do the actual evaluation
                AmbiguityTesterResult warmupResult = evaluate(spoofax, subject);
                AmbiguityTesterResult result = evaluate(spoofax, subject);

                if (result.foundAmbiguity()) {
                    System.out.println("Found ambiguous sentence after " + result.getTerms() + " terms (" + result.getDuration() + " ms).");
                } else {
                    System.out.println("No sentence found after " + result.getTerms() + " terms (" + result.getDuration() + " ms).");
                }
            }
        } catch (MetaborgException e) {
            e.printStackTrace();
        }
    }

    public static AmbiguityTesterResult evaluate(Spoofax spoofax, Subject subject) throws Exception {
        ILanguageImpl language = Utils.loadLanguage(spoofax, subject.getLanguageFile());
        IProject project = Utils.getOrCreateProject(spoofax, subject.getProjectFile());

        AmbiguityTesterFactory ambiguityTesterFactory = spoofax.injector.getInstance(AmbiguityTesterFactory.class);
        AmbiguityTester ambiguityTester = ambiguityTesterFactory.create();
        AmbiguityTesterProgress progress = new AmbiguityTesterProgressNoop();
        AmbiguityTesterConfig config = new AmbiguityTesterConfig(1000, 100);
        AmbiguityTesterResult result = ambiguityTester.findAmbiguity(language, project, config, progress);

        return result;
    }
}
