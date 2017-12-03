package org.metaborg.spg.sentence.evaluation;

import org.metaborg.core.MetaborgException;
import org.metaborg.core.language.ILanguageImpl;
import org.metaborg.core.project.IProject;
import org.metaborg.spg.sentence.Utils;
import org.metaborg.spg.sentence.ambiguity.*;
import org.metaborg.spoofax.core.Spoofax;

import java.util.Arrays;
import java.util.Collection;

public class Main {
    public static long RANDOM_SEED = 0;

    public static void main(String[] args) throws Exception {
        try (Spoofax spoofax = new Spoofax(new EvaluationModule(), new EvaluationSentenceModule(RANDOM_SEED))) {
            Collection<Subject> subjects = Arrays.asList(
                    new Subject(
                            "units/appfun",
                            "tmp/metaborg-units/org.metaborg.lang.appfunc/target/org.metaborg.lang.appfunc-0.1.0-SNAPSHOT.spoofax-language",
                            "tmp/metaborg-units/org.metaborg.lang.appfunc"
                    ),

                    new Subject(
                            "units/mixml",
                            "tmp/metaborg-units/org.metaborg.lang.mixml/target/org.metaborg.lang.mixml-0.1.0-SNAPSHOT.spoofax-language",
                            "tmp/metaborg-units/org.metaborg.lang.mixml"
                    ),

                    new Subject(
                            "units/sml",
                            "tmp/metaborg-units/org.metaborg.lang.sml/target/org.metaborg.lang.sml-0.1.0-SNAPSHOT.spoofax-language",
                            "tmp/metaborg-units/org.metaborg.lang.sml"
                    ),

                    new Subject(
                            "units/stsrm",
                            "tmp/metaborg-units/org.metaborg.lang.stsrm/target/org.metaborg.lang.stsrm-0.1.0-SNAPSHOT.spoofax-language",
                            "tmp/metaborg-units/org.metaborg.lang.stsrm"
                    ),

                    new Subject(
                            "units/units",
                            "tmp/metaborg-units/org.metaborg.lang.units/target/org.metaborg.lang.units-0.1.0-SNAPSHOT.spoofax-language",
                            "tmp/metaborg-units/org.metaborg.lang.units"
                    ),

                    new Subject(
                            "metaborg.calc",
                            "tmp/metaborg-calc/org.metaborg.lang.calc/target/org.metaborg.lang.calc-0.1.0-SNAPSHOT.spoofax-language",
                            "tmp/metaborg-calc/org.metaborg.lang.calc"
                    ),

                    new Subject(
                            "metaborg.sl",
                            "tmp/metaborg-sl/org.metaborg.lang.sl/target/org.metaborg.lang.sl-2.4.0-SNAPSHOT.spoofax-language",
                            "tmp/metaborg-sl/org.metaborg.lang.sl"
                    ),

                    new Subject(
                            "metaborg.while",
                            "tmp/metaborg-whilelang/org.metaborg.lang.whilelang/target/org.metaborg.lang.whilelang-0.1.0-SNAPSHOT.spoofax-language",
                            "tmp/metaborg-whilelang/org.metaborg.lang.whilelang"
                    ),

                    new Subject(
                            "metaborg.js",
                            "tmp/metaborg-js/spoofaxJS/target/spoofaxJS-0.1.0-SNAPSHOT.spoofax-language",
                            "tmp/metaborg-js/spoofaxJS"
                    ),

                    new Subject(
                            "metaborg.typescript",
                            "tmp/metaborg-typescript/typescript/target/typescript-0.1.0-SNAPSHOT.spoofax-language",
                            "tmp/metaborg-typescript/typescript"
                    ),

                    new Subject(
                            "stratego.typed",
                            "tmp/stratego/org.metaborg.meta.lang.stratego.typed/target/org.metaborg.meta.lang.stratego.typed-0.1.0-SNAPSHOT.spoofax-language",
                            "tmp/stratego/org.metaborg.meta.lang.stratego.typed"
                    ),

                    new Subject(
                            "sdf3 demo",
                            "tmp/sdf3-demo/sdf3-demo/target/sdf_demo-0.1.0-SNAPSHOT.spoofax-language",
                            "tmp/sdf3-demo/sdf3-demo"
                    ),

                    /*new Subject(
                            "metaborg.tiger",
                            "tmp/metaborg-tiger/org.metaborg.lang.tiger/correct/target/org.metaborg.lang.tiger-0.1.0-SNAPSHOT.spoofax-language",
                            "tmp/metaborg-tiger/org.metaborg.lang.tiger/correct"
                    ),*/

                    new Subject(
                            "metaborg.jasmin",
                            "tmp/metaborg-jasmin/jasmin/target/jasmin-1.0.0-SNAPSHOT.spoofax-language",
                            "tmp/metaborg-jasmin/jasmin"
                    ),

                    new Subject(
                            "metaborg.pascal",
                            "tmp/metaborg-pascal/org.metaborg.lang.pascal/target/org.metaborg.lang.pascal-0.1.0-SNAPSHOT.spoofax-language",
                            "tmp/metaborg-pascal/org.metaborg.lang.pascal"
                    ),

                    new Subject(
                            "metaborg.llir",
                            "tmp/metaborg-llir/metaborg-llir/target/metaborgllir-0.1.0-SNAPSHOT.spoofax-language",
                            "tmp/metaborg-llir/metaborg-llir"
                    ),

                    new Subject(
                            "metaborg.smalltalk",
                            "tmp/metaborg-smalltalk/Smalltalk/target/Smalltalk-0.1.0-SNAPSHOT.spoofax-language",
                            "tmp/metaborg-smalltalk/Smalltalk"
                    ),

                    /*new Subject(
                            "metaborg.coq",
                            "tmp/metaborg-coq/org.metaborg.lang.coq/target/org.metaborg.lang.coq-2.4.0-SNAPSHOT.spoofax-language",
                            "tmp/metaborg-coq/org.metaborg.lang.coq"
                    ),*/

                    new Subject(
                            "metaborg.grace",
                            "tmp/metaborg-grace/grace/target/grace-0.3.0-SNAPSHOT.spoofax-language",
                            "tmp/metaborg-grace/grace"
                    )/*,

                    new Subject(
                            "java-front-17",
                            "tmp/java-front-17/lang.java/target/lang.java-1.0.0-SNAPSHOT.spoofax-language",
                            "tmp/java-front-17/lang.java"
                    ),*/

                    /*new Subject(
                            "java-front-18",
                            "tmp/java-front-18/lang.java/target/lang.java-1.0.0-SNAPSHOT.spoofax-language",
                            "tmp/java-front-18/lang.java"
                    )*/

                    /*
                    Also ambiguous:
                    - metaborg-pcf (uses longest-match, bit iffy)
                    - IceDust
                    - SLDE/GreenMarl (private)
                    */
            );

            for (Subject subject : subjects) {
                System.out.println("Evaluate " + subject);

                // Load language
                ILanguageImpl language = Utils.loadLanguage(spoofax, subject.getLanguageFile());
                IProject project = Utils.getOrCreateProject(spoofax, subject.getProjectFile());

                // Warm-up the JVM, then do the actual evaluation
                AmbiguityTesterResult warmupResult = evaluate(spoofax, language, project);
                AmbiguityTesterResult result = evaluate(spoofax, language, project);

                if (result.foundAmbiguity()) {
                    System.out.print("Found ambiguous sentence ");
                    System.out.print("after " + result.getTerms() + " terms ");
                    System.out.print("(" + result.getDuration() + " ms) ");
                    System.out.print("of size " + result.getAmbiguousText().length() + " chars.\n");
                } else {
                    System.out.println("No sentence found after " + result.getTerms() + " sentences (" + result.getDuration() + " ms).");
                }
            }

            System.out.println("Evaluation done.");
        } catch (MetaborgException e) {
            e.printStackTrace();
        }
    }

    public static AmbiguityTesterResult evaluate(Spoofax spoofax, ILanguageImpl languageImpl, IProject project) throws Exception {
        int maxNumberOfTerms = 1000;
        int maxTermSize = 1000;

        AmbiguityTesterFactory ambiguityTesterFactory = spoofax.injector.getInstance(AmbiguityTesterFactory.class);
        AmbiguityTester ambiguityTester = ambiguityTesterFactory.create();
        AmbiguityTesterProgress progress = new AmbiguityTesterProgressNoop();
        AmbiguityTesterConfig config = new AmbiguityTesterConfig(maxNumberOfTerms, maxTermSize);

        return ambiguityTester.findAmbiguity(languageImpl, project, config, progress);
    }
}
