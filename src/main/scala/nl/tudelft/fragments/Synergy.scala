package nl.tudelft.fragments

import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

import com.typesafe.scalalogging.Logger
import nl.tudelft.fragments.spoofax.models.{SortAppl, SortVar}
import nl.tudelft.fragments.spoofax.{Converter, Language}
import org.metaborg.core.MetaborgException
import org.slf4j.LoggerFactory

import scala.annotation.tailrec
import scala.io.StdIn
import scala.util.Random

object Synergy {
  val logger = Logger(LoggerFactory.getLogger(this.getClass))

  /**
    * Generate a single term.
    *
    * @param language
    * @return
    */
  def generate(language: Language, config: Config, interactive: Boolean, verbosity: Int): GenerationResult = {
    implicit val l = language

    def generatePrivate(): Option[GenerationResult] = {
      val startRule = language.startRules.random
      val init = language.specification.init.instantiate()
      val start = init.merge(CGenRecurse("Default", init.state.pattern, init.scopes, init.typ, startRule.sort), startRule, 0).get
      val result = synergize(language.specification.rules, config, interactive, verbosity)(start)

      result match {
        case None =>
          None
        case Some(rule) =>
          val solvedStates = Solver.solve(rule.state)

          if (solvedStates.isEmpty) {
            if (verbosity >= 1) {
              println(rule)
              println("Still inconsistent")
            }

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
  def synergize(rules: List[Rule], config: Config, interactive: Boolean, verbosity: Int)(term: Rule)(implicit language: Language): Option[Rule] = {
    if (verbosity >= 2) {
      println(term)
    }

    if (term.pattern.size > config.sizeLimit) {
      if (verbosity >= 1) {
        println("Too large")
      }

      return None
    }

    if (term.recurse.isEmpty) {
      return Some(term)
    }

    // Pick a recurse constraint (position to expand the current rule)
    val recurse = if (interactive) {
      println("Which recurse constraint to expand?")

      for (hole <- term.recurse) {
        println(hole)
      }

      term.recurse(readChoice(term.recurse))
    } else {
      config.next(term, term.recurse)
    }

    // Merge with every other rule and filter out inconsistent merges and too big rules
    val options = step(term, recurse)

    // For each option, solve constraints that we can already solve
    val solvedOptions = options.flatMap(rule =>
      Solver.solveAny(rule.state).map(rule.withState)
    )

    // For each solved option, probabilistically solve a resolve constraint
    val resolvedOptions = solvedOptions.flatMap(rule =>
      resolve(rule.state, config).map(rule.withState)
    )

    // For each resolved option, solve constraints that we can already solve
    val solvedResolvedOptions = resolvedOptions
      .flatMap(rule => Solver.solveAny(rule.state).map(rule.withState))
      .filter(Consistency.check(_, 2))

    // Filter the list of options by a language-dependent function
    val scoredSolvedResolvedOptions = config.choose(term, solvedResolvedOptions)

    if (scoredSolvedResolvedOptions.isEmpty) {
      if (verbosity >= 1) {
        println(term)
        println("No expansion for " + recurse.pattern)
      }

      return None
    }

    // Continue with a random program among the best programs
    if (interactive) {
      println(s"Expand hole ${recurse.pattern}: $recurse")

      for ((rule, score) <- scoredSolvedResolvedOptions) {
        println(rule)
      }

      println("Which term would you like to continue with?")
      val choice = readChoice(scoredSolvedResolvedOptions)

      synergize(rules, config, interactive, verbosity)(scoredSolvedResolvedOptions(choice)._1)
    } else {
      val distribution = Distribution(scoredSolvedResolvedOptions)

      synergize(rules, config, interactive, verbosity)(distribution.sample)
    }
  }

  /**
    * Have the user choose one of the options. Repeat the question if the
    * input is invalid.
    *
    * @param options
    * @return
    */
  @tailrec def readChoice[T](options: List[T]): Int = {
    val choice = StdIn.readInt()

    if (choice >= 0 && choice <= options.length-1) {
      choice
    } else {
      readChoice(options)
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

  // Print the time so we can bencmark
  def bench() = {
    val now = DateTimeFormatter.ISO_DATE_TIME.format(ZonedDateTime.now())
    println("=== " + now)
  }
}

// For parsing a printed rule
class Binding[A, B](val a: A, val b: B)

object Binding {
  def apply[A, B](a: A, b: B): Binding[A, B] =
    new Binding(a, b)
}
