package nl.tudelft.fragments

object Builder {
  import Graph._

  type Point = (Pattern, Sort, List[Scope])

  // Complete the given rule
  def build(rules: List[Rule], current: Rule, size: Int, up: Map[Sort, Int], down: Map[Sort, Int]): Option[Rule] = {
    val holes = current.pattern.vars

    // If we have a complete program, return
    if (holes.isEmpty && current.pattern.asInstanceOf[TermAppl].cons == "Program") {
      return Some(current)
    }

    // TODO: consult the inverse signature. If we have a class and we are at size = 17, then we will never get a program,
    // TODO:   since this requires Program(_, Cons(x, _)) which is size 21 and hence exceeds limit 20. So no need to go over the 5 possibilities
    // TODO: another example: if we have a method at size = 11, then to get a program we need Program(_, Cons(Class(_, _, _, Cons(x, _)), _))
    // TODO:   which has size = 21 and hence exceeds limit 20. So no need to go over the 8 possibilities here.
    // TODO: and we can do the same thing for going down: given a

    // TODO: Handle root cleanly. Is it logical to force generating the root first?
    if (current.pattern.asInstanceOf[TermAppl].cons != "Program") {
      // Consistency check (TODO: handle lists uniformly)
      if (up.contains(current.sort)) {
        if (up(current.sort) + current.pattern.size > size) {
          return None
        }
      }

      // Merge this rule into another rule
      val applicable = rules
        .filter(rule => rule.pattern.vars.exists(_.sort.unify(current.sort).isDefined))
        .filter(rule => rule.pattern.size + current.pattern.size + rule.pattern.vars.length + current.pattern.vars.length <= size)

      for (randomRule <- applicable.randomSubset(20)) {
        val randomRuleHoles = randomRule.pattern.vars.filter(hole => hole.sort.unify(current.sort).isDefined)
        val randomHole = randomRuleHoles.random

        val merged = randomRule
          .merge(randomHole, current)
          .substituteSort(randomHole.sort.unify(current.sort).get)

        // Check consistency
        if (Consistency.check(merged.constraints)) {
          val result = build(rules, merged, size, up, down)

          // Return if defined
          if (result.isDefined) {
            return result
          }
        }
      }
    } else {
      // Consistency check
      val minimal = holes.map(hole => down.getOrElse(hole.sort, 0)).sum
      if (minimal + current.pattern.size > size) {
        return None
      }

      val hole: TermVar = holes.random
      
      // Filter rules that are a) syntactically valid and b) balance the size
      val applicable = rules
        .filter(rule => rule.sort.unify(hole.sort).isDefined)
        .filter(rule => rule.pattern.size+rule.pattern.vars.length <= (2*size-current.pattern.size-current.pattern.vars.length)/current.pattern.vars.length)

      for (randomRule <- applicable.randomSubset(20)) {
        val merged = current
          .merge(hole, randomRule)
          .substituteSort(randomRule.sort.unify(hole.sort).get)

        // Check consistency
        if (Consistency.check(merged.constraints)) {
          val result = build(rules, merged, size, up, down)

          // Return if defined
          if (result.isDefined) {
            return result
          }
        }
      }
    }

    None
  }

  // Close a hole in rule
  def buildToClose(rules: List[Rule], rule: Rule): Option[Rule] = {
    for (hole <- rule.pattern.vars; other <- rules; if hole.sort.unify(other.sort).isDefined) {
      val merged = rule
        .merge(hole, other)
        .substituteSort(hole.sort.unify(other.sort).get)

      if (Consistency.check(merged.constraints)) {
        return Some(merged)
      }
    }

    None
  }

  // Combine a rule with its resolution constraints (flattened)
  def withRess(rule: Rule): List[(Rule, Res)] = {
    rulesWithRes(List(rule)).flatMap { case (rule, ress) =>
      ress.map(res =>
        (rule, res)
      )
    }
  }

