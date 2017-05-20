package org.metaborg.spg.core

import org.metaborg.spg.core.sdf.Sort
import org.metaborg.spg.core.solver.TypeEnv
import org.metaborg.spg.core.spoofax.Language
import org.metaborg.spg.core.sdf.{SortAppl, SortVar}
import org.metaborg.spg.core.terms.Pattern

object Merger {
  /**
    * Merge two sorts.
    *
    * If there is an injection from sort A to sort B, i.e. sort B is-an sort A,
    * merging A with B succeeds, but merging B with A does not succeed. As
    * such, this operation is not commutative!
    *
    * @param rule
    * @param s1
    * @param s2
    * @param language
    * @return
    */
  def mergeSorts(rule: Rule)(s1: Sort, s2: Sort)(implicit language: Language): Option[Rule] = s1 match {
    case SortVar(_) =>
      s1.unify(s2).map(rule.substituteSort)
    case SortAppl(_, _) =>
      language.signature
        .injectionsClosure(s1).view
        .flatMap(_.unify(s2))
        .headOption
        .map(rule.substituteSort)
  }

  /**
    * Merge two patterns.
    *
    * @param program
    * @param p1
    * @param p2
    * @return
    */
  def mergePatterns(program: Rule)(p1: Pattern, p2: Pattern): Option[Rule] = {
    p1.unify(p2).map(program.substitute)
  }

  /**
    * Merge two types.
    *
    * @param program
    * @param t1
    * @param t2
    * @return
    */
  def mergeTypes(program: Rule)(t1: Option[Pattern], t2: Option[Pattern]): Option[Rule] = (t1, t2) match {
    case (None, None) =>
      Some(program)
    case (Some(t1), Some(t2)) =>
      t1.unify(t2).map(program.substitute)
    case _ =>
      None
  }

  /**
    * Merge two lists of scopes.
    *
    * @param program
    * @param ss1
    * @param ss2
    * @return
    */
  def mergeScopes(program: Rule)(ss1: List[Pattern], ss2: List[Pattern]): Option[Rule] = {
    Unifier.unify(ss1, ss2).map(substitution =>
      program.substitute(substitution)
    )
  }
}

object ProgramMerger {
  /**
    * Merge two sorts.
    *
    * TODO: Unifier.empty is wrong, but the type system clashes with Sorts and Patterns
    *
    * @param rule
    * @param s1
    * @param s2
    * @param language
    * @return
    */
  def mergeSorts(rule: Program)(s1: Sort, s2: Sort)(implicit language: Language): Option[(Program, Substitution)] = s1 match {
    case SortVar(_) =>
      s1.unify(s2).map(unifier =>
        (rule.substituteSort(unifier), Substitution.empty)
      )
    case SortAppl(_, _) =>
      language.signature
        .injectionsClosure(s1).view
        .flatMap(_.unify(s2))
        .headOption
        .map(unifier =>
          (rule.substituteSort(unifier), Substitution.empty)
        )
  }

  /**
    * Merge two patterns.
    *
    * @param program
    * @param p1
    * @param p2
    * @return
    */
  def mergePatterns(program: Program)(p1: Pattern, p2: Pattern): Option[(Program, Substitution)] = {
    Unifier.unify(p1, p2).map(substitution =>
      (program.substitute(substitution), substitution)
    )
  }

  /**
    * Merge two types.
    *
    * @param program
    * @param t1
    * @param t2
    * @return
    */
  def mergeTypes(program: Program)(t1: Option[Pattern], t2: Option[Pattern]): Option[(Program, Substitution)] = (t1, t2) match {
    case (None, None) =>
      Some(program, Substitution.empty)
    case (Some(t1), Some(t2)) =>
      Unifier.unify(t1, t2).map(unifier =>
        (program.substitute(unifier), Substitution(unifier))
      )
    case _ =>
      None
  }

  /**
    * Merge two lists of scopes.
    *
    * @param program
    * @param ss1
    * @param ss2
    * @return
    */
  def mergeScopes(program: Program)(ss1: List[Pattern], ss2: List[Pattern]): Option[(Program, Substitution)] = {
    Unifier.unify(ss1, ss2).map(substitution =>
      (program.substitute(substitution), substitution)
    )
  }

  /**
    * Merge two type environments.
    *
    * @param program
    * @param t1
    * @param t2
    * @param language
    * @return
    */
  def mergeTypeEnv(program: Program)(t1: TypeEnv, t2: TypeEnv)(implicit language: Language): Option[(Program, Substitution)] = {
    t1.merge(t2).map {
      case (typeEnv, unifier) =>
        (program.copy(typeEnv = typeEnv).substitute(unifier), Substitution(unifier))
    }
  }
}
