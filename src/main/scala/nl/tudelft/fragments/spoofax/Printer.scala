package nl.tudelft.fragments.spoofax

import javax.inject.Singleton

import com.google.common.collect.Iterables
import org.metaborg.core.project.{IProjectService, SimpleProjectService}
import org.metaborg.spoofax.core.{Spoofax, SpoofaxModule}
import org.spoofax.interpreter.terms.{IStrategoString, IStrategoTerm}

object Printer {
  def print(language: String) = {
    val s = new Spoofax(new SpoofaxModule() {
      override def bindProject() {
        bind(classOf[SimpleProjectService]).in(classOf[Singleton])
        bind(classOf[IProjectService]).to(classOf[SimpleProjectService])
      }
    })

    val languageLocation = s.resourceService.resolve(language)
    val projectLocation = s.resourceService.resolve(language)

    val languageComponents = s.discoverLanguages(projectLocation)

    val component = Iterables.get(languageComponents, 0)
    val languageImpl = Iterables.get(component.contributesTo(), 0)

    val projectService = s.injector.getInstance(classOf[SimpleProjectService])
    projectService.create(projectLocation)

    val project = s.projectService.get(projectLocation)
    val context = s.contextService.get(languageLocation, project, languageImpl)
    val runtime = s.strategoRuntimeService.runtime(component, context)

    (term: IStrategoTerm) => s.strategoCommon.invoke(runtime, term, "pp-debug").asInstanceOf[IStrategoString]
  }
}
