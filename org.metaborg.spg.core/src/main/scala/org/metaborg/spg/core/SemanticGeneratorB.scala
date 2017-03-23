package org.metaborg.spg.core

import com.google.inject.Inject
import com.typesafe.scalalogging.LazyLogging
import org.metaborg.core.language.ILanguageService
import org.metaborg.spg.core.solver._
import org.metaborg.spg.core.spoofax.{Converter, Language, LanguageService}

import scala.util.Random

/**
  * The semantics generator generates semantically valid terms.
  *
  * @param languageService
  * @param baseLanguageService
  * @param chooser
  * @param random
  */
class SemanticGeneratorB @Inject()(languageService: LanguageService, baseLanguageService: ILanguageService, chooser: AutomaticChooser)(implicit val random: Random) extends AbstractGenerator(languageService, baseLanguageService) with LazyLogging {
  /**
    * Generate a single term by repeatedly invoking generateTry until it
    * returns a semantically valid term.
    *
    * @param language
    * @param config
    * @return
    */
  override def generateSingle(language: Language, config: Config): String = {
    Iterator
      .continually(generateTry(language, config))
      .dropWhile(_.isEmpty)
      .next
      .get
  }

  /**
    * Try to generate a semantically valid term by invoking generateFueled with
    * a fuel parameter taken from the config object.
    *
    * If a term can be generated, returns Some with the term. Otherwise,
    * returns None.
    *
    * @param language
    * @param config
    * @return
    */
  private def generateTry(implicit language: Language, config: Config): Option[String] = {
    nameProvider.reset()

    val init = language.initRule.instantiate()
    val start = language.startRules.random
    val recurse = CGenRecurse(start.name, init.pattern, init.scopes, init.typ, start.sort, config.sizeLimit)
    val program = Program.fromRule(init) + recurse

    try {
      val termOpt = generateTop(program)(language, config)

      termOpt.map(term => {
        val concretePattern = Concretor(language).concretize(term)
        val strategoTerm = Converter.toTerm(concretePattern)

        language.printer(strategoTerm)
      })
    } catch {
      case OutOfFuelException(rule) =>
        logger.debug("Out of fuel: {}", rule)

        None
      case PatternSizeException(rule) =>
        logger.debug("Rule pattern too large: {}", rule)

        None
      case InconsistencyException(rule) =>
        logger.debug("Inconsistency observed in program: {}", rule)

        None
    }
  }

  def generateTop(program: Program)(implicit language: Language, config: Config): Option[Program] = {
    generateRecursive(program.recurse.head, config.sizeLimit).flatMap(program =>
      Solver.solveAll(program).randomOption
    )
  }

  /**
    * Given a recurse constraint, generate a program.
    *
    * For the given recurse: pick a random rule, generate a program for a
    * random child, merge it back into the rule, solve unsolved "trivial"
    * constraints, and continue with the next child.
    *
    * Afterwards, there may still be unsolved constraints (e.g. resolve
    * constraints). Let the caller solve remaining constraints (see
    * generateTop).
    *
    * @param recurse
    * @param size
    * @param language
    * @param config
    * @return
    */
  def generateRecursive(recurse: CGenRecurse, size: Int)(implicit language: Language, config: Config): Option[Program] = {
    if (size <= 0) {
      return None
    }

    val rules = language
      // Get rules for given recurse.name and recurse.sort
      .rulesMem(recurse.name, recurse.sort)
      // Also make sure the types will unify
      .flatMap(rule =>
        Merger.mergeTypes(rule)(recurse.typ, rule.typ).flatMap(rule =>
          Merger.mergeSorts(rule)(recurse.sort, rule.sort)
        )
      )

    for (rule <- rules.shuffle) {
      val program = Program.fromRule(rule.instantiate().freshen())

      val childRecurse = program.recurse
      val childSize = (size - 1) / (childRecurse.size max 1)

      // For every recurse, recursively generate a program and merge it into this program
      // After solving one recurse (e.g. the rhs of App(_, _) becomes Int), this changes the other recurse in the fold (e.g. the lhs of the application becomes Int -> ?)
      val mergedProgram = (1 to program.recurse.size).toList.foldLeftWhile(program) {
        case (program, _) =>
          val recurse = program.recurse.random

          generateRecursive(recurse, childSize).flatMap(subProgram =>
            program.merge(recurse, subProgram)
          )
      }

      // Solve any constraint that is solvable
      mergedProgram match {
        case Some(program) =>
          return Solver.solveFixpoint(program)
        case _ =>
          // Noop
      }
    }

    None
  }
}
