package nl.tudelft.fragments

import nl.tudelft.fragments.spoofax.models.{Signature, Sort, SortAppl, SortVar}
import nl.tudelft.fragments.spoofax.Language

// Rule
case class Rule(sort: Sort, typ: Option[Pattern], scopes: List[Scope], state: State) {
  def mergex(recurse: CGenRecurse, rule: Rule, level: Int)(implicit language: Language): Option[(Rule, Map[String, String], SortBinding, TermBinding, ScopeBinding)] = {
    // Prevent naming conflicts by freshening the names in the other rule
    val (nameBinding, freshRule) = rule.freshen()

    // Unify sort, type, scope and merge the rules
    val merged = for (
      sortUnifier <- Rule.mergeSorts(recurse.sort, freshRule.sort);
      typeUnifier <- Rule.mergeTypes(recurse.typ, freshRule.typ);
      scopeUnifier <- Rule.mergeScopes(recurse.scopes, freshRule.scopes)
    ) yield {
      val merged = copy(state = state.merge(recurse, freshRule.state))
        .substitute(typeUnifier)
        .substituteSort(sortUnifier)
        .substituteScope(scopeUnifier)

      // The merge might have broken references. Restore these by adding name disequalities. TODO
      //val restored = restoreResolution(merged)
      val restored = merged

      // Check consistency
      if (Consistency.check(restored, level)) {
        Some((restored, nameBinding, sortUnifier, typeUnifier, scopeUnifier))
      } else {
        None
      }
    }

    merged.flatten
  }

  // Shortcut when only the merged rule should be returned
  def merge(recurse: CGenRecurse, rule: Rule, level: Int)(implicit language: Language): Option[Rule] =
    mergex(recurse, rule, level).map(_._1)

  // Fix broken references by adding name disequalities
  def restoreResolution(rule: Rule) = {
    // The merge may have broken existing resolutions, fix this
    val fixedRule = rule.state.resolution.bindings.foldLeft(rule) { case (rule, (ref, dec)) =>
      // Get the declarations that `ref` may resolve to and remove declarations longer than `dec` as they don't break the resolution
      val newResolves = Graph(rule.state.facts).res(rule.state.resolution)(ref)

      if (newResolves.length != 1 || (newResolves.length == 1 && newResolves.head._1 != dec)) {
        // TODO: We can use the constraint from newResolves here, as that already contains the necessary condition for resolving to that single name
        val newDisEqs = newResolves
          .filter(_._1 != dec)
          .map { case (newDec, _) => Diseq(dec, newDec) }

        rule.copy(state =
          rule.state.copy(nameConstraints =
            newDisEqs ++ rule.state.nameConstraints
          )
        )
      } else {
        rule
      }
    }

    // TODO) Sanity check: did we really restore the resolution? (Remove this code eventually)
    for ((ref, dec) <- fixedRule.state.resolution.bindings) {
      val newResolves = Graph(fixedRule.state.facts).res(fixedRule.state.resolution)(ref)

      if (newResolves.length != 1 || (newResolves.length == 1 && newResolves.head._1 != dec)) {
        println(ref)
        println(fixedRule)
        println(newResolves)

        assert(false, "Ook na fixen nog geshadowed?!")
      }
    }

    fixedRule
  }

  def recurse: List[CGenRecurse] =
    state.constraints
      .filter(_.isInstanceOf[CGenRecurse])
      .asInstanceOf[List[CGenRecurse]]

  def resolve =
    constraints
      .filter(_.isInstanceOf[CResolve])
      .asInstanceOf[List[CResolve]]

  // Backwards compatibility
  def constraints: List[Constraint] =
    state.constraints

  // Backwards compatibility
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
      Some(Map.empty[TermVar, Pattern])
    case (Some(x), Some(y)) =>
      x.unify(y)
    case _ =>
      None
  }

  // Merge sort s1 with s2
  def mergeScopes(s1: List[Scope], s2: List[Scope]) =
    s1.unify(s2)
}