  // Combine a (Rule, Res) with the scopes reachable from the reference in the resolution constraint (flattened)
  def withScopes(r: (Rule, Res)): List[(Rule, Res, Scope)] = {
    r match { case (rule, res@Res(ref, _)) =>
      path(Nil, scope(ref, rule.constraints).head, rule.constraints, Nil).map(_._3).map(scope =>
        (rule, res, scope)
      )
    }
  }

  // Combine a (Rule, Res, Scope) with the extension points, i.e. hole + root (flattened)
  // TODO: the situation is more complex with direct imports. E.g. n -> (s2) -> (s3), there is no hole with scope s3. There is one with s2,
  def withPoints(r: (Rule, Res, Scope)): List[(Rule, Res, Scope, Point)] = {
    r match { case (rule, res, scope) =>
      rule.points.filter(_._3.contains(scope)).map { case point =>
        (rule, res, scope, point)
      }
    }
  }

  // Combine a (Rule, Res, Scope, Point) with a mergepoint and another rule (flattened)
  def withOther(r: (Rule, Res, Scope, Point), rules: List[Rule]): List[(Rule, Res, Scope, Point, TermVar, Rule)] = {
    r match {
      // Merge other rule in this rule
      case (rule, res, scope, point@(_ : TermVar, _, _)) =>
        // The sorts must unify
        rules.filter(other =>
          point._1.asInstanceOf[TermVar].sort.unify(other.sort).isDefined
        ).map(other =>
          (rule, res, scope, point, point._1.asInstanceOf[TermVar], other)
        )
      // Merge this rule in other rule
      case (rule, res, scope, point@(_ : TermAppl, _, _)) =>
        rules.flatMap(other =>
          other.pattern.vars.flatMap(hole =>
            // The sorts must unify
            if (hole.sort.unify(rule.sort).isDefined) {
              Some((rule, res, scope, point, hole, other))
            } else {
              None
            }
          )
        )
    }
  }

  def withDecs(r: (Rule, Res, Scope, Point, TermVar, Rule)): List[(Rule, Res, Scope, Point, TermVar, Rule, Name)] = {
    r match {
      case (rule, res, scope, point, mergeHole, other) =>
        val reachableDeclarations = mergeHole.scope.flatMap(s =>
          decls(other, s)
            .filter {
              case (_, _, SymbolicName(ns, _), _) =>
                ns == res.n1.namespace
              case (_, _, ConcreteName(ns, name, pos), List()) =>
                ns == res.n1.namespace && res.n1.isInstanceOf[ConcreteName] && res.n1.name == name // TODO: We should also be able to resolve symbolic names to concrete names..
            }
            .filter { _ =>
              // The resolve may fail if the fragment becomes inconsistent. We test this here..
              point._1 match {
                // Merge other rule in this rule and resolve
                case _: TermVar =>
                  val (merged, nameBinding) = rule.mergex(mergeHole, other)
                  val newRes = res.substitute(nameBinding)
                  val resolved = resolve(merged, newRes, merged.constraints)

                  resolved.isDefined
                // Merge this rule in other rule
                case _ =>
                  val (merged, nameBinding) = other.mergex(mergeHole, rule)
                  val newRes = res.substitute(nameBinding)
                  val resolved = resolve(merged, newRes, merged.constraints)

                  resolved.isDefined
              }
            }
        )

        reachableDeclarations.map { case (_, _, dec, _) =>
          (rule, res, scope, point, mergeHole, other, dec)
        }
    }
  }

