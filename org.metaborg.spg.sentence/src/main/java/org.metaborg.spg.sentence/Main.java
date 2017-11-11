package org.metaborg.spg.sentence;

import java.io.File;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.stream.Stream;

import org.apache.commons.vfs2.FileObject;
import org.metaborg.core.MetaborgException;
import org.metaborg.core.language.ILanguageImpl;
import org.metaborg.core.project.IProject;
import org.metaborg.core.project.SimpleProjectService;
import org.metaborg.spoofax.core.Spoofax;
import org.spoofax.interpreter.terms.IStrategoTerm;

public class Main {
  public static Random random = new Random(0);

  public static void main(String[] args) throws Exception {
    try (final Spoofax spoofax = new Spoofax(new Module())) {
      ILanguageImpl strategoLanguage = loadLanguage(spoofax, new File(args[0]));
      ILanguageImpl objectLanguage = loadLanguage(spoofax, new File(args[1]));
      IProject project = getOrCreateProject(spoofax, new File(args[2]));

      ParseService parseService = spoofax.injector.getInstance(ParseService.class);

      PrettyPrinterFactory prettyPrinterFactory = spoofax.injector.getInstance(PrettyPrinterFactory.class);
      PrettyPrinter prettyPrinter = prettyPrinterFactory.create(objectLanguage, project);

      GeneratorFactory generatorFactory = spoofax.injector.getInstance(GeneratorFactory.class);
      Generator generator = generatorFactory.create(objectLanguage, project);

      ShrinkerFactory shrinkerFactory = spoofax.injector.getInstance(ShrinkerFactory.class);
      Shrinker shrinker = shrinkerFactory.create(objectLanguage, project, prettyPrinter, generator, spoofax.termFactoryService.getGeneric(), strategoLanguage);

      for (int i = 0; i < 1000; i++) {
        Optional<IStrategoTerm> termOpt = generator.generate(1000);

        if (termOpt.isPresent()) {
          IStrategoTerm term = termOpt.get();
          String text = prettyPrinter.prettyPrint(term);
          System.out.println("=== Program ===");
          System.out.println(text);

          IStrategoTerm parsedTerm = parseService.parse(objectLanguage, text);

          if (parseService.isAmbiguous(parsedTerm)) {
            System.out.println("=== Ambiguous ===");
            System.out.println(parsedTerm);

            shrink(shrinker, parsedTerm);
          }
        }
      }
    } catch (MetaborgException e) {
      e.printStackTrace();
    }
  }

  public static void shrink(Shrinker shrinker, IStrategoTerm term) {
    System.out.println("=== Shrink ==");
    System.out.println(term);

    Stream<IStrategoTerm> shrunkTerms = shrinker.shrink(term);
    Optional<IStrategoTerm> ShrunkTermOpt = shrunkTerms.findAny();

    ShrunkTermOpt.ifPresent(shrunkTerm -> {
      shrink(shrinker, shrunkTerm);
    });
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
