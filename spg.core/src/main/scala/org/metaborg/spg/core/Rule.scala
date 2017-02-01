package org.metaborg.spg.core

import org.metaborg.spg.core.solver.{CGenRecurse, Constraint, NewScope}
import org.metaborg.spg.core.spoofax.Language
import org.metaborg.spg.core.spoofax.models.{Sort, SortAppl, SortVar}

/**
  * Representation of a constraint generation rule.
  *
  * @param name
  * @param pattern
  * @param sort
  * @param typ
  * @param scopes
  * @param constraints
  */
case class Rule(name: String, sort: Sort, pattern: Pattern, scopes: List[Pattern], typ: Option[Pattern], constraints: List[Constraint]) {
  /**
    * Alpha-rename variables to avoid name clashes.
    *
    * @param nameBinding
    * @return
    */
  def freshen(nameBinding: Map[String, String] = Map.empty): Rule = {
    pattern.freshen(nameBinding).map { case (nameBinding, pattern) =>
      scopes.freshen(nameBinding).map { case (nameBinding, scopes) =>
        typ.freshen(nameBinding).map { case (nameBinding, typ) =>
          constraints.freshen(nameBinding).map { case (nameBinding, constraints) =>
            Rule(name, sort, pattern, scopes, typ, constraints)
          }
        }
      }
    }
  }

  /**
    * Replace all variables (scopes) that are marked as "new" by a concrete
    * scope with a fresh name and remove the NewScope constraints.
    */
  def instantiate(): Rule = newScopes.foldLeft(this) {
    case (rule, c@NewScope(s)) =>
      (rule - c).substitute(s, TermAppl("s" + nameProvider.next))
  }

  /**
    * Apply given substitution to the rule.
    *
    * @param substitution
    * @return
    */
  def substitute(substitution: TermBinding): Rule = {
    Rule(
      name =
        name,
      sort =
        sort,
      pattern =
        pattern.substitute(substitution),
      typ =
        typ.map(_.substitute(substitution)),
      scopes =
        scopes.map(_.substitute(substitution)),
      constraints =
        constraints.map(_.substitute(substitution))
    )
  }

  /**
    * Substitute the first parameter by the second parameter.
    *
    * @param a
    * @param b
    * @return
    */
  def substitute(a: Var, b: Pattern): Rule = {
    substitute(Map(a -> b))
  }

  /**
    * Create a new rule with the given constraint removed.
    *
    * @param constraint
    */
  def -(constraint: Constraint) = {
    Rule(name, sort, pattern, scopes, typ, constraints - constraint)
  }

  /**
    * Create a new rule with the given constraint added.
    *
    * @param constraint
    */
  def +(constraint: Constraint) = {
    Rule(name, sort, pattern, scopes, typ, constraint :: constraints)
  }

  /**
    * Collect all NewScope constraints.
    */
  lazy val newScopes = constraints.collect {
    case c: NewScope =>
      c
  }

  /**
    * Get all recurse constraints.
    */
  lazy val recurses = constraints.collect {
    case c: CGenRecurse =>
      c
  }

  override def toString: String = {
    s"""Rule("$name", $sort, $pattern, $typ, $scopes, $constraints)"""
  }
}

object Rule {
  // Merge sort s1 with s2 by unifying s2 with any of the sorts in the injection closure of s1
  def mergeSorts(s1: Sort, s2: Sort)(implicit language: Language): Option[SortBinding] = s1 match {
    case SortVar(_) =>
      s1.unify(s2)
    case SortAppl(_, children) =>
      Sort
        .injectionsClosure(language.signatures, s1).view
        .flatMap(_.unify(s2))
        .headOption
  }

  // Merge type t1 with t2
  def mergeTypes(t1: Option[Pattern], t2: Option[Pattern]): Option[TermBinding] = (t1, t2) match {
    case (None, None) =>
      Some(Map.empty[Var, Pattern])
    case (Some(x), Some(y)) =>
      x.unify(y)
    case _ =>
      None
  }

  // Merge two lists of scopes
  def mergeScopes(s1: List[Pattern], s2: List[Pattern]): Option[TermBinding] = {
    s1.unify(s2)
  }

  // If p1 occurs as `As(p1, x)` in r.pattern, then (x unify p2) must be defined
  def mergePatterns(r: Program, p1: Pattern, p2: Pattern): Option[TermBinding] = {
    // Find all As(p1, x) in r.pattern
    val aliases = r.pattern.collect {
      case As(`p1`, term) =>
        List(term)
      case _ =>
        Nil
    }

    // Unify p2 with all of the found x's (TODO: I don't think the substitution is needed; just keep track of the unifier)
    val unifier = aliases.foldLeft(Option((Map.empty[Var, Pattern], p2))) {
      case (None, _) =>
        None
      case (Some((unifier, b)), a) =>
        b.unify(a, unifier).map(unifier =>
          (unifier, b.substitute(unifier))
        )
    }

    unifier.map {
      case (unifier, _) =>
        unifier
    }
  }
}
