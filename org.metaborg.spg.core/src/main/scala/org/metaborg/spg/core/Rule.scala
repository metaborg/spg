package org.metaborg.spg.core

import org.metaborg.spg.core.resolution.Occurrence
import org.metaborg.spg.core.resolution.OccurrenceImplicits._
import org.metaborg.spg.core.solver.{CGDecl, CGRef, CGenRecurse, Constraint, NewScope}
import org.metaborg.spg.core.spoofax.Language
import org.metaborg.spg.core.spoofax.models.{Sort, SortAppl, SortVar}
import org.metaborg.spg.core.terms._

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
    * Instantiate a rule.
    *
    * - Replace all variables (scopes) that are marked as "new" by a concrete
    * scope with a fresh name and remove the NewScope constraints.
    *
    * - Replace meta-level variables in occurrences by object-level NameVar
    * terms.
    */
  def instantiate(): Rule = {
    // Substitute Var(x) by TermAppl("s1") when x is marked as a new scope.
    val r2 = newScopes.foldLeft(this) {
      case (rule, c@NewScope(s)) =>
        (rule - c).substitute(s, TermAppl("s" + nameProvider.next))
    }

    // Substitute Var(x) in an occurrence by TermAppl("NameVar", List(TermString("x1"))
    val occurrences = constraints.flatMap {
      case CGDecl(_, declaration) =>
        declaration.occurrence.name match {
          case v@Var(_) =>
            Some(v)
          case _ =>
            None
        }
      case CGRef(reference, _) =>
        reference.occurrence.name match {
          case v@Var(_) =>
            Some(v)
          case _ =>
            None
        }
      case _ =>
        None
    }

    val varToNameVar: TermBinding = occurrences.zipWith(variable =>
      TermAppl("NameVar", List(
        TermString("x" + nameProvider.next)
      ))
    ).toMap

    r2.substitute(varToNameVar)
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
    * Apply the given sort substitution to the program.
    *
    * @param binding
    * @return
    */
  def substituteSort(binding: SortBinding): Rule = {
    Rule(
      name =
        name,
      sort =
        sort.substituteSort(binding),
      pattern =
        pattern.substituteSort(binding),
      scopes =
        scopes,
      typ =
        typ,
      constraints =
        constraints.substituteSort(binding)
    )
  }

  /**
    * Merges two rules.
    *
    * If the given rule is not applicable (e.g. wrong rule name), returns None.
    *
    * @param recurse
    * @param rule
    * @param language
    * @return
    */
  def merge(recurse: CGenRecurse, rule: Rule)(implicit language: Language): Option[Rule] = {
    if (rule.name != recurse.name) {
      return None
    }

    // Instantiate and freshen the given rule
    val freshRule = rule
      .instantiate()
      .freshen()

    // Remove the recurse
    val merged = copy(constraints = constraints ++ freshRule.constraints - recurse)

    // Merge sorts, patterns, types, scopes
    Merger.mergeSorts(merged)(recurse.sort, freshRule.sort).flatMap(merged =>
      Merger.mergePatterns(merged)(recurse.pattern, freshRule.pattern).flatMap(merged =>
        Merger.mergeTypes(merged)(recurse.typ, freshRule.typ).flatMap(merged =>
          Merger.mergeScopes(merged)(recurse.scopes, freshRule.scopes).flatMap(merged =>
            Some(merged)
          )
        )
      )
    )
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
