package nl.tudelft.fragments.spoofax

import com.google.common.collect.Iterables
import nl.tudelft.fragments.MainBuilder
import org.metaborg.core.project.SimpleProjectService
import org.spoofax.interpreter.terms.{IStrategoString, IStrategoTerm}

object Printer {
  val s = MainBuilder.spoofax

  def printer(languagePath: String) = {
    // TODO: languageLocation == projectLocation ?
    val languageLocation = s.resourceService.resolve(languagePath)
    val projectLocation = s.resourceService.resolve(languagePath)

    val languageComponents = s.discoverLanguages(projectLocation)
    val component = Iterables.get(languageComponents, 0)
    val languageImpl = Iterables.get(component.contributesTo(), 0)

    val projectService = s.injector.getInstance(classOf[SimpleProjectService])
    projectService.create(projectLocation)

    val project = s.projectService.get(projectLocation)
    val context = s.contextService.get(languageLocation, project, languageImpl)
    val runtime = s.strategoRuntimeService.runtime(component, context, false)

    (term: IStrategoTerm) => s.strategoCommon.invoke(runtime, term, "pp-debug").asInstanceOf[IStrategoString]
  }
}
