package org.metaborg.spg.cmd.handler

import com.typesafe.scalalogging.LazyLogging
import org.metaborg.spg.cmd.command.TermCommand
import org.metaborg.spg.core.{Config, SemanticGeneratorFactory}

class TermHandler(command: TermCommand) extends CommonHandler with LazyLogging {
  loadLanguage(command.sdfPath)
  loadLanguage(command.nablPath)

  val injector = getInjector(command)

  val generatorFactory = injector.getInstance(classOf[SemanticGeneratorFactory])

  val config = Config(
    command.limit,
    command.fuel,
    command.sizeLimit
  )

  val generator = generatorFactory.create(
    lut =
      loadLanguage(command.projectPath),
    project =
      getOrCreateProject(command.projectPath),
    config =
      config
  )

  generator
    .doOnNext(showProgress)
    .subscribe()
}
