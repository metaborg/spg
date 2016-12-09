package nl.tudelft.fragments

import nl.tudelft.fragments.spoofax.Language
import nl.tudelft.fragments.spoofax.models.{Sort, SortAppl, SortVar}

// Rule
case class Rule(name: String, sort: Sort, typ: Option[Pattern], scopes: List[Pattern], state: State) {
  def mergex(recurse: CGenRecurse, rule: Rule, level: Int)(implicit language: Language): Option[(Rule, Map[String, String], SortBinding, TermBinding, TermBinding)] = {
    if (recurse.name != rule.name) {
      return None
    }

    val (nameBinding, freshRule) = rule.freshen()

    val merged = for (
      sortUnifier <- Rule.mergeSorts(recurse.sort, freshRule.sort);
      typeUnifier <- Rule.mergeTypes(recurse.typ, freshRule.typ);
      scopeUnifier <- Rule.mergeScopes(recurse.scopes, freshRule.scopes);
      patternUnifier <- Rule.mergePatterns(this, recurse.pattern, freshRule.pattern)
    ) yield {
      val merged = copy(state = state.merge(recurse, freshRule.state))
        .substitute(typeUnifier)
        .substitute(scopeUnifier)
        .substituteSort(sortUnifier)
        .substitute(patternUnifier)

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
    Rule(name, sort, typ.map(_.substitute(binding)), scopes.map(_.substitute(binding)), state.substitute(binding))

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
  def mergeScopes(s1: List[Pattern], s2: List[Pattern]): Option[TermBinding] =
    s1.unify(s2)

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

  /**
    * Compute alpha-equivalence. r1 and r2 are alpha-equivalent if there exists
    * a renaming r such that r(r1) == r2 and r1 == r(r2). We consider two rules
    * equal if their patterns are equal, since everything else can be derived
    * from the pattern.
    *
    * @param r1
    * @param r2
    * @return
    */
//  def equivalence(r1: Rule, r2: Rule): Boolean =
//    subsumes(r1, r2).isDefined && subsumes(r2, r1).isDefined

  /**
    * Compute if r1 subsumes r2. r1 subsumes r2 if there exists a renaming r
    * of all variables in r1.pattern such that s(r1.pattern) = r2.pattern.
    *
    * @param r1
    * @param r2
    * @return
    */
//  def subsumes(r1: Rule, r2: Rule): Option[Map[String, String]] = {
//    Pattern.subsumes(r1.pattern, r2.pattern)/*.flatMap(renaming =>
//      Resolution.subsumes(r1.state.resolution, r2.state.resolution, renaming)
//    )*/
//  }

  // Wrap state in a rule -- TODO: Nonsensical.. state vs. rule is a bad abstraction
  def fromState(state: State): Rule =
    Rule("Default", SortAppl("Module"), None, Nil, state)
}
