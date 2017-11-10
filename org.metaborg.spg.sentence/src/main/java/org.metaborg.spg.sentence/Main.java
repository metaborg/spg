package org.metaborg.spg.sentence;

import java.io.File;
import java.util.Optional;

import org.apache.commons.vfs2.FileObject;
import org.metaborg.core.MetaborgException;
import org.metaborg.core.language.ILanguageImpl;
import org.metaborg.core.project.IProject;
import org.metaborg.core.project.SimpleProjectService;
import org.metaborg.spoofax.core.Spoofax;
import org.spoofax.interpreter.terms.IStrategoTerm;

public class Main {
  public static void main(String[] args) throws Exception {
    try (final Spoofax spoofax = new Spoofax()) {
      ILanguageImpl language = loadLanguage(spoofax, new File(args[0]));
      IProject project = getOrCreateProject(spoofax, new File(args[1]));

      PrettyPrinterFactory prettyPrinterFactory = spoofax.injector.getInstance(PrettyPrinterFactory.class);
      PrettyPrinter prettyPrinter = prettyPrinterFactory.create(language, project);

      SentenceGeneratorFactory sentenceGeneratorFactory = spoofax.injector.getInstance(SentenceGeneratorFactory.class);
      SentenceGenerator sentenceGenerator = sentenceGeneratorFactory.create(language, project);

      for (int i = 0; i < 1000; i++) {
        Optional<IStrategoTerm> termOpt = sentenceGenerator.generate();

        if (termOpt.isPresent()) {
          IStrategoTerm term = termOpt.get();

          System.out.println("=== Program ===");
          System.out.println(prettyPrinter.prettyPrint(term));
        }

        // TODO:
        // 1) pretty-print generated term
        // 2) parse pretty-printed term
        // 3) test for ambiguity
        // 4) shrink
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
