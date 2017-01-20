package nl.tudelft.fragments

import java.io.{File, FileOutputStream, PrintWriter}
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

import nl.tudelft.fragments.example.{L1, L2, L3, MiniJava, Tiger}
import nl.tudelft.fragments.spoofax.Language
import org.backuity.clist.{Cli, Command => BaseCommand, arg, opt}
import rx.lang.scala.Observable

import scala.annotation.tailrec

class Generator {
  /**
    * Generate an Observable of programs that emits at most limit programs with
    * interactive and verbose flags.
    *
    * @param sdfPath
    * @param nablPath
    * @param projectPath
    * @param semanticsPath
    * @param limit
    * @param interactive
    * @param verbosity
    * @return
    */
  def generate(sdfPath: String, nablPath: String, projectPath: String, semanticsPath: String, config: Config, limit: Int = -1, interactive: Boolean = false, verbosity: Int = 0): Observable[GenerationResult] =
    generate(Language.load(sdfPath, nablPath, projectPath, semanticsPath), config, limit, interactive, verbosity)

  /**
    * Generate a cold Observable of programs that emits at most n programs.
    *
    * @param language
    * @param config
    * @param limit
    * @param interactive
    * @param verbosity
    * @return
    */
  def generate(language: Language, config: Config, limit: Int, interactive: Boolean, verbosity: Int): Observable[GenerationResult] =
    Observable(subscriber => {
      repeat(limit) {
        if (!subscriber.isUnsubscribed) {
          subscriber.onNext(Synergy.generate(language, config, interactive, verbosity))
        }
      }

      if (!subscriber.isUnsubscribed) {
        subscriber.onCompleted()
      }
    })

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

object Generator {
  /**
    * Entry point when running as CLI. Takes a path to a Spoofax project as
    * argument and outputs a stream of programs.
    *
    * @param args
    */
  def main(args: Array[String]): Unit = {
    Cli.parse(args).withCommand(new Command)(options => {
      val writer = new PrintWriter(
        new FileOutputStream(new File("l3.log"), true)
      )

      new Generator().generate(
        options.sdfPath,
        options.nablPath,
        options.projectPath,
        options.semanticsPath,
        L3.l3Config,
        options.limit,
        options.interactive,
        options.verbosity
      ).subscribe(program => {
        writer.println("===================================")
        writer.println(program)
        writer.println("---")
        writer.println(DateTimeFormatter.ISO_DATE_TIME.format(ZonedDateTime.now()))
        writer.println("-----------------------------------")
        writer.println(program.text)
        writer.println("-----------------------------------")
        writer.flush()

        println(program.text)
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

  var interactive = opt[Boolean](
    description = "Run generator in interactive mode (default: false)",
    default = false
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
