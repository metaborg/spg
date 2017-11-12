package org.metaborg.spg.sentence;

import org.apache.commons.vfs2.FileObject;
import org.metaborg.core.MetaborgException;
import org.metaborg.core.language.ILanguageImpl;
import org.metaborg.core.project.IProject;
import org.metaborg.core.project.SimpleProjectService;
import org.metaborg.spoofax.core.Spoofax;
import org.spoofax.interpreter.terms.IStrategoTerm;
import org.spoofax.interpreter.terms.ITermFactory;

import java.io.File;
import java.util.Optional;

public class Main {
    public static void main(String[] args) throws Exception {
        try (final Spoofax spoofax = new Spoofax(new SentenceModule())) {
            ILanguageImpl strategoLanguage = loadLanguage(spoofax, new File(args[0]));
            ILanguageImpl objectLanguage = loadLanguage(spoofax, new File(args[1]));
            IProject project = getOrCreateProject(spoofax, new File(args[2]));

            ParseService parseService = spoofax.injector.getInstance(ParseService.class);

            PrettyPrinterFactory prettyPrinterFactory = spoofax.injector.getInstance(PrettyPrinterFactory.class);
            PrettyPrinter prettyPrinter = prettyPrinterFactory.create(objectLanguage, project);

            GeneratorFactory generatorFactory = spoofax.injector.getInstance(GeneratorFactory.class);
            Generator generator = generatorFactory.create(objectLanguage, project, prettyPrinter);

            ITermFactory termFactory = spoofax.termFactoryService.getGeneric();
            ShrinkerFactory shrinkerFactory = spoofax.injector.getInstance(ShrinkerFactory.class);
            Shrinker shrinker = shrinkerFactory.create(objectLanguage, project, prettyPrinter, generator, termFactory, strategoLanguage);

            for (int i = 0; i < 1000; i++) {
                Optional<String> textOpt = generator.generate(1000);

                if (textOpt.isPresent()) {
                    String text = textOpt.get();

                    System.out.println("=== Program ===");
                    System.out.println(text);

                    IStrategoTerm term = parseService.parse(objectLanguage, text);

                    if (parseService.isAmbiguous(term)) {
                        shrink(shrinker, new ShrinkerUnit(term, text));
                    }
                }
            }
        } catch (MetaborgException e) {
            e.printStackTrace();
        }
    }

    public static void shrink(Shrinker shrinker, ShrinkerUnit shrinkerUnit) {
        System.out.println("=== Shrink ==");
        System.out.println(shrinkerUnit.getText());

        shrinker.shrink(shrinkerUnit)
                .findAny()
                .ifPresent(shrunkTerm -> shrink(shrinker, shrunkTerm));
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