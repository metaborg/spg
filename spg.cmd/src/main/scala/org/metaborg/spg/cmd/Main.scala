package org.metaborg.spg.cmd

import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

import ch.qos.logback.classic.{Level, Logger}
import org.backuity.clist.Cli
import org.metaborg.core.language.LanguageUtils
import org.metaborg.spg.core.Config
import org.metaborg.spoofax.core.Spoofax
import org.slf4j.LoggerFactory
import org.metaborg.spg.core.Generator

object Main extends App {
  val spoofax = new Spoofax(new SPGModule)

  /**
    * Load language at given path.
    *
    * @param path
    */
  def loadLanguage(path: String) = {
    val languageDiscoveryRequest = spoofax.languageDiscoveryService.request(spoofax.resourceService.resolve(path))
    val lutComponents = spoofax.languageDiscoveryService.discover(languageDiscoveryRequest)

    val languages = LanguageUtils.toImpls(lutComponents)

    if (languages.size() == 0) {
      throw new IllegalArgumentException("No language found at path '" + path + "'")
    }

    languages.iterator().next()
  }

  /**
    * Get project at given path.
    *
    * @param path
    */
  def getProject(path: String) = {
    val file = spoofax.resourceService.resolve(path)

    spoofax.projectService.get(file)
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

    val generator = spoofax.injector
      .getInstance(classOf[Generator])

    val config = Config(
      options.semanticsPath,
      options.limit,
      options.fuel,
      options.sizeLimit,
      options.consistency,
      options.throwOnUnresolvable
    )

    val programs = generator.generate(
      loadLanguage(options.projectPath),
      getProject(options.projectPath),
      config
    )

    programs.subscribe(program => {
      println("===================================")
      println(DateTimeFormatter.ISO_DATE_TIME.format(ZonedDateTime.now()))
      println("-----------------------------------")
      println(program)
    })
  })
}
