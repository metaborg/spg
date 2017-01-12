package nl.tudelft.fragments

import java.io.{File, FileOutputStream, PrintWriter}
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

import nl.tudelft.fragments.example.{MiniJava, Tiger}
import nl.tudelft.fragments.spoofax.Language
import rx.lang.scala.Observable

import scala.annotation.tailrec

class Generator {
  /**
    * Generate an Observable of programs that emits at most limit programs with
    * interactive and verbose flags.
    *
    * @param projectPath
    * @param semanticsPath
    * @param limit
    * @param interactive
    * @param verbose
    * @return
    */
  def generate(projectPath: String, semanticsPath: String, config: Config, limit: Int = -1, interactive: Boolean = false, verbose: Boolean = false): Observable[GenerationResult] =
    generate(Language.load(projectPath, semanticsPath), config, limit, interactive, verbose)

  /**
    * Generate a cold Observable of programs that emits at most n programs.
    *
    * @param language
    * @param config
    * @param limit
    * @param interactive
    * @param verbose
    * @return
    */
  def generate(language: Language, config: Config, limit: Int, interactive: Boolean, verbose: Boolean): Observable[GenerationResult] =
    Observable(subscriber => {
      repeat(limit) {
        if (!subscriber.isUnsubscribed) {
          subscriber.onNext(Synergy.generate(language, config, interactive, verbose))
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
    if (args.length == 0) {
      println("Usage: Generator <project-path> [options]")
      println("  --semantics <nabl2-path>   Path to the NaBL2 file (relative to the project)")
      println("  --limit <n>                Generate at most n terms")
      println("  --interactive              Interactive mode")
      println("  --verbose                  Verbose output")
    } else {
      def parseOptions(options: List[String], config: Map[String, String] = Map.empty): Map[String, String] = options match {
        case "--semantics" :: semantics :: rest =>
          parseOptions(rest, config + ("semantics" -> semantics))
        case "--limit" :: n :: rest =>
          parseOptions(rest, config + ("limit" -> n))
        case "--interactive" :: rest =>
          parseOptions(rest, config + ("interactive" -> "true"))
        case "--verbose" :: rest =>
          parseOptions(rest, config + ("verbose" -> "true"))
        case Nil =>
          config
        case _ :: rest =>
          parseOptions(rest, config)
      }

      val options = parseOptions(args.drop(1).toList)
      val projectPath = args(0)
      val semanticsPath = options.get("semantics").map(_.toString).getOrElse("trans/static-semantics.nabl2")
      val limit = options.get("limit").map(_.toInt).getOrElse(-1)
      val interactive = options.get("interactive").map(_.toBoolean).getOrElse(false)
      val verbose = options.get("verbose").map(_.toBoolean).getOrElse(false)

      val writer = new PrintWriter(new FileOutputStream(new File("05-01-2017-mutant-3-1.log"), true))

      new Generator().generate(projectPath, semanticsPath, /*DefaultConfig*/ Tiger.tigerConfig /*MiniJava.miniJavaConfig*/, limit, interactive, verbose).subscribe(program => {
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
