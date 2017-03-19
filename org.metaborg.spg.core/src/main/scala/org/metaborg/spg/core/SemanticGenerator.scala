package org.metaborg.spg.core

import com.google.inject.Inject
import com.typesafe.scalalogging.LazyLogging
import org.metaborg.core.language.ILanguageService
import org.metaborg.spg.core.resolution.Label
import org.metaborg.spg.core.solver._
import org.metaborg.spg.core.spoofax.models.SortAppl
import org.metaborg.spg.core.spoofax.{Converter, Language, LanguageService}
import org.metaborg.spg.core.terms.{TermAppl, TermString, Var}

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
    val recurse = CGenRecurse(start.name, init.pattern, init.scopes, init.typ, start.sort, config.sizeLimit)
    val program = Program.fromRule(init) + recurse
    //val program = Program(TermAppl("Program", List(TermAppl("Fun", List(TermAppl("NameVar", List(TermString("x115"))), TermAppl("FunType", List(Var("x168"), Var("x169"))), TermAppl("Add", List(TermAppl("Var", List(TermAppl("NameVar", List(TermString("x149"))))), Var("x133"))))))),List(CGDecl(TermAppl("s114", List()),TermAppl("Occurrence", List(TermString("Var"), TermAppl("NameVar", List(TermString("x115"))), TermString("4")))), CGDirectEdge(TermAppl("s114", List()),Label('P'),TermAppl("s101", List())), CGenRecurse("Default", Var("x133"), List(TermAppl("s114", List())), Some(TermAppl("TInt", List())), SortAppl("Exp", List())), CGRef(TermAppl("Occurrence", List(TermString("Var"), TermAppl("NameVar", List(TermString("x149"))), TermString("2"))),TermAppl("s114", List())), CResolve(TermAppl("Occurrence", List(TermString("Var"), TermAppl("NameVar", List(TermString("x149"))), TermString("2"))),Var("x152")), CTypeOf(Var("x152"),TermAppl("TInt", List())), CGenRecurse("Default", Var("x169"), List(), Some(Var("x171")), SortAppl("Type", List())), CGenRecurse("Default", Var("x168"), List(), Some(Var("x170")), SortAppl("Type", List()))),TypeEnv(Map(Binding(TermAppl("Occurrence", List(TermString("Var"), TermAppl("NameVar", List(TermString("x115"))), TermString("4"))), TermAppl("TFun", List(Var("x170"), Var("x171")))))),Resolution(Map()),Subtypes(List()),List())

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
      return None
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
          val result = generate(Solver.solveFixpoint(program))

          if (result.isDefined) {
            return result
          }
        }
      }

      None
    }
  }
}
