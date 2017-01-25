package org.metaborg.spg

import org.metaborg.spg.solver.{CGenRecurse, Constraint, NewScope, State}
import org.metaborg.spg.spoofax.models.{SortAppl, SortVar}
import org.metaborg.spg.spoofax.Language
import org.metaborg.spg.spoofax.models.{Sort, SortAppl}

// Rule
case class Rule(name: String, sort: Sort, typ: Option[Pattern], scopes: List[Pattern], state: State) {
  def mergex(recurse: CGenRecurse, rule: Rule, level: Int)(implicit language: Language): Option[(Rule, Map[String, String], SortBinding, TermBinding, TermBinding)] = {
    val (nameBinding, freshRule) = rule.instantiate().freshen()

    val merged = for (
      sortUnifier <- Rule.mergeSorts(recurse.sort, freshRule.sort);
      typeUnifier <- Rule.mergeTypes(recurse.typ, freshRule.typ);
      scopeUnifier <- Rule.mergeScopes(recurse.scopes, freshRule.scopes);
      patternUnifier <- Rule.mergePatterns(this, recurse.pattern, freshRule.pattern)
    ) yield {
      val merged = copy(state = state.merge(recurse, freshRule.state))
        .substituteType(typeUnifier)
        .substituteScope(scopeUnifier)
        .substituteSort(sortUnifier)
        .substitutePattern(patternUnifier)

      Some((merged, nameBinding, sortUnifier, typeUnifier, scopeUnifier))
    }

    merged.flatten
  }

  // Shortcut when only the merged rule should be returned
  def merge(recurse: CGenRecurse, rule: Rule, level: Int)(implicit language: Language): Option[Rule] =
    mergex(recurse, rule, level).map(_._1)

  // Get all recurse constraints
  def recurse: List[CGenRecurse] =
    state.recurse

  // Get all resolve constraints
  def resolve =
    state.resolve

  // Get all constraints
  def constraints: List[Constraint] =
    state.constraints

  // Get all proper constraints
  lazy val properConstraints: List[Constraint] =
    state.constraints.filter(_.isProper)

  // Get the pattern
  def pattern: Pattern =
    state.pattern

  /**
    * Substitute scopes. A specialized version of `substitute` that only
    * substitutes in the AST pattern.
    *
    * @param binding
    * @return
    */
  def substitutePattern(binding: TermBinding): Rule =
    Rule(name, sort, typ, scopes, state.substitutePattern(binding))

  /**
    * Substitute scopes. A specialized version of `substitute` that ignores the
    * pattern during substitution.
    *
    * @param binding
    * @return
    */
  def substituteScope(binding: TermBinding): Rule =
    Rule(name, sort, typ.map(_.substituteScope(binding)), scopes.map(_.substituteScope(binding)), state.substituteScope(binding))

  def substituteType(binding: TermBinding): Rule =
    Rule(name, sort, typ.map(_.substituteType(binding)), scopes, state.substituteType(binding))

  def substituteSort(binding: SortBinding): Rule =
    Rule(name, sort.substituteSort(binding), typ, scopes, state.substituteSort(binding))

  def freshen(nameBinding: Map[String, String] = Map.empty): (Map[String, String], Rule) = {
    scopes.freshen(nameBinding).map { case (nameBinding, scopes) =>
      state.freshen(nameBinding).map { case (nameBinding, state) =>
        val newTyp = typ.map(_.freshen(nameBinding))

        newTyp
          .map { case (nameBinding, typ) =>
            (nameBinding, Rule(name, sort, Some(typ), scopes, state))
          }
          .getOrElse(
            (nameBinding, Rule(name, sort, typ, scopes, state))
          )
      }
    }
  }

  def withState(state: State) =
    this.copy(state = state)

  /**
    * Instantiate a rule by replacing all variables s that are marked as
    * "new s" by a concrete scope with a fresh name and removing the NewScope
    * constraints.
    */
  def instantiate(): Rule = {
    val newScopeConstraints = state.constraints.filter {
      case NewScope(_) =>
        true
      case _ =>
        false
    }

    val newScopeVariables = newScopeConstraints.map {
      case NewScope(variable) =>
        variable
    }

    val substitution = newScopeVariables.map(variable =>
      (variable, TermAppl("s" + nameProvider.next))
    ).toMap

    // Remove NewScope constraints
    val ruleWithoutNewScopes = newScopeConstraints.foldLeft(this) {
      case (rule, c@NewScope(_)) =>
        rule.withState(rule.state - c)
    }

    // Apply the substitution
    ruleWithoutNewScopes.substituteScope(substitution)
  }

  override def toString: String =
    s"""Rule("$name", $sort, $typ, $scopes, $state)"""
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
  def mergePatterns(r: Rule, p1: Pattern, p2: Pattern): Option[TermBinding] = {
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

  // Wrap state in a rule -- TODO: Nonsensical.. state vs. rule is a bad abstraction
  def fromState(state: State): Rule =
    Rule("Default", SortAppl("Module"), None, Nil, state)
}
