package org.metaborg.spg.core

import com.typesafe.scalalogging.LazyLogging
import org.metaborg.spg.core.resolution.{Graph, Occurrence}
import org.metaborg.spg.core.resolution.OccurrenceImplicits._
import org.metaborg.spg.core.solver._
import org.metaborg.spg.core.stratego.Strategy.{attempt, topdown}
import org.metaborg.spg.core.spoofax.{Converter, Language}
import org.metaborg.spg.core.stratego.Strategy
import org.metaborg.spg.core.terms.{Pattern, Var}

import scala.util.Random

/**
  * The semantics generator generates semantically valid terms.
  *
  * @param language
  * @param random
  */
class SemanticGenerator(language: Language, config: Config)(implicit val random: Random) extends LazyLogging {
  /**
    * Generate a single term by repeatedly invoking generateTry until it
    * returns a semantically valid term.
    *
    * @return
    */
  def generateOne(): String = {
    Iterator
      .continually(generateTry())
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
    * @return
    */
  private def generateTry(): Option[String] = {
    nameProvider.reset()

    val init = language.initRule.instantiate()
    val start = language.startRules.random
    val recurse = CGenRecurse(start.name, init.pattern, init.scopes, init.typ, start.sort, config.sizeLimit)
    val program = Program.fromRule(init) + recurse

    try {
      val termOpt = generateTop(program)

      termOpt.map(term => {
        val concretePattern = Concretor(language).concretize(term)(language)
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

  def generateTop(program: Program): Option[Program] = {
    generateFueled()(program.recurse.head, config.sizeLimit, program.constraints).flatMap {
      case (program, _) =>
        Solver.solveAll(program)(language).randomOption
    }.randomOption
  }

  /**
    * Wraps generateRecursive in a function that limits backtracking by the
    * given fuel parameter.
    *
    * @return
    */
  def generateFueled(): (CGenRecurse, Int, List[Constraint]) => List[(Program, Unifier)] = {
    var mutableFuel = config.fuel

    lazy val self: (CGenRecurse, Int, List[Constraint]) => List[(Program, Unifier)] = (r: CGenRecurse, s: Int, c: List[Constraint]) => mutableFuel match {
      case 0 =>
        Nil
      case _ =>
        mutableFuel = mutableFuel - 1; generateRecursive(self)(r, s, c)
    }

    self
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
    * @param generateRecursive Continuation
    * @param recurse
    * @param size
    * @return
    */
  def generateRecursive(generateRecursive: (CGenRecurse, Int, List[Constraint]) => List[(Program, Unifier)])(recurse: CGenRecurse, size: Int, context: List[Constraint]): List[(Program, Unifier)] = {
    if (size > 0) {
      for (rule <- language.rules(recurse)(language).shuffle) {
        val program = Program.fromRule(rule.instantiate().freshen())
        val mergedPrograms = generateProgram(generateRecursive)(program, size, context)

        for (mergedProgram <- mergedPrograms) {
          val cleaned = clean(mergedProgram, context)

          if (cleaned.nonEmpty) {
            return cleaned
          }
        }
      }
    }

    Nil
  }

  /**
    * Given a program, complete it by filling in all holes.
    *
    * @param generateRecursive
    * @param program
    * @param size
    * @param context
    * @return
    */
  def generateProgram(generateRecursive: (CGenRecurse, Int, List[Constraint]) => List[(Program, Unifier)])(program: Program, size: Int, context: List[Constraint]): List[(Program, Unifier)] = {
    val childSize = (size - 1) / (program.recurse.size max 1)

    program.recurse.shuffle.foldLeftMap(program, Unifier.empty) {
      case ((program, unifier), recurse) =>
        val updatedRecurse = recurse.substitute(unifier)
        val updatedContext = context.substitute(unifier) ++ program.constraints

        generateRecursive(updatedRecurse, childSize, updatedContext).flatMap {
          case (childProgram, childUnifier) =>
            program.merge(updatedRecurse, childProgram)(language).flatMap {
              case (mergedProgram, mergeUnifier) =>
                Solver.solveFixpointE(mergedProgram)(language).map {
                  case (solvedProgram, solveUnifier) =>
                    (solvedProgram, unifier ++ childUnifier ++ mergeUnifier ++ solveUnifier)
                }
            }
        }
    }
  }

  /**
    * Cleanup a program by solving all its CResolve constraints.
    *
    * TODO: We should also require all CSubtype constraints to be satisfied, as these may also lead to excessive backtracking.
    *
    * @param pu
    * @param context
    * @return
    */
  def clean(pu: (Program, Unifier), context: List[Constraint]): List[(Program, Unifier)] = {
    pu match {
      case (program, substitution) =>
        // First, solveFixpoint to cleanup any remaining constraints
        val a = Solver.solveFixpoint(program)(language)

        // Second, resolve every reference. We don't want unresolved references, because this kills performance
        val b = solveResolves(a, context)

        // Third, solveFixpoint on each option to cleanup any remaining constraints (again)
        val c = b.map {
          case (program, newSubstitution) =>
            (Solver.solveFixpoint(program)(language), newSubstitution)
        }

        // Forth, remove any programs that are now inconsistent. We don't want inconsistencies.
        val d = c.filter {
          case (program, _) =>
            Consistency.constraintsCheck(program)
        }

        // If there is at least one valid program
        if (d.nonEmpty) {
          // Fifth, for all remaining programs, combine the substitution
          return d.map {
            case (program, newSubstitution) =>
              (program, substitution ++ Unifier(newSubstitution))
          }
        }
    }

    Nil
  }

  /**
    * Solve all CResolve constraints in the given program.
    *
    * If a reference cannot be resolved, return None. If a reference can be
    * resolved to multiple declarations, fork on each choice.
    *
    * TODO: The order of resolving references may be important (when references depend upon each other)
    */
  def solveResolves(program: Program, context: List[Constraint]): List[(Program, Map[Var, Pattern])] = {
    program.resolve.foldLeftMap((program, Map.empty[Var, Pattern])) {
      case ((program, substitution), resolve) =>
        solveResolve(program, resolve, context).map {
          case (program, newSubstitution) =>
            (program, substitution ++ newSubstitution)
        }
    }
  }

  /**
    * Solve the given CResolve constraint in the given program.
    *
    * If the reference cannot be resolved, return None. Otherwise, fork on each
    * declaration that the reference may resolve to and return the new program.
    *
    * @return
    */
  def solveResolve(program: Program, resolve: CResolve, context: List[Constraint]): List[(Program, TermBinding)] = {
    val declarations = Graph(program.constraints ++ context)(language).res(Resolution())(resolve.n1)

    declarations.toList.flatMap(declaration => {
      applyResolution(program, resolve, declaration)
    })
  }

  /**
    * Solve the given CResolve constraint by resolving to the given declaration.
    *
    * TODO: We use a rewrite rule to replace the reference (occurrence) by the declaration (occurrence) to ensure they get the same name. Hacky..
    * TODO: Can't we move this method to the Program class? I.e. program.resolve(reference, declaration)?
    *
    * @param program
    * @param resolve
    * @param declaration
    * @return
    */
  def applyResolution(program: Program, resolve: CResolve, declaration: Occurrence): Option[(Program, TermBinding)] = {
    val substitutionOpt = resolve.n2.unify(declaration)

    substitutionOpt.map(substitution => {
      val newProgram = (program - resolve)
        .substitute(substitution)
        .addResolution(resolve.n1 -> declaration)
        .rewrite(topdown(attempt(new Strategy {
          override def apply(p: Pattern): Option[Pattern] = {
            if (p == resolve.n1.occurrence.name) {
              Some(declaration.name)
            } else {
              None
            }
          }
        })))

      (newProgram, substitution)
    })
  }
}
