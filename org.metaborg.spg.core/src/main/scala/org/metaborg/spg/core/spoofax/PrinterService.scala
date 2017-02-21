package org.metaborg.spg.core.spoofax

import com.google.common.collect.Iterables
import com.google.inject.Inject
import org.metaborg.core.context.IContextService
import org.metaborg.core.language.ILanguageImpl
import org.metaborg.core.project.IProject
import org.metaborg.spoofax.core.stratego.{IStrategoCommon, IStrategoRuntimeService}
import org.spoofax.interpreter.terms.{IStrategoString, IStrategoTerm}

class PrinterService @Inject() (val strategoCommon: IStrategoCommon, val strategoRuntimeService: IStrategoRuntimeService, val contextService: IContextService) {
  /**
    * Get a pretty printer for ATerms in the given language.
    *
    * @param languageImpl
    * @param project
    * @return
    */
  def getPrinter(languageImpl: ILanguageImpl, project: IProject): (IStrategoTerm => String) = {
    val languageLocation = Iterables.get(languageImpl.locations(), 0)
    val component = Iterables.get(languageImpl.components(), 0)

    val context = contextService.getTemporary(languageLocation, project, languageImpl)
    val runtime = strategoRuntimeService.runtime(component, context, false)

    (term: IStrategoTerm) => {
      strategoCommon.invoke(runtime, term, "pp-debug").asInstanceOf[IStrategoString].stringValue()
    }
  }
}
