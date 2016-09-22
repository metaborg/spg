package nl.tudelft.fragments

import com.typesafe.scalalogging.Logger
import nl.tudelft.fragments.spoofax.models.SortAppl
import nl.tudelft.fragments.spoofax.{Converter, Language}
import org.slf4j.LoggerFactory

import scala.annotation.tailrec

// Synergy is v2 where we combine fragments if it solves constraints.
object Synergy {
  val logger = Logger(LoggerFactory.getLogger(this.getClass))

  // Make the language parts implicitly available
  implicit val language = Language.load("/Users/martijn/Projects/MiniJava", "org.metaborg:MiniJava:0.1.0-SNAPSHOT", "MiniJava")
  implicit val productions = language.productions
  implicit val signatures = language.signatures
  implicit val specification = language.specification
  implicit val printer = language.printer
  implicit val rules = specification.rules

  // Parameters
  val steps = 2000

  def main(args: Array[String]): Unit = {
    // Grow some programs
    logger.info("Start growing")

    val rules = repeat(grow, steps)(Synergy.rules)

    // Synergize from a random start rule
    logger.info("Start synergizing with " + rules.length + " rules")

    for (i <- 0 to 100) {
      // TODO: 100 steps won't complete the program... as long as there are recurse constraints, we have work to do!
      val result = repeatUntilNoneOrDone(synergize(rules), 100)(language.startRules.random)

      if (result.isEmpty) {
        // Synergize returned None meaning there was an inconsistency..
        println("Failed")
      } else {
        // Print rule
        println(result)

        // Try to solve
        val solvedStates = Solver.solve(result.get.state)

        if (solvedStates.isEmpty) {
          println("Unsolvable")
        } else {
          // Print pretty-printed concrete solved rule
          println(printer(
            Converter.toTerm(
              Concretor.concretize(result.get, solvedStates.random)
            )
          ).stringValue())

          println("===")
        }
      }
    }
  }

  // Expand rule with the best alternative from rules
  def synergize(rules: List[Rule])(current: Rule): Option[Rule] = {
    // Debug
    //println(current)

    // Pick a random recurse constraint (position to expand the current rule)
    val recurseOpt = current.recurse.randomOption

    // If there is still a recurse constraint
    if (recurseOpt.nonEmpty) {
      // Merge with every other rule and filter out inconsistent merges and too big rules
      val options = rules
        .flatMap(current.merge(recurseOpt.get, _, 2))
        .filter(_.pattern.size < 50)

      // For every option, solve constraints and compute its score
      val scored = options.flatMap(score)

      if (scored.isEmpty) {
        None
      } else {
        /*
        // Get the option with the best (lowest) score
        val best = scored.minBy(_._2)

        // Return the best
        best._1
        */

        // Take the top. We do not want to always take the best, as this is pretty deterministic...
        val bests = scored.sortBy(_._2).take((scored.length * 0.2).ceil.toInt)

        // Return a random from this top
        Some(bests.random._1)
      }
    } else {
      Some(current)
    }
  }

  // Compute the solved rule and its score for the given rule
  def score(rule: Rule): Option[(Rule, Int)] =
    Consistency
      .solve(rule.state)
      .map(state => (rule.copy(state = state), state.constraints.length))

  // Randomly merge rules in a rules into larger consistent rules
  def grow(rules: List[Rule]): List[Rule] = {
    val ruleA = rules.random
    val ruleB = rules.random

    if (ruleA.recurse.nonEmpty) {
      val recurse = ruleA.recurse.random

      ruleA
        .merge(recurse, ruleB, 1)
        .filter(_.pattern.size < 50)
        .map(_ :: rules)
        .getOrElse(rules)
    } else {
      rules
    }
  }

  // Repeat until f returns None or there are no more recurse constraints
  def repeatUntilNoneOrDone(f: Rule => Option[Rule], n: Int): Rule => Option[Rule] = {
    @tailrec def repeatAcc(acc: Rule, n: Int): Option[Rule] = n match {
      case 0 => Some(acc)
      case _ => f(acc) match {
        case Some(x) =>
          if (x.recurse.isEmpty) {
            Some(x)
          } else {
            repeatAcc(x, n - 1)
          }
        case None => None
      }
    }

    (t: Rule) => repeatAcc(t, n)
  }
}
