package org.metaborg.spg.core

import com.google.inject.Inject
import com.typesafe.scalalogging.LazyLogging
import org.metaborg.core.language.{ILanguageImpl, ILanguageService, LanguageIdentifier}
import org.metaborg.core.project.IProject
import org.metaborg.spg.core.solver._
import org.metaborg.spg.core.spoofax.{Converter, Language, LanguageService}
import rx.lang.scala.Observable

import scala.annotation.tailrec

class Generator @Inject() (val languageService: LanguageService, val baseLanguageService: ILanguageService) extends LazyLogging {
  /**
    * Create a cold Observable that emits programs for the given language
    * implementation and generation configuration.
    *
    * @param lut
    * @param project
    * @param config
    * @return
    */
  def generate(lut: ILanguageImpl, project: IProject, config: Config): Observable[String] = {
    val templateLang = getLanguage("org.metaborg:org.metaborg.meta.lang.template:2.1.0")
    val nablLang = getLanguage("org.metaborg:org.metaborg.meta.nabl2.lang:2.1.0")
    val language = languageService.load(templateLang, nablLang, lut, project, config.semanticsPath)

    generate(language, config)
  }

  /**
    * Create a cold Observable that emits programs for the given language
    * and generation configuration.
    *
    * @param lut
    * @param config
    * @return
    */
  private def generate(lut: Language, config: Config): Observable[String] = {
    Observable(subscriber => {
      repeat(config.limit) {
        if (!subscriber.isUnsubscribed) {
          subscriber.onNext(generateSingle(lut, config))
        }
      }

      if (!subscriber.isUnsubscribed) {
        subscriber.onCompleted()
      }
    })
  }

  /**
    * Generate a single term by repeatedly invoking generateTry until it
    * returns a well-formed term.
    *
    * @param language
    * @param config
    * @return
    */
  private def generateSingle(implicit language: Language, config: Config): String = {
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

  /**
    * Repeat the function `f` for `n` times. If `n` is negative, the function
    * is repeated ad infinitum.
    *
    * @param n
    * @param f
    * @tparam A
    */
  @tailrec final private def repeat[A](n: Int)(f: => A): Unit = n match {
    case 0 =>
      // Noop
    case _ =>
      f; repeat(n - 1)(f)
  }

  /**
    * Get a language implementation based on its identifier.
    *
    * @param identifier
    */
  private def getLanguage(identifier: String) = {
    val languageIdentifier = LanguageIdentifier.parse(identifier)

    baseLanguageService.getImpl(languageIdentifier)
  }
}
