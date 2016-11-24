package nl.tudelft.fragments

import nl.tudelft.fragments.spoofax.Language
import nl.tudelft.fragments.spoofax.models.{Sort, SortAppl, SortVar}

// Rule
case class Rule(sort: Sort, typ: Option[Pattern], scopes: List[Scope], state: State) {
  def mergex(recurse: CGenRecurse, rule: Rule, level: Int)(implicit language: Language): Option[(Rule, Map[String, String], SortBinding, TermBinding, ScopeBinding)] = {
    val (nameBinding, freshRule) = rule.freshen()

    val merged = for (
      sortUnifier <- Rule.mergeSorts(recurse.sort, freshRule.sort);
      typeUnifier <- Rule.mergeTypes(recurse.typ, freshRule.typ);
      scopeUnifier <- Rule.mergeScopes(recurse.scopes, freshRule.scopes)
    ) yield {
      val merged = copy(state = state.merge(recurse, freshRule.state))
        .substitute(typeUnifier)
        .substituteSort(sortUnifier)
        .substituteScope(scopeUnifier)

      if (Consistency.check(merged, level)) {
        Some((merged, nameBinding, sortUnifier, typeUnifier, scopeUnifier))
      } else {
        None
      }
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

  // Get all facts
  def facts: List[Constraint] =
    state.facts

  // Get the pattern
  def pattern: Pattern =
    state.pattern

  def substitute(binding: TermBinding): Rule =
    Rule(sort, typ.map(_.substitute(binding)), scopes, state.substitute(binding))

  def substituteScope(binding: ScopeBinding): Rule =
    Rule(sort, typ, scopes.substituteScope(binding), state.substituteScope(binding))

  def substituteSort(binding: SortBinding): Rule =
    Rule(sort.substituteSort(binding), typ, scopes, state.substituteSort(binding))

  def freshen(nameBinding: Map[String, String] = Map.empty): (Map[String, String], Rule) = {
    scopes.freshen(nameBinding).map { case (nameBinding, scopes) =>
      state.freshen(nameBinding).map { case (nameBinding, state) =>
        val newTyp = typ.map(_.freshen(nameBinding))

        newTyp
          .map { case (nameBinding, typ) =>
            (nameBinding, Rule(sort, Some(typ), scopes, state))
          }
          .getOrElse(
            (nameBinding, Rule(sort, typ, scopes, state))
          )
      }
    }
  }

  def withState(state: State) =
    this.copy(state = state)

  override def toString: String =
    s"""Rule($sort, $typ, $scopes, $state)"""
}

object Rule {
  // Merge sort s1 with s2 by unifying s2 with any of the sorts in the injection closure of s1
  def mergeSorts(s1: Sort, s2: Sort)(implicit language: Language): Option[SortBinding] = s1 match {
    case SortVar(_) =>
      s1.unify(s2)
    case SortAppl(_, children) =>
      Sort
        .injectionsClosure(language.signatures)(Set(s1)).view
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

  // Merge sort s1 with s2
  def mergeScopes(s1: List[Scope], s2: List[Scope]) =
    s1.unify(s2)
}
