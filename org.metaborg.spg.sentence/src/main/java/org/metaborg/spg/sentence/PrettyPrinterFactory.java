package org.metaborg.spg.sentence;

import com.google.common.collect.Iterables;
import com.google.inject.Inject;
import org.apache.commons.vfs2.FileObject;
import org.metaborg.core.MetaborgException;
import org.metaborg.core.context.IContext;
import org.metaborg.core.context.IContextService;
import org.metaborg.core.language.ILanguageComponent;
import org.metaborg.core.language.ILanguageImpl;
import org.metaborg.core.project.IProject;
import org.metaborg.spoofax.core.stratego.IStrategoCommon;
import org.metaborg.spoofax.core.stratego.IStrategoRuntimeService;
import org.strategoxt.HybridInterpreter;

public class PrettyPrinterFactory {
  private final IContextService contextService;
  private final IStrategoRuntimeService runtimeService;
  private IStrategoCommon stratego;

  @Inject
  public PrettyPrinterFactory(IContextService contextService, IStrategoRuntimeService runtimeService, IStrategoCommon stratego) {
    this.contextService = contextService;
    this.runtimeService = runtimeService;
    this.stratego = stratego;
  }

  public PrettyPrinter create(ILanguageImpl language, IProject project) throws MetaborgException {
    FileObject languageLocation = Iterables.get(language.locations(), 0);
    IContext context = contextService.getTemporary(languageLocation, project, language);

    ILanguageComponent component = Iterables.get(language.components(), 0);
    HybridInterpreter interpreter = runtimeService.runtime(component, context, false);

    return new PrettyPrinter(stratego, interpreter);
  }
}
