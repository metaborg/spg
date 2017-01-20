package nl.tudelft.fragments

import java.io.{File, FileOutputStream, PrintWriter}
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

import nl.tudelft.fragments.example.{L1, L2, L3, MiniJava, Tiger}
import nl.tudelft.fragments.spoofax.Language
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
    // TODO: Replace this by a CLI runner?

    if (args.length == 0) {
      println("Usage: Generator <sdf-path> <nabl-path> <project-path> [options]")
      println("  --semantics <nabl2-path>   Path to the NaBL2 file (relative to the project)")
      println("  --limit <n>                Generate at most n terms")
      println("  --interactive              Interactive mode")
      println("  --verbosity <n>            Verbosity of the output (n = 0, 1, 2)")
    } else {
      def parseOptions(options: List[String], config: Map[String, String] = Map.empty): Map[String, String] = options match {
        case "--semantics" :: semantics :: rest =>
          parseOptions(rest, config + ("semantics" -> semantics))
        case "--limit" :: n :: rest =>
          parseOptions(rest, config + ("limit" -> n))
        case "--interactive" :: rest =>
          parseOptions(rest, config + ("interactive" -> "true"))
        case "--verbosity" :: n :: rest =>
          parseOptions(rest, config + ("verbosity" -> n))
        case Nil =>
          config
        case _ :: rest =>
          parseOptions(rest, config)
      }

      val options = parseOptions(args.drop(1).toList)
      val sdfPath = args(0)
      val nablPath = args(1)
      val projectPath = args(2)
      val semanticsPath = options.get("semantics").map(_.toString).getOrElse("trans/static-semantics.nabl2")
      val limit = options.get("limit").map(_.toInt).getOrElse(-1)
      val interactive = options.get("interactive").map(_.toBoolean).getOrElse(false)
      val verbosity = options.get("verbosity").map(_.toInt).getOrElse(0)

      val writer = new PrintWriter(
        new FileOutputStream(new File("l3.log"), true)
      )

      new Generator().generate(
        sdfPath =
          sdfPath,
        nablPath =
          nablPath,
        projectPath =
          projectPath,
        semanticsPath =
          semanticsPath,
        config =
          L3.l3Config,
        limit =
          limit,
        interactive =
          interactive,
        verbosity =
          verbosity
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
    }
  }
}
