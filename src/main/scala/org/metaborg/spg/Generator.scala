package org.metaborg.spg

import com.typesafe.scalalogging.Logger
import org.metaborg.spg.solver.{CGenRecurse, Solver}
import org.metaborg.spg.spoofax.{Converter, Language}
import org.metaborg.spg.spoofax.Language
import org.slf4j.LoggerFactory

object Generator {
  val logger = Logger(LoggerFactory.getLogger(this.getClass))

  /**
    * Generate a single term by repeatedly invoking generateTry until it
    * returns a well-formed term.
    *
    * @param language
    * @param config
    * @return
    */
  def generate(implicit language: Language, config: Config): String = {
    Iterator.continually(generateTry).dropWhile(_.isEmpty).next.get
  }

  /**
    * Try to generate a well-formed term by invoking generateFueled with a fuel
    * parameter taken from the config object.
    *
    * @param language
    * @param config
    * @return
    */
  private def generateTry(implicit language: Language, config: Config): Option[String] = {
    val startRule = language.startRules.random
    val init = language.specification.init.instantiate()
    val start = init.merge(CGenRecurse("Default", init.state.pattern, init.scopes, init.typ, startRule.sort), startRule, 0).get

    try {
      val termOpt = generateFueled(language, config)(start)

      termOpt.map(term => {
        val concretePattern = Concretor(language).concretize(term.state)
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
        logger.debug("Inconsistency observed in rule: {}", rule)

        None
    }
  }

  /**
    * Returns a generation function that closes over a mutable fuel variable
    * that is subtracted with each invocation. Throws an OutOfFuelException
    * when there is no more fuel.
    *
    * @param language
    * @param config
    * @return
    */
  private def generateFueled(language: Language, config: Config): Rule => Option[Rule] = {
    var mutableFuel = config.fuel

    lazy val self: Rule => Option[Rule] = (rule: Rule) => mutableFuel match {
      case 0 =>
        throw OutOfFuelException(rule)
      case _ =>
        mutableFuel = mutableFuel - 1; generate(self)(rule)(language, config)
    }

    self
  }


  /**
    * Generate a term.
    *
    * @param generate
    * @param rule
    * @param language
    * @param config
    * @return
    */
  private def generate(generate: Rule => Option[Rule])(rule: Rule)(implicit language: Language, config: Config): Option[Rule] = {
    logger.trace("Generate with rule: {}", rule)

    if (rule.pattern.size > config.sizeLimit) {
      throw PatternSizeException(rule)
    }

    if (!Consistency.check(rule)) {
      // Returning None causes backtracking
      return None

      // Throwing InconsistencyException causes abandoning the term
      //throw InconsistencyException(rule)
    }

    if (rule.properConstraints.isEmpty) {
      logger.debug("All constraints solved: {}", rule)

      Some(rule)
    } else {
      for (constraint <- rule.properConstraints.shuffle.sortBy(_.priority)) {
        val states = Solver.rewrite(constraint, rule.state - constraint)

        for (state <- states.shuffle) {
          val result = generate(rule.withState(state))

          if (result.isDefined) {
            return result
          }
        }
      }

      None
    }
  }
}
