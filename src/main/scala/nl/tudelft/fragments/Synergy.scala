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
  val interactive = false

  /**
    * Generate a single term.
    *
    * @param language
    * @return
    */
  def generate(language: Language, config: Config, verbose: Boolean): GenerationResult = {
    implicit val l = language

    def generatePrivate(): Option[GenerationResult] = {
      val startRule = language.startRules.random
      val init = language.specification.init.instantiate()
      val start = init.merge(CGenRecurse("Default", init.state.pattern, init.scopes, init.typ, startRule.sort), startRule, 0).get
      val result = synergize(language.specification.rules, config, verbose)(start)

      result match {
        case None =>
          None
        case Some(rule) =>
          val solvedStates = Solver.solve(rule.state)

          if (solvedStates.isEmpty) {
            None
          } else {
            val state = solvedStates.random
            val concrete = Concretor(language).concretize(state)
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
  def synergize(rules: List[Rule], config: Config, verbose: Boolean)(term: Rule)(implicit language: Language): Option[Rule] = {
    if (verbose) {
      println(term)
    }

    if (term.pattern.size > config.sizeLimit) {
      if (verbose) {
        println(term)
        println("Too large")
      }

      return None
    }

    if (term.recurse.isEmpty) {
      return Some(term)
    }

    // Pick a random recurse constraint (position to expand the current rule)
    val recurse = term.recurse.random

    // Merge with every other rule and filter out inconsistent merges and too big rules
    val options = step(term, recurse)

    // For each option, solve constraints that we can already solve
    val solvedOptions = options.flatMap(rule => Solver.solveAny(rule.state).map(rule.withState))

    // For each solved option, probabilistically solve a resolve constraint
    val resolvedOptions = solvedOptions.flatMap(rule => resolve(rule.state, config).map(rule.withState))

    // For each resolved option, solve constraints that we can already solve
    val solvedResolvedOptions = resolvedOptions.flatMap(rule => Solver.solveAny(rule.state).map(rule.withState)).filter(rule => Consistency.check(rule))

    // Filter the list of options by a language-dependent function
    val scoredSolvedResolvedOptions = config.choose(term, solvedResolvedOptions)

    if (scoredSolvedResolvedOptions.isEmpty) {
      if (verbose) {
        println(term)
        println("Inconsistency observed in " + recurse.pattern)
      }

      return None
    }

    // Continue with a random program among the best programs
    if (interactive) {
      println(s"Expand hole ${recurse.pattern}: $recurse")
      println("Which term would you like to continue with?")

      for ((rule, score) <- scoredSolvedResolvedOptions) {
        println(rule)
      }

      val choice = scala.io.StdIn.readInt()

      synergize(rules, config, verbose)(scoredSolvedResolvedOptions(choice)._1)
    } else {
      val distribution = Distribution(scoredSolvedResolvedOptions)

      synergize(rules, config, verbose)(distribution.sample)
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

  /**
    * For every resolve constraint, probabilistically resolve it to any possible declaration.
    *
    * @param state
    * @param config
    * @param language
    * @return
    */
  def resolve(state: State, config: Config)(implicit language: Language): List[State] = {
    state.resolve.foldLeftMap(state) {
      case (state, resolve) =>
        if (Random.nextInt(config.resolveProbability) == 0) {
          Solver.solveResolve(state, resolve)
        } else {
          List(state)
        }
    }
  }

  /**
    * Compute the solved rule and its score for each possible solution
    *
    * @deprecated
    */
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
