package nl.tudelft.fragments

import nl.tudelft.fragments.spoofax.Signatures
import nl.tudelft.fragments.spoofax.Signatures.Decl

// Rule
case class Rule(sort: Sort, typ: Option[Pattern], scopes: List[Scope], state: State) {
  def mergex(recurse: CGenRecurse, rule: Rule)(implicit signatures: List[Decl]): Option[(Rule, Map[String, String])] = {
    // Prevent naming conflicts by freshening the names in the other rule
    val (nameBinding, freshRule) = rule.freshen()

    // Unify sort, type, scope and merge the rules
    val merged = for (
      sortUnifier <- mergeSorts(recurse.sort, freshRule.sort);
      typeUnifier <- mergeTypes(recurse.typ, freshRule.typ);
      scopeUnifier <- recurse.scopes.unify(freshRule.scopes)
    ) yield {
      val merged = copy(state = state.merge(recurse, freshRule.state))
        .substitute(typeUnifier)
        .substituteSort(sortUnifier)
        .substituteScope(scopeUnifier)

      // The merge might have broken references. Restore these by adding name disequalities.
      restoreResolution(merged)
    }

    // Check consistency. E.g. the merge might have unified t1 with t2, but if t1 = Int, t2 = Bool, it's inconsistent
    merged.flatMap(rule =>
      if (Consistency.check(merged.get)) {
        Some((merged.get, nameBinding))
      } else {
        None
      }
    )
  }

  // Backwards compatibility
  def merge(recurse: CGenRecurse, rule: Rule)(implicit signatures: List[Decl]): Option[Rule] = {
    mergex(recurse, rule).map(_._1)
  }

  // Merge sorts s1, s2 by unifying s2 with any of the sorts in the injection closure of s1
  def mergeSorts(s1: Sort, s2: Sort)(implicit signatures: List[Decl]): Option[SortBinding] = {
    val possibleSorts = Signatures.injectionsClosure(Set(s1))

    possibleSorts.view
      .flatMap(_.unify(s2))
      .headOption
  }

  // Merge types
  def mergeTypes(t1: Option[Pattern], t2: Option[Pattern]): Option[TermBinding] = (t1, t2) match {
    case (None, None) =>
      Some(Map.empty[Var, Pattern])
    case (Some(x), Some(y)) =>
      x.unify(y)
    case _ =>
      None
  }

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

  def resolutionConstraints =
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

// TODO: Backwards compatibility
object Rule {
  def apply(pattern: Pattern, sort: Sort, typ: Pattern, scopes: List[Scope], state: State): Rule =
    Rule(sort, Some(typ), scopes, state.copy(pattern = pattern))
}