  // Build to resolve on a single rule
  def buildToResolve(rules: List[Rule], rule: Rule): Option[Rule] = {
    // TODO: We might be able to resolve a reference within the fragment itself; no need to merge (though we stil can)

    val ruleWithRess: List[(Rule, Res)] =
      withRess(rule)

    val ruleWithRessWithScopes: List[(Rule, Res, Scope)] =
      ruleWithRess.flatMap(withScopes)

    val ruleWithRefsWithScopesWithPoints =
      ruleWithRessWithScopes.flatMap(withPoints)

    val ruleWithRefsWithScopesWithPointsWithRules =
      ruleWithRefsWithScopesWithPoints.flatMap(withOther(_, rules))

    val ruleWithRefsWithScopesWithPointsWithRulesWithDecs =
      ruleWithRefsWithScopesWithPointsWithRules.flatMap(withDecs)

    if (ruleWithRefsWithScopesWithPointsWithRulesWithDecs.nonEmpty) {
      val (rule, res, _, (p, _, _), mergeHole, other, dec) = ruleWithRefsWithScopesWithPointsWithRulesWithDecs.random

      val (merged, nameBinding) = if (p.isInstanceOf[TermVar]) {
        rule.mergex(mergeHole, other)
      } else {
        other.mergex(mergeHole, rule)
      }

      // The merge may have changed the name of the Res-constraint that we are trying to fix, so apply same name substitution
      val newRes = res.substitute(nameBinding)
      val resolved = resolve(merged, newRes, merged.constraints)

      if (resolved.isDefined) {
        resolved
      } else {
        None
      }
    } else {
      None
    }
  }

  // Merge fragments in such a way that we close resolution constraints (TODO: after resolving, we should canonicalize the constraints)
  def buildToResolve(rules: List[Rule]): List[Rule] = {
    val generated = buildToResolve(rules, rulesWithRes(rules).random._1)

    if (generated.isDefined) {
      generated.get :: rules
    } else {
      rules
    }
  }

  // Get the declarations that are reachable from given scope (TODO: "visible from given scope" ignores that we are looking for resolutions of a name, which has a namespace. We don't need DisEq constraints if the namespaces don't match anyway.)
  def decls(rule: Rule, scope: Scope) =
    visible(Nil, scope, rule.constraints, Nil)

  // Get [Rule, [Res]]
  def rulesWithRes(rules: List[Rule]) = rules
    .filter(_.constraints.exists {
      case Res(n1, n2) => true
      case _ => false
    })
    .map(rule =>
      (rule, rule.constraints.flatMap {
        case res@Res(_, _) => Some(res)
        case _ => None
      })
    )

  // Try to solve the given resolution constraint and return a new rule with
  // the resolution constraint replaced by naming constraints.
  def resolve(rule: Rule, res: Res, all: List[Constraint]) = res match {
    case Res(n1, d@NameVar(_)) =>
      // All the names that we can resolve to
      val names = resolves(Nil, n1, all, Nil)

      // All the names that we can resolve to and do not cause an inconsistency in a) the naming constraints and b) the whole constraint problem
      val consistentNames = names.flatMap { case (_, _, n, conditions) =>
        // Remove resolution constraint and substitute the unknown name by the resolved name
        val resultingConstraints = (rule.constraints - res ++ conditions)
          .substituteName(Map(d -> n))

        // TODO: Solving might assign a concrete name to a name variable, but we do not propagate this to the pattern..
        Solver.solve(resultingConstraints, rule.state)


        // TODO: Just substituting names is not sufficient. E.g. with type-dependent name resolution, we also need to propagate to other constraints.. (see notes)
        // TODO: I.e. we need to _solve_ the constraints. But then we get a Solution that we need to attach to the rule.

        if (Consistency.checkNamingConditions(resultingConstraints) && Consistency.check(resultingConstraints)) {
          Some(resultingConstraints)
        } else {
          None
        }
      }

      // TODO: after choosing a name, there must still be a way to complete the program (e.g. the other references)

      if (consistentNames.nonEmpty) {
        val resultingConstraints = consistentNames.random

        Some(
          // TODO: besides settings the ref and dec name equal, we must also prevent the resolution from being altered

          rule.copy(
            constraints = resultingConstraints
          )
        )
      } else {
        None
      }
  }
}
