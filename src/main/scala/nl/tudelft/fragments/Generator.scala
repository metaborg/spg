package nl.tudelft.fragments

import com.typesafe.scalalogging.Logger
import nl.tudelft.fragments.spoofax.{Converter, Language}
import org.metaborg.core.MetaborgException
import org.slf4j.LoggerFactory

object Generator {
  val logger = Logger(LoggerFactory.getLogger(this.getClass))
  var fuel = 200

  /**
    * Generate a single term.
    *
    * @param language
    * @return
    */
  def generate(language: Language, config: Config): String = {
    implicit val l = language

    def generateTry: Option[String] = {
      val startRule = language.startRules.random
      val init = language.specification.init.instantiate()
      val start = init.merge(CGenRecurse("Default", init.state.pattern, init.scopes, init.typ, startRule.sort), startRule, 0).get

      fuel = 200

      try {
        val termOpt = generate(config)(start)

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
        case e: MetaborgException =>
          None
      }
    }

    Iterator.continually(generateTry).dropWhile(_.isEmpty).next.get
  }

  def generate(config: Config)(rule: Rule)(implicit language: Language): Option[Rule] = {
    logger.trace("Generate with rule: {}", rule)

    if (fuel <= 0) {
      throw OutOfFuelException(rule)
    }

    if (rule.pattern.size > config.sizeLimit) {
      throw PatternSizeException(rule)
    }

    if (!Consistency.check(rule)) {
      throw InconsistencyException(rule)
    }

    if (rule.properConstraints.isEmpty) {
      logger.debug("All constraints solved: {}", rule)

      Some(rule)
    } else {
      for (constraint <- rule.properConstraints.shuffle.sortBy(_.priority)) {
        val states = Solver.rewrite(constraint, rule.state - constraint)

        for (state <- states.shuffle) {
          fuel = fuel - 1
          val result = generate(config)(rule.withState(state))

          if (result.isDefined) {
            return result
          }
        }
      }

      None
    }
  }
}
