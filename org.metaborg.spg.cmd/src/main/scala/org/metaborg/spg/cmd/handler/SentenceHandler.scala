package org.metaborg.spg.cmd.handler

import com.typesafe.scalalogging.LazyLogging
import org.metaborg.core.language.ILanguageImpl
import org.metaborg.spg.cmd.command.SentenceCommand
import org.metaborg.spg.core.spoofax.ParseService
import org.metaborg.spg.core.{Config, SyntaxGeneratorFactory, SyntaxShrinker}
import org.metaborg.spoofax.core.unit.ISpoofaxParseUnit

import scala.util.Random

class SentenceHandler(command: SentenceCommand) extends CommonHandler with LazyLogging {
  /**
    * Shrink the given ambiguous program until it cannot be shrunk any further
    * while still triggering the ambiguity.
    *
    * @param shrinker
    * @param parseService
    * @param parseUnit
    * @param language
    */
  def shrinkStar(shrinker: SyntaxShrinker, parseService: ParseService, parseUnit: ISpoofaxParseUnit, language: ILanguageImpl): ISpoofaxParseUnit = {
    logger.info(s"Shrunk to ${parseUnit.input().text()}")

    val shrunkPrograms = shrinker.shrink(parseUnit.input().text())

    shrunkPrograms
      .map(parseService.parse(language, _))
      .filter(parseService.isAmbiguous)
      .map(shrinkStar(shrinker, parseService, _, language))
      .firstOrElse(parseUnit)
      .toBlocking
      .single
  }

  loadLanguage(command.sdfPath)
  loadLanguage(command.nablPath)

  val injector = getInjector(command)

  val generatorFactory = injector.getInstance(classOf[SyntaxGeneratorFactory])

  val parseService = injector.getInstance(classOf[ParseService])

  val language = generatorFactory.loadLanguage(
    lut =
      loadLanguage(command.projectPath),
    project =
      getOrCreateProject(command.projectPath)
  )

  val config = Config(
    limit =
      command.limit,
    sizeLimit =
      command.sizeLimit,
    fuel =
      0
  )

  val generator = generatorFactory.create(
    language =
      language,
    config =
      config
  )

  val shrinker = new SyntaxShrinker(generator, parseService, language)(new Random(0))

  val programs = generator
    .generate()
    .doOnNext(showProgress)

  if (command.ambiguity) {
    programs
      .map(parseService.parse(language.implementation, _))
      .filter(parseService.isAmbiguous)
      .map(shrinkStar(shrinker, parseService, _, language.implementation))
      .map(_.input().text())
      .doOnNext(showProgress)
      .first
      .subscribe()
  } else {
    programs
      .subscribe()
  }
}
