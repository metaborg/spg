package nl.tudelft.fragments

import java.io.{File, FileOutputStream, PrintWriter}
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

import ch.qos.logback.classic.{Level, Logger}
import nl.tudelft.fragments.spoofax.Language
import org.backuity.clist.{Cli, arg, opt, Command => BaseCommand}
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
    * @param limit
    * @return
    */
  def generate(sdfPath: String, nablPath: String, projectPath: String, semanticsPath: String, limit: Int = -1, config: Config): Observable[String] = {
    generate(Language.load(sdfPath, nablPath, projectPath, semanticsPath), limit, config)
  }

  /**
    * Create a cold Observable that emits at most limit programs.
    *
    * @param language
    * @param config
    * @param limit
    * @return
    */
  def generate(language: Language, limit: Int, config: Config): Observable[String] = {
    Observable(subscriber => {
      repeat(limit) {
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
    // Stop
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

      options.verbosity match {
        case 2 =>
          rootLogger.setLevel(Level.DEBUG)
        case 1 =>
          rootLogger.setLevel(Level.INFO)
        case _ =>
          rootLogger.setLevel(Level.WARN)
      }

      val writer = new PrintWriter(
        new FileOutputStream(new File("l2.log"), true)
      )

      new GeneratorEntryPoint().generate(
        options.sdfPath,
        options.nablPath,
        options.projectPath,
        options.semanticsPath,
        options.limit,
        DefaultConfig
      ).subscribe(program => {
        writer.println("===================================")
        writer.println(program)
        writer.println("---")
        writer.println(DateTimeFormatter.ISO_DATE_TIME.format(ZonedDateTime.now()))
        writer.println("-----------------------------------")
        writer.flush()

        println(program)
        println("-----------------------------------")
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

  var verbosity = opt[Int](
    description = "Verbosity of the output (default: 0)",
    default = 0
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
