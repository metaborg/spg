package org.metaborg.spg.cmd.handler

import com.google.inject.{AbstractModule, Injector}
import org.metaborg.core.language.{ILanguageImpl, LanguageUtils}
import org.metaborg.core.project.{IProject, SimpleProjectService}
import org.metaborg.spg.cmd.SPGModule
import org.metaborg.spg.cmd.command.CommonCommand
import org.metaborg.spoofax.core.Spoofax
import net.codingwell.scalaguice.InjectorExtensions._

import scala.util.Random

trait CommonHandler {
  val spoofax = new Spoofax(new SPGModule)

  /**
    * Load language at given path.
    *
    * @param path
    */
  def loadLanguage(path: String): ILanguageImpl = {
    val languageDiscoveryRequest = spoofax.languageDiscoveryService.request(spoofax.resourceService.resolve(path))
    val lutComponents = spoofax.languageDiscoveryService.discover(languageDiscoveryRequest)

    val languages = LanguageUtils.toImpls(lutComponents)

    if (languages.size() == 0) {
      throw new IllegalArgumentException("No language found at path '" + path + "'")
    }

    languages.iterator().next()
  }

  /**
    * Get project at given path or create a new project if none exists.
    *
    * @param path
    */
  def getOrCreateProject(path: String): IProject = {
    val simpleProjectService = spoofax.injector.instance[SimpleProjectService]
    val resource = spoofax.resourceService.resolve(path)

    Option(simpleProjectService.get(resource)).getOrElse(
      simpleProjectService.create(resource)
    )
  }

  /**
    * Construct an injector based on the given options.
    *
    * If the a seed is set, this method returns an injector that binds
    * scala.util.Random to an instance with the given seed.
    *
    * @param options
    * @return
    */
  def getInjector(options: CommonCommand): Injector = {
    options.seed match {
      case Some(seed) =>
        spoofax.injector.createChildInjector(new AbstractModule() {
          override def configure(): Unit = {
            bind(classOf[Random]).toInstance(new Random(seed))
          }
        })
      case _ =>
        spoofax.injector
    }
  }

  /**
    * Display the generated program.
    *
    * @param program
    */
  def showProgress(program: String): Unit = {
    println(program)
    println("============================")
  }
}
