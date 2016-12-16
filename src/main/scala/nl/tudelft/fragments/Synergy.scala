package nl.tudelft.fragments

import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

import com.typesafe.scalalogging.Logger
import nl.tudelft.fragments.spoofax.models.{SortAppl, SortVar}
import nl.tudelft.fragments.spoofax.{Converter, Language}
import org.metaborg.core.MetaborgException
import org.slf4j.LoggerFactory

import scala.util.Random

object Synergy {
  val logger = Logger(LoggerFactory.getLogger(this.getClass))
  val verbose = false
  val interactive = false

  /**
    * Generate a single term.
    *
    * @param language
    * @return
    */
  def generate(language: Language, config: Config): GenerationResult = {
    implicit val l = language

    def generatePrivate(): Option[GenerationResult] = {
      val startRule = language.startRules.random
      val init = language.specification.init.instantiate()
      val start = init.merge(CGenRecurse("Default", init.state.pattern, init.scopes, init.typ, startRule.sort), startRule, 0).get
      val result = synergize(language.specification.rules, config)(start)

      result match {
        case None =>
          None
        case Some(rule) =>
          val solvedStates = Solver.solve(rule.state)

          if (solvedStates.isEmpty) {
            None
          } else {
            val state = solvedStates.random
            val concrete = Concretor(language).concretize(rule, state)
            val term = Converter.toTerm(concrete)

            try {
              val source = language.printer(term)

              Some(GenerationResult(rule, source))
            } catch {
              case e: MetaborgException =>
                throw GenerateException(s"Unable to print $term for concrete $concrete in state $state", e)
            }
          }
      }
    }

    // Repeat generatePrivate until it returns a Some
    Iterator.continually(generatePrivate).dropWhile(_.isEmpty).next.get
  }

  // Expand rule with the best alternative from rules
  def synergize(rules: List[Rule], config: Config)(term: Rule)(implicit language: Language): Option[Rule] = {
    if (verbose) {
      //println(term)
    }

    // Prevent diverging
    if (term.pattern.size > config.sizeLimit) {
      if (verbose) {
        println(term)
        println("Too large")
      }

      return None
    }
    
    // If the term is complete, return
    if (term.recurse.isEmpty) {
      return Some(term)
    }

    // Pick a random recurse constraint (position to expand the current rule)
    val recurse = term.recurse.random

    // Merge with every other rule and filter out inconsistent merges and too big rules
    val options = step(term, recurse)

    // Eliminate some constraints (and probabilistically solve CResolve constraints)
    val options2 = options.flatMap(rule => Eliminate
      .solve(rule.state)
      .map(state => rule.withState(state))
    )

    // Compute a score for each rule
    val scored = options2.map(rule => (rule, config.scoreFn(rule)))

    // For every option, solve constraints and compute its score
    //val scored = options.flatMap(score)

    if (scored.isEmpty) {
      if (verbose) {
        println(term)
        println("Inconsistency observed in " + recurse.pattern)
      }

      return None
    }

    // Take the best n programs. We shuffle before sorting to randomize ruels with same score
    val bests = scored.shuffle.sortBy(_._2).take(10)

    // If there is a complete fragment, return it with p = 0.2
    for ((rule, score) <- bests) if (score == 0 && Random.nextInt(5) == 0) {
      return Some(rule)
    }

    // Continue with a random program among the best programs
    if (interactive) {
      println(s"Expand hole ${recurse.pattern}: $recurse")
      println("Which term would you like to continue with?")
      for (((rule, score), index) <- scored.zipWithIndex) {
        println(s"Score($index, $score, $rule)")
      }

      val choice = scala.io.StdIn.readInt()

      synergize(rules, config)(scored(choice)._1)
    } else {
      synergize(rules, config)(bests.random._1)
    }
  }

  /**
    * Compute all possible expansions of recurse.
    *
    * @param term
    * @param recurse
    * @return
    */
  def step(term: Rule, recurse: CGenRecurse)(implicit language: Language): List[Rule] =
    language.rules(recurse.name, recurse.sort).flatMap(rule =>
      term.merge(recurse, rule, 2)
    )

  // Compute the solved rule and its score for each possible solution
  def score(rule: Rule)(implicit language: Language): List[(Rule, Int)] = {
    // Compute the score for a single constraint
    def constraintScore(constraint: Constraint) = constraint match {
      case _: CResolve => 3
      case _: CGenRecurse =>
        if (rule.pattern.size > 10) {
          6
        } else {
          -6
        }
      case _: CTrue => 0
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
