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
    * Generate an Observable of programs that emits programs ad infinitum.
    *
    * @param path
    * @return
    */
  def generate(path: String, config: Config, verbose: Boolean): Observable[GenerationResult] =
    generate(path, config, -1, verbose)

  /**
    * Generate an Observable of programs that emits at most n programs.
    *
    * @param path
    * @param n
    * @return
    */
  def generate(path: String, config: Config, n: Int, verbose: Boolean): Observable[GenerationResult] =
    generate(Language.load(path), config, n, verbose)

  /**
    * Generate a cold Observable of programs that emits at most n programs.
    *
    * @param language
    * @param n
    * @return
    */
  def generate(language: Language, config: Config, n: Int, verbose: Boolean): Observable[GenerationResult] =
    Observable(subscriber => {
      repeat(n) {
        if (!subscriber.isUnsubscribed) {
          subscriber.onNext(Synergy.generate(language, config, verbose))
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
      println("Usage: Generator <path> [options]")
      println("  -l --limit <n>   Generate at most n terms")
      println("  -v --verbose     Verbose output")
    } else {
      def parseOptions(options: List[String], config: Map[String, String] = Map.empty): Map[String, String] = options match {
        case Nil =>
          config
        case "--limit" :: limit :: rest =>
          parseOptions(rest, config + ("limit" -> limit))
        case "--verbose" :: rest =>
          parseOptions(rest, config + ("verbose" -> "true"))
      }

      val path = args(0)
      val options = parseOptions(args.drop(1).toList)
      val limit = options.get("limit").map(_.toInt).getOrElse(-1)
      val verbose = options.get("verbose").map(_.toBoolean).getOrElse(false)

      val writer = new PrintWriter(new FileOutputStream(new File("22-12-2016-morning.log"), true))

      new Generator().generate(path, /*DefaultConfig*/ Tiger.tigerConfig /*MiniJava.miniJavaConfig*/, limit, verbose).subscribe(program => {
        writer.println("===================================")
        writer.println(DateTimeFormatter.ISO_DATE_TIME.format(ZonedDateTime.now()))
        writer.println("-----------------------------------")
        writer.println(program)
        writer.println("-----------------------------------")
        writer.flush()

        println(program.text)
        println("-----------------------------------")
      })
    }
  }
}
