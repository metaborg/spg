package org.metaborg.spg

import com.typesafe.scalalogging.Logger
import org.metaborg.spg.solver.{CGenRecurse, Solver}
import org.metaborg.spg.spoofax.Converter
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
    val init = language.initRule.instantiate()
    val start = language.startRules.random
    val recurse = CGenRecurse(start.name, init.pattern, init.scopes, init.typ, start.sort)
    val program = Program.fromRule(init) + recurse

    try {
      val termOpt = generateFueled(language, config)(program)

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

  /**
    * Returns a generation function that closes over a mutable fuel variable
    * that is subtracted with each invocation. Throws an OutOfFuelException
    * when there is no more fuel.
    *
    * @param language
    * @param config
    * @return
    */
  private def generateFueled(language: Language, config: Config): Program => Option[Program] = {
    var mutableFuel = config.fuel

    lazy val self: Program => Option[Program] = (rule: Program) => mutableFuel match {
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
    * @param program
    * @param language
    * @param config
    * @return
    */
  private def generate(generate: Program => Option[Program])(program: Program)(implicit language: Language, config: Config): Option[Program] = {
    logger.trace("Generate with program: {}", program)

    if (program.pattern.size > config.sizeLimit) {
      throw PatternSizeException(program)
    }

    if (!Consistency.check(program)) {
      // Returning None causes backtracking
      return None

      // Throwing InconsistencyException causes abandoning the term
      //throw InconsistencyException(program)
    }

    if (program.properConstraints.isEmpty) {
      logger.debug("All constraints solved: {}", program)

      Some(program)
    } else {
      for (constraint <- program.properConstraints.shuffle.sortBy(_.priority)) {
        val programs = Solver.rewrite(constraint, program - constraint)

        for (program <- programs.shuffle) {
          val result = generate(program)

          if (result.isDefined) {
            return result
          }
        }
      }

      None
    }
  }
}
