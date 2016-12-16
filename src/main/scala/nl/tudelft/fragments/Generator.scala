package nl.tudelft.fragments

import java.io.{File, FileOutputStream, PrintWriter}
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

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
  def generate(path: String, config: Config): Observable[GenerationResult] =
    generate(path, config, -1)

  /**
    * Generate an Observable of programs that emits at most n programs.
    *
    * @param path
    * @param n
    * @return
    */
  def generate(path: String, config: Config, n: Int): Observable[GenerationResult] =
    generate(Language.load(path), config, n)

  /**
    * Generate a cold Observable of programs that emits at most n programs.
    *
    * @param language
    * @param n
    * @return
    */
  def generate(language: Language, config: Config, n: Int): Observable[GenerationResult] =
    Observable(subscriber => {
      repeat(n) {
        if (!subscriber.isUnsubscribed) {
          subscriber.onNext(Synergy.generate(language, config))
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
      println("Usage: Generator <path> [limit]")
    } else {
      val path = args(0)
      val limit = args.lift(1).map(_.toInt).getOrElse(-1)

      val writer = new PrintWriter(new FileOutputStream(new File("15-12-2016-morning.log"), true))

      new Generator().generate(path, DefaultConfig, limit).subscribe(program => {
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
