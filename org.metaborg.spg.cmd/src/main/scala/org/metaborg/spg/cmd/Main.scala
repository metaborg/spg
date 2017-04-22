package org.metaborg.spg.cmd

import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

import ch.qos.logback.classic.{Level, Logger}
import com.google.inject.{AbstractModule, Injector}
import com.typesafe.scalalogging.LazyLogging
import net.codingwell.scalaguice.InjectorExtensions._
import org.backuity.clist.Cli
import org.metaborg.core.language.{ILanguageImpl, LanguageUtils}
import org.metaborg.core.project.{IProject, SimpleProjectService}
import org.metaborg.core.syntax.IParseUnit
import org.metaborg.spg.core.spoofax.ParseService
import org.metaborg.spg.core.{Config, SemanticGenerator, SemanticGeneratorFactory, SyntaxGeneratorFactory, SyntaxShrinker}
import org.metaborg.spoofax.core.Spoofax
import org.metaborg.spoofax.core.unit.ISpoofaxParseUnit
import org.slf4j.LoggerFactory

import scala.util.Random

object Main extends App with LazyLogging {
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
    * Shrink the given program until it cannot be shrunk any more.
    *
    * @param shrinker
    * @param parseService
    * @param parseUnit
    * @param language
    */
  def multiShrink(shrinker: SyntaxShrinker, parseService: ParseService, parseUnit: ISpoofaxParseUnit, language: ILanguageImpl): ISpoofaxParseUnit = {
    logger.info(parseUnit.input().text())

    val shrunkPrograms = shrinker.shrink(parseUnit.input().text())

    shrunkPrograms
      .map(parseService.parse(language, _))
      .filter(parseService.isAmbiguous)
      .map(multiShrink(shrinker, parseService, _, language))
      .firstOrElse(parseUnit)
      .toBlocking
      .single
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

    val generatorFactory = injector.getInstance(classOf[SyntaxGeneratorFactory])

    val language = generatorFactory.loadLanguage(
      lut =
        loadLanguage(options.projectPath),
      project =
        getOrCreateProject(options.projectPath)
    )

    val generator = generatorFactory.create(
      language =
        language,
      config =
        getConfig(options)
    )

    val shrinker = new SyntaxShrinker(generator, injector.getInstance(classOf[ParseService]), language)(new Random(0))

    generator.generate().subscribe(_ match {
      case program => {
        logger.info("===================================")
        logger.info(program)

        // TODO: Move this stuff, it's valuable!
        try {
          val parseService = injector.getInstance(classOf[ParseService])
          val parseUnit = parseService.parse(language.implementation, program)

          if (parseService.isAmbiguous(parseUnit)) {
            logger.info("The program is ambiguous, go shrink it.")

            val shrunken = multiShrink(shrinker, parseService, parseUnit, language.implementation)

            logger.info(shrunken.input().text())

            System.exit(42)
          }
        } catch {
          case e: Exception =>
            println(e)
        }
      }
    })
  })
}
