package org.metaborg.spg

import java.io.{File, FileOutputStream, PrintWriter}
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

import ch.qos.logback.classic.{Level, Logger}
import org.backuity.clist.{Cli, arg, opt, Command => BaseCommand}
import org.metaborg.spg.spoofax.Language
import org.slf4j.LoggerFactory
import rx.lang.scala.Observable

import scala.annotation.tailrec

class GeneratorEntryPoint {
  /**
    * Create a cold Observable that emits at most limit programs.
    *
    * @param sdfPath
    * @param nablPath
    * @param projectPath
    * @param semanticsPath
    * @param config
    * @return
    */
  def generate(sdfPath: String, nablPath: String, projectPath: String, semanticsPath: String, config: Config): Observable[String] = {
    generate(Language.load(sdfPath, nablPath, projectPath, semanticsPath), config)
  }

  /**
    * Create a cold Observable that emits at most limit programs.
    *
    * @param language
    * @param config
    * @return
    */
  def generate(language: Language, config: Config): Observable[String] = {
    Observable(subscriber => {
      repeat(config.limit) {
        if (!subscriber.isUnsubscribed) {
          subscriber.onNext(Generator.generate(language, config))
        }
      }

      if (!subscriber.isUnsubscribed) {
        subscriber.onCompleted()
      }
    })
  }

  /**
    * Repeat the function `f` for `n` times. If `n` is negative, the function
    * is repeated ad infinitum.
    *
    * @param n
    * @param f
    * @tparam A
    */
  @tailrec final def repeat[A](n: Int)(f: => A): Unit = n match {
    case 0 =>
      // Noop
    case _ =>
      f; repeat(n - 1)(f)
  }
}

object GeneratorEntryPoint {
  /**
    * Entry point when running as CLI. Takes a path to a Spoofax project as
    * argument and outputs a stream of programs.
    *
    * @param args
    */
  def main(args: Array[String]): Unit = {
    Cli.parse(args).withCommand(new Command)(options => {
      val rootLogger = LoggerFactory.getLogger("ROOT").asInstanceOf[Logger]
      rootLogger.setLevel(Level.toLevel(options.verbosity))

      val writer = new PrintWriter(
        new FileOutputStream(new File("mj.log"), true)
      )

      new GeneratorEntryPoint().generate(
        options.sdfPath,
        options.nablPath,
        options.projectPath,
        options.semanticsPath,
        Config(
          options.limit,
          options.fuel,
          options.sizeLimit
        )
      ).subscribe(program => {
        writer.println("===================================")
        writer.println(DateTimeFormatter.ISO_DATE_TIME.format(ZonedDateTime.now()))
        writer.println("-----------------------------------")
        writer.println(program)
        writer.flush()

        println("===================================")
        println(DateTimeFormatter.ISO_DATE_TIME.format(ZonedDateTime.now()))
        println("-----------------------------------")
        println(program)
      })
    })
  }
}

class Command extends BaseCommand(name = "generator", description = "Generate random well-formed terms") {
  var semanticsPath = opt[String](
    description = "Path to the static semantics specification (default: trans/static-semantics.nabl2)",
    default = "trans/static-semantics.nabl2"
  )

  var limit = opt[Int](
    description = "Number of terms to generate (default: -1)",
    default = -1
  )

  var fuel = opt[Int](
    description = "Fuel provided to the backtracker (default: 400)",
    default = 400
  )

  var sizeLimit = opt[Int](
    description = "Maximum size of terms to generate (default: 60)",
    default = 60
  )

  var verbosity = opt[String](
    description = "Verbosity of the output as log level (default: ERROR)",
    default = "ERROR"
  )

  var sdfPath = arg[String](
    description = "Path to the SDF language implementation archive"
  )

  var nablPath = arg[String](
    description = "Path to the NaBL2 language implementation archive"
  )

  var projectPath = arg[String](
    description = "Path to the Spoofax project of the language to generate terms for"
  )
}
