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
class SemanticGenerator @Inject()(languageService: LanguageService, baseLanguageService: ILanguageService, chooser: AutomaticChooser)(implicit val random: Random) extends AbstractGenerator(languageService, baseLanguageService) with LazyLogging {
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
        logger.trace("Out of fuel: {}", rule)

        None
      case PatternSizeException(rule) =>
        logger.trace("Rule pattern too large: {}", rule)

        None
      case InconsistencyException(rule) =>
        logger.trace("Inconsistency observed in program: {}", rule)

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
      return None
    }

    if (program.properConstraints.isEmpty) {
      logger.trace("All constraints solved: {}", program)

      Some(program)
    } else {
      for (constraint <- chooser.nextConstraints(program)) {
        val programs = Solver.rewrite(constraint, program - constraint)

        for (program <- chooser.nextProgram(programs)) {
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
