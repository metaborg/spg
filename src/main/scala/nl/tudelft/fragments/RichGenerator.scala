package nl.tudelft.fragments

import nl.tudelft.fragments.spoofax.Language
import rx.lang.scala.Observable

import scala.collection.mutable

/**
  * A generator that collects statistics on the terms it generates.
  */
class RichGenerator {
  /**
    * Returns a cold observable that emits the running averages.
    *
    * @param path
    * @param limit
    * @return
    */
  def generate(path: String, config: Config, limit: Int): Observable[mutable.Map[String, Int]] =
    generate(Language.load(path), config, limit)

  def generate(language: Language, config: Config, limit: Int): Observable[mutable.Map[String, Int]] = {
    val statistics = mutable.Map[String, Int](
      language.constructors.map(cons => (cons, 0)): _*
    )

    new Generator().generate(language, config, limit).map(result => {
      // Constructors
      val appls = result.rule.pattern.collect {
        case t@TermAppl(_, _) =>
          List(t)
        case _ =>
          Nil
      }

      val constructors = appls.map {
        case TermAppl(cons, _) =>
          cons
      }

      val groups = constructors.group.toList.map {
        case (cons, conss) =>
          (cons, conss.length)
      }

      for ((cons, length) <- groups) {
        statistics.update(cons, statistics(cons) + length)
      }

      /*
      // Distribution of terms
      println("## Distribution of terms")

      val grouped = states.groupByWith(_.pattern, Pattern.equivalence).toList
        // Bind the size of the group
        .map {
        case (pattern, states) =>
          (states.size, pattern, states)
      }
        // Sort descending by size
        .sortBy {
        case (size, pattern, states) =>
          -size
      }

      grouped.foreach { case (size, pattern, states) =>
        println(s"${size}x $pattern")
      }

      println(s"${grouped.size} groups")
      */

      /*
      // General
      // Term size
      val termSizes = states.map(state => state.pattern.size)

      // Number of name resolutions
      val resolutionCounts = states.map(state => state.resolution.size)

      // Count constructor occurrences
      println("# Statistics")

      println("Generated terms = " + states.size)
      println("Average term size = " + termSizes.average)
      println("Average resolution count = " + resolutionCounts.average)
      */

      statistics
    })
  }
}

object RichGenerator {
  /**
    * Entry point when running as CLI. Takes a path to a Spoofax project as
    * argument and outputs running statistics.
    *
    * @param args
    */
  def main(args: Array[String]): Unit = {
    if (args.length == 0) {
      println("Usage: Generator <path> [limit]")
    } else {
      val path = args(0)

      val limit = args.lift(1).map(_.toInt).getOrElse(-1)

      new RichGenerator().generate(path, DefaultConfig, limit).subscribe(stats => {
        print("\033[2J")

        for ((k, v) <- stats) {
          println(s"$k = $v")
        }
      })
    }
  }
}
