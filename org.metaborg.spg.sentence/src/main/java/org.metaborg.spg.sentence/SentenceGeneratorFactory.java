package org.metaborg.spg.sentence;

import java.io.File;
import java.util.Collections;
import java.util.List;

import com.google.inject.Inject;
import org.metaborg.core.language.ILanguageImpl;
import org.metaborg.core.project.IProject;
import org.metaborg.core.resource.IResourceService;
import org.metaborg.sdf2table.grammar.NormGrammar;
import org.metaborg.sdf2table.io.GrammarReader;
import org.metaborg.spoofax.core.build.SpoofaxCommonPaths;
import org.metaborg.spoofax.core.terms.ITermFactoryService;
import org.spoofax.interpreter.terms.ITermFactory;

public class SentenceGeneratorFactory {
  private final IResourceService resourceService;
  private final ITermFactoryService termFactoryService;

  @Inject
  public SentenceGeneratorFactory(IResourceService resourceService, ITermFactoryService termFactoryService) {
    this.resourceService = resourceService;
    this.termFactoryService = termFactoryService;
  }

  public SentenceGenerator create(ILanguageImpl language, IProject project) throws Exception {
    ITermFactory termFactory = termFactoryService.getGeneric();
    GrammarReader grammarReader = new GrammarReader(termFactory);
    SpoofaxCommonPaths spoofaxCommonPaths = new SpoofaxCommonPaths(project.location());

    File syntaxMainFile = getSyntaxMainFile(spoofaxCommonPaths, language);
    List<String> syntaxPath = getSyntaxPath(spoofaxCommonPaths);
    NormGrammar grammar = grammarReader.readGrammar(syntaxMainFile, syntaxPath);

    return new SentenceGenerator(termFactory, grammar);
  }

  protected File getSyntaxMainFile(SpoofaxCommonPaths spoofaxCommonPaths, ILanguageImpl language) {
    return resourceService.localFile(spoofaxCommonPaths.syntaxSrcGenMainNormFile(language.id().id));
  }

  protected File getSyntaxDirectory(SpoofaxCommonPaths spoofaxCommonPaths) {
    return resourceService.localFile(spoofaxCommonPaths.syntaxSrcGenDir());
  }

  protected List<String> getSyntaxPath(SpoofaxCommonPaths spoofaxCommonPaths) {
    File syntaxDirectory = getSyntaxDirectory(spoofaxCommonPaths);

    return Collections.singletonList(syntaxDirectory.getAbsolutePath());
  }
}
