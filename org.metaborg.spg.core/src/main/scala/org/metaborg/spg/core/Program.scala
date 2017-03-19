package org.metaborg.spg.core

import org.metaborg.spg.core.solver.{CGenRecurse, CResolve, Constraint, Resolution, Solver, Subtypes, TypeEnv}
import org.metaborg.spg.core.spoofax.Language
import org.metaborg.spg.core.spoofax.models.Strategy
import org.metaborg.spg.core.terms.Pattern

/**
  * Representation of a (partial) program.
  *
  * @param pattern
  */
case class Program(pattern: Pattern, constraints: List[Constraint], typeEnv: TypeEnv, resolution: Resolution, subtypes: Subtypes, inequalities: List[(Pattern, Pattern)]) {
  /**
    * Get all recurse constraints.
    */
  lazy val recurse = constraints.collect {
    case c: CGenRecurse =>
      c
  }

  /**
    * Get all resolve constraints.
    */
  lazy val resolve = constraints.collect {
    case c: CResolve =>
      c
  }

  /**
    * Get all proper constraints.
    */
  lazy val properConstraints = {
    constraints.filter(_.isProper)
  }

  /**
    * Apply a rule to a program to get a new program.
    *
    * @param rule
    */
  def apply(recurse: CGenRecurse, rule: Rule)(implicit language: Language): Option[Program] = {
    if (rule.name != recurse.name) {
      return None
    }

    val freshRule = rule.instantiate().freshen()

    // Compute the new balanced size
    val balancedSize = (recurse.size - rule.pattern.size)/(rule.recurses.length max 1)

    if (balancedSize <= 0) {
      return None
    }

    // Update the size in the recurse constraints
    val newConstraints = freshRule.constraints.map {
      case CGenRecurse(n, p, s, t, sort, _) =>
        CGenRecurse(n, p, s, t, sort, balancedSize)
      case x =>
        x
    }

    // TODO: Check that we stay below the balanced size...

    // Create a program that combines the constraints
    val program = copy(constraints = constraints ++ newConstraints)

    Solver.mergeSorts(program)(recurse.sort, freshRule.sort).flatMap(program =>
      Solver.mergePatterns(program)(recurse.pattern, freshRule.pattern).flatMap(program =>
        Solver.mergeTypes(program)(recurse.typ, freshRule.typ).flatMap(program =>
          Solver.mergeScopes(program)(recurse.scopes, freshRule.scopes)
        )
      )
    )
  }

  /**
    * Create a new program with the constraint removed.
    *
    * @param constraint
    * @return
    */
  def -(constraint: Constraint): Program = {
    copy(constraints = constraints - constraint)
  }

  /**
    * Create a new program with the constraint added.
    *
    * @param constraint
    * @return
    */
  def +(constraint: Constraint): Program = {
    copy(constraints = constraint :: constraints)
  }

  /**
    * Create a new program with the binding added.
    *
    * @param nameType
    * @return
    */
  def addBinding(nameType: (Pattern, Pattern)): Program = {
    copy(typeEnv = typeEnv + nameType)
  }

  /**
    * Create a new program with the resolution added.
    *
    * @param refDec
    * @return
    */
  def addResolution(refDec: (Pattern, Pattern)): Program = {
    copy(resolution = resolution + refDec)
  }

  /**
    * Create a new program with the inequality added.
    *
    * @param inequals
    * @return
    */
  def addInequalities(inequals: List[(Pattern, Pattern)]): Program = {
    copy(inequalities = inequals ++ inequalities)
  }

  /**
    * Apply given substitution to the program.
    *
    * @param substitution
    * @return
    */
  def substitute(substitution: TermBinding): Program = {
    Program(
      pattern =
        pattern.substitute(substitution),
      constraints =
        constraints.substitute(substitution),
      typeEnv =
        typeEnv.substitute(substitution),
      resolution =
        resolution.substitute(substitution),
      subtypes =
        subtypes.substitute(substitution),
      inequalities =
        inequalities.substitute(substitution)
    )
  }

  /**
    * Apply the given sort substitution to the program.
    *
    * @param binding
    * @return
    */
  def substituteSort(binding: SortBinding): Program = {
    copy(constraints = constraints.substituteSort(binding))
  }

  /**
    * Apply the given strategy to all components of the program.
    *
    * @param strategy
    * @return
    */
  def rewrite(strategy: Strategy) = {
    Program(
      pattern =
        pattern.rewrite(strategy),
      constraints =
        constraints.rewrite(strategy),
      typeEnv =
        typeEnv.rewrite(strategy),
      resolution =
        resolution.rewrite(strategy),
      subtypes =
        subtypes.rewrite(strategy),
      inequalities =
        inequalities.rewrite(strategy)
    )
  }
}

object Program {
  /**
    * Create an initial program from the given rule.
    *
    * @param rule
    */
  def fromRule(rule: Rule) = {
    val freshRule = rule.instantiate()

    Program(
      pattern =
        freshRule.pattern,
      constraints =
        freshRule.constraints,
      typeEnv =
        TypeEnv(),
      resolution =
        Resolution(),
      subtypes =
        Subtypes(),
      inequalities =
        Nil
    )
  }
}
