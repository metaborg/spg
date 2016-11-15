package nl.tudelft.fragments

import java.io.{File, FileOutputStream, PrintWriter}
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

import com.typesafe.scalalogging.Logger
import nl.tudelft.fragments.spoofax.{Converter, Language}
import org.slf4j.LoggerFactory

import scala.annotation.tailrec

// Synergy is v2 where we combine fragments if it solves constraints.
object Synergy {
  val logger = Logger(LoggerFactory.getLogger(this.getClass))

  // Make the language parts implicitly available
  //implicit val language = Language.load("/Users/martijn/Projects/scopes-frames/L3", "org.metaborg:L3:0.1.0-SNAPSHOT", "L3")
  implicit val language = Language.load("/Users/martijn/Projects/MiniJava", "org.metaborg:MiniJava:0.1.0-SNAPSHOT", "MiniJava")
  implicit val productions = language.productions
  implicit val signatures = language.signatures
  implicit val specification = language.specification
  implicit val printer = language.printer
  implicit val rules = specification.rules

  def main(args: Array[String]): Unit = {
    val rules = Synergy.rules
    val writer = new PrintWriter(new FileOutputStream(new File("results.log"), true))

    for (i <- 0 to 10000) {
      bench()

      val result = synergize(rules)(language.startRules.random)

      if (result.isEmpty) {
        // Synergize returned None meaning there was an inconsistency or the term diverged
        //println("Failed")
      } else {
        // Print rule
        println(result)

        // Solve remaining constraints
        val solvedStates = Solver.solve(result.get.state)

        if (solvedStates.isEmpty) {
          //println("Unsolvable")
        } else {
          // State
          val state = solvedStates.random

          // Print pretty-printed concrete solved rule
          val source = printer(
            Converter.toTerm(
              Concretor(language).concretize(result.get, state)
            )
          )

          // Append to results.log
          writer.println("===")
          writer.println(result)
          writer.println("---")
          writer.println(source.stringValue())
          writer.println("===")
          writer.flush()

          // Print to stdout
          println(source.stringValue())
          println("===")

          bench()
        }
      }
    }
  }

  // Expand rule with the best alternative from rules
  def synergize(rules: List[Rule])(term: Rule): Option[Rule] = {
    // Prevent diverging
    if (term.pattern.size > 140) {
      return None
    }

    // If the term is complete, return
    if (term.recurse.isEmpty) {
      return Some(term)
    }

    // Pick a random recurse constraint (position to expand the current rule)
    val recurse = term.recurse.random

    // Merge with every other rule and filter out inconsistent merges and too big rules
    val options = rules.flatMap(term.merge(recurse, _, 2))

    // For every option, solve constraints and compute its score
    val scored = options.flatMap(score)

    if (scored.isEmpty) {
      return None
    }

    // Take the best 5 programs
    val bests = scored.sortBy(_._2).take(3)

    // If there is a complete fragment, return it!
    for ((rule, score) <- bests) if (score == 0) {
      return Some(rule)
    }

    // Continue with a random program among the best programs
    synergize(rules)(bests.random._1)
  }

  // Compute the solved rule and its score for each possible solution
  def score(rule: Rule): List[(Rule, Int)] = {
    // Compute the score for a single constraint
    def constraintScore(constraint: Constraint) = constraint match {
      case _: CResolve => 3
      case _: CGenRecurse => 6
      case _ => 1
    }

    // Compute the score after crossing out constraints
    Eliminate
      .solve(rule.state)
      .map(state => (rule.withState(state), state.constraints.map(constraintScore).sum))
  }

  // Print the time so we can bencmark
  def bench() = {
    val now = DateTimeFormatter.ISO_DATE_TIME.format(ZonedDateTime.now())
    println("=== " + now)
  }
}

// For parsing a printed rule
class Binding[A, B](a: A, b: B) extends Tuple2[A, B](a, b)

object Binding {
  def apply[A, B](a: A, b: B): Binding[A, B] =
    new Binding(a, b)
}
