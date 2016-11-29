package nl.tudelft.fragments

import java.io.{File, FileOutputStream, PrintWriter}
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

import com.typesafe.scalalogging.Logger
import nl.tudelft.fragments.spoofax.{Converter, Language}
import org.slf4j.LoggerFactory

object Synergy {
  val logger = Logger(LoggerFactory.getLogger(this.getClass))

  // Make the language parts implicitly available
  //implicit val language = Language.load("/Users/martijn/Projects/scopes-frames/L3", "org.metaborg:L3:0.1.0-SNAPSHOT", "L3")
  //implicit val language = Language.load("/Users/martijn/Projects/metaborg-tiger/org.metaborg.lang.tiger", "org.metaborg:org.metaborg.lang.tiger:0.1.0-SNAPSHOT", "Tiger")
  implicit val language = Language.load("/Users/martijn/Projects/MiniJava", "org.metaborg:MiniJava:0.1.0-SNAPSHOT", "MiniJava")
  implicit val productions = language.productions
  implicit val signatures = language.signatures
  implicit val specification = language.specification
  implicit val printer = language.printer
  implicit val rules = specification.rules

  def statistics(states: List[State]): Unit = {
    // Count the number of occurrences of the given constructor in the states
    val consCount = (cons: String) => states.map(state => state.pattern.collect {
      case x@TermAppl(`cons`, _) =>
        List(x)
      case _ =>
        Nil
    }.size)

    // Term size
    val termSizes = states.map(state => state.pattern.size)

    // Number of name resolutions
    val resolutionCounts = states.map(state => state.resolution.size)

    // Count constructor occurrences
    println("Averages:")
    println("Generated terms = " + states.size)
    println("Term size = " + termSizes.average)
    println("Resolution count = " + resolutionCounts.average)
    println("'Class' constructors = " + consCount("Class").average)
    println("'Parent' constructors = " + consCount("Parent").average)
    println("'Assign' constructors = " + consCount("Assign").average)
    println("'Not' constructors = " + consCount("Not").average)
    println("'Add' constructors = " + consCount("Add").average)
    println("'Sub' constructors = " + consCount("Sub").average)
    println("'Mul' constructors = " + consCount("Mul").average)
    println("'Lt' constructors = " + consCount("Lt").average)
    println("'And' constructors = " + consCount("And").average)
    println("'Call' constructors = " + consCount("Call").average)
    println("'NewArray' constructors = " + consCount("NewArray").average)
    println("'Subscript' constructors = " + consCount("Subscript").average)
    println("'Length' constructors = " + consCount("Length").average)
    println("'NewObject' constructors = " + consCount("NewObject").average)
  }

  def main(args: Array[String]): Unit = {
    val rules = Synergy.rules
    val writer = new PrintWriter(new FileOutputStream(new File("results.log"), true))

    val states = (0 to 1000).toList.flatMap(_ => {
      bench()

      val startRule = language.startRules.random
      val result = synergize(rules)(startRule)

      result match {
        case None => {
          // Synergize returned None meaning there was an inconsistency or the term diverged
          println("Failed")

          None
        }
        case Some(rule) => {
          // Print rule
          println(result)

          // Solve remaining constraints
          val solvedStates = Solver.solve(rule.state)

          if (solvedStates.isEmpty) {
            println("Unsolvable")

            None
          } else {
            // State
            val state = solvedStates.random

            // Concretize the tree
            val concrete = Concretor(language).concretize(rule, state)

            // Convert back to IStrategoTerm
            val term = Converter.toTerm(concrete)

            // Pretty-print IStrategoTerm to String
            val source = printer(term)

            // Print to stdout
            println(source)

            // Append to results.log
            writer.println("===============")
            writer.println(DateTimeFormatter.ISO_DATE_TIME.format(ZonedDateTime.now()))
            writer.println("---------------")
            writer.println(source)
            writer.flush()

            bench()

            Some(state)
          }
        }
      }
    })

    statistics(states)
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

    // All scored terms should still be consistent!
    assert(scored.forall { case (rule, score) => Consistency.check(rule) })

    if (scored.isEmpty) {
      return None
    }

    // Take the best n programs. We shuffle before sorting to randomize ruels with same score
    val bests = scored.shuffle.sortBy(_._2).take(5)

    // If there is a complete fragment, return it!
    for ((rule, score) <- bests) if (score == 0) {
      return Some(rule)
    }

    // Approach A: Backtrack among the best programs
    /*
    for (best <- bests) {
      synergize(rules)(bests.random._1) match {
        case r@Some(_) => return r
        case _ =>
      }
    }

    None
    */

    // Approach B: Continue with a random program among the best programs
    synergize(rules)(bests.random._1)
  }

  // Compute the solved rule and its score for each possible solution
  def score(rule: Rule): List[(Rule, Int)] = {
    // Compute the score for a single constraint
    def constraintScore(constraint: Constraint) = constraint match {
      case _: CResolve => 3
      case _: CGenRecurse => 3
      case _ => 1
    }

    // Compute the score after eliminating constraints
    Eliminate
      .solve(rule.state)
      .map(state => (rule.withState(state), state.constraints.map(constraintScore).sum))
      .filter { case (rule, _) => Consistency.check(rule) }
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
