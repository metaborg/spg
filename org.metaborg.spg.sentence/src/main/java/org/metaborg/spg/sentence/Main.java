package org.metaborg.spg.sentence;

import org.apache.commons.vfs2.FileObject;
import org.metaborg.core.MetaborgException;
import org.metaborg.core.language.ILanguageImpl;
import org.metaborg.core.project.IProject;
import org.metaborg.core.project.SimpleProjectService;
import org.metaborg.spg.sentence.ambiguity.AmbiguityTester;
import org.metaborg.spg.sentence.ambiguity.AmbiguityTesterConfig;
import org.metaborg.spg.sentence.ambiguity.AmbiguityTesterProgress;
import org.metaborg.spg.sentence.ambiguity.AmbiguityTesterResult;
import org.metaborg.spg.sentence.generator.Generator;
import org.metaborg.spg.sentence.generator.GeneratorFactory;
import org.metaborg.spg.sentence.parser.ParseService;
import org.metaborg.spg.sentence.printer.Printer;
import org.metaborg.spg.sentence.printer.PrinterFactory;
import org.metaborg.spg.sentence.shrinker.Shrinker;
import org.metaborg.spg.sentence.shrinker.ShrinkerFactory;
import org.metaborg.spoofax.core.Spoofax;
import org.metaborg.spoofax.core.terms.ITermFactoryService;
import org.spoofax.interpreter.terms.ITermFactory;

import java.io.File;

public class Main {
    public static void main(String[] args) throws Exception {
        try (final Spoofax spoofax = new Spoofax()) {
            ILanguageImpl language = loadLanguage(spoofax, new File(args[0]));
            IProject project = getOrCreateProject(spoofax, new File(args[1]));

            ParseService parseService = spoofax.injector.getInstance(ParseService.class);

            PrinterFactory printerFactory = spoofax.injector.getInstance(PrinterFactory.class);
            Printer printer = printerFactory.create(language, project);

            GeneratorFactory generatorFactory = spoofax.injector.getInstance(GeneratorFactory.class);
            Generator generator = generatorFactory.create(language, project, printer);

            ITermFactoryService termFactoryService = spoofax.termFactoryService;
            ITermFactory termFactory = spoofax.termFactoryService.getGeneric();

            ShrinkerFactory shrinkerFactory = spoofax.injector.getInstance(ShrinkerFactory.class);
            Shrinker shrinker = shrinkerFactory.create(language, project, printer, generator, termFactory);

            AmbiguityTester ambiguityTester = new AmbiguityTester(
                    parseService,
                    termFactoryService,
                    printerFactory,
                    generatorFactory,
                    shrinkerFactory
            );

            AmbiguityTesterProgress progress = new AmbiguityTesterProgress() {
                @Override
                public void sentenceGenerated(String text) {
                    System.out.println("=== Program ===");
                    System.out.println(text);
                }

                @Override
                public void sentenceShrinked(String text) {
                    System.out.println("=== Shrink ==");
                    System.out.println(text);
                }
            };

            AmbiguityTesterConfig config = new AmbiguityTesterConfig(1000, 100);

            AmbiguityTesterResult ambiguityTesterResult = ambiguityTester.findAmbiguity(language, project, config, progress);

            if (ambiguityTesterResult.foundAmbiguity()) {
                System.out.println("Found ambiguous sentence after " + ambiguityTesterResult.getTerms() + " terms (" + ambiguityTesterResult.getDuration() + " ms).");
            } else {
                System.out.println("No sentence found after " + ambiguityTesterResult.getTerms() + " terms (" + ambiguityTesterResult.getDuration() + " ms).");
            }
        } catch (MetaborgException e) {
            e.printStackTrace();
        }
    }

    public static ILanguageImpl loadLanguage(Spoofax spoofax, File file) throws MetaborgException {
        FileObject languageLocation = spoofax.resourceService.resolve(file);

        return spoofax.languageDiscoveryService.languageFromArchive(languageLocation);
    }

    public static IProject getOrCreateProject(Spoofax spoofax, File file) throws MetaborgException {
        SimpleProjectService simpleProjectService = spoofax.injector.getInstance(SimpleProjectService.class);
        FileObject resource = spoofax.resourceService.resolve(file);
        IProject project = simpleProjectService.get(resource);

        if (project == null) {
            return simpleProjectService.create(resource);
        } else {
            return project;
        }
    }
}
