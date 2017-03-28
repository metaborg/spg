package org.metaborg.spg.cmd

import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

import ch.qos.logback.classic.{Level, Logger}
import com.google.inject.{AbstractModule, Injector}
import net.codingwell.scalaguice.InjectorExtensions._
import org.backuity.clist.Cli
import org.metaborg.core.language.{ILanguageImpl, LanguageUtils}
import org.metaborg.core.project.{IProject, SimpleProjectService}
import org.metaborg.spg.core.{Config, SemanticGenerator, SemanticGeneratorFactory}
import org.metaborg.spoofax.core.Spoofax
import org.slf4j.LoggerFactory

import scala.util.Random

object Main extends App {
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
    * If the options contains a seed, this method returns an injector that
    * binds Random to an instance of Random with the given seed.
    *
    * @param options
    * @return
    */
  def getInjector(options: Command): Injector = {
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
    * Create a config object from the given command.
    *
    * @param options
    * @return
    */
  def getConfig(options: Command): Config = {
    Config(
      options.limit,
      options.fuel,
      options.sizeLimit
    )
  }

  /**
    * Entry-point of the CLI.
    */
  Cli.parse(args).withCommand(new Command)(options => {
    LoggerFactory
      .getLogger("ROOT").asInstanceOf[Logger]
      .setLevel(Level.toLevel(options.verbosity))

    loadLanguage(options.sdfPath)
    loadLanguage(options.nablPath)

    val injector = getInjector(options)

    /*
    val generator = injector.getInstance(classOf[SemanticGenerator])

    val programs = generator.generate(
      loadLanguage(options.projectPath),
      getOrCreateProject(options.projectPath),
      getConfig(options)
    )
    */

    val generatorFactory = injector.getInstance(classOf[SemanticGeneratorFactory])

    val generator = generatorFactory.create(
      loadLanguage(options.projectPath),
      getOrCreateProject(options.projectPath),
      getConfig(options)
    )

    generator.subscribe(program => {
      println("===================================")
      println(DateTimeFormatter.ISO_DATE_TIME.format(ZonedDateTime.now()))
      println("-----------------------------------")
      println(program)
    })
  })
}
