package nl.tudelft.fragments

object Builder {
  import Graph._

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

  // Build to resolve on a single rule
  def buildToResolve(rules: List[Rule], rule: Rule): Option[Rule] = {
    // TODO: We might be able to resolve a reference within the fragment itself; no need to merge (though we stil can)

    val ruleWithRefs: List[(Rule, List[Res])] = rulesWithRes(List(rule))

    val ruleWithRefsWithScopes: List[(Rule, List[(Res, List[Scope])])] = ruleWithRefs
      .map { case (rule, ress) =>
        (rule, ress.map { case res@Res(ref, _) =>
          (res, path(Nil, scope(ref, rule.constraints).head, rule.constraints, Nil).map(_._3))
        })
      }

    // TODO: the situation is more complex with direct imports. E.g. n -> (s2) -> (s3), there is no hole with scope s3. There is one with s2,
    val ruleWithRefsWithScopesWithPoints = ruleWithRefsWithScopes
      .map { case (rule, ressAndScopes) =>
        (rule, ressAndScopes.map { case (res, scopes) =>
          (res, scopes.map { case scope =>
            (scope, rule.points.filter(_._3.contains(scope)))
          })
        })
      }

    val ruleWithRefsWithScopesWithPointsWithRules = ruleWithRefsWithScopesWithPoints
      .map { case (rule, ressAndScopes) =>
        (rule, ressAndScopes.map { case (res, scopes) =>
          (res, scopes.map { case (scope, points) =>
            (scope, points.map { case point =>
              (point,
                // Merge other rule in this rule
                if (point._1.isInstanceOf[TermVar]) {
                  rules
                    // The sorts must unify
                    .filter(rule => {
                      point._1.asInstanceOf[TermVar].sort.unify(rule.sort).isDefined
                    })
                    // Other rule must have a reachable declaration from any of the merge-scopes (TODO: "exists" on list of scopes is wrong)
                    .filter(other => other.scopes.exists(s =>
                      decls(other, s).exists {
                        case (_, _, SymbolicName(ns, _), _) =>
                          ns == res.n1.namespace
                        // TODO: ConcreteName (+ ConcreteName should not be reserved)
                      }
                    ))
                    .map(other => {
                      // Merge other in rule at point
                      (rule, point._1.asInstanceOf[TermVar], other)

                      // TODO: Do the merge as well, check consistency of the result. We do not want to resolve an integer varref to a boolean vardec.
                    })
                  // Merge this rule in other rule
                } else {
                  rules
                    .flatMap(other => {
                      other.pattern.vars.flatMap(hole =>
                        if (hole.sort.unify(rule.sort).isDefined) {
                          if (hole.scope.exists(s =>
                            // TODO: We also want the declaration to have a compatible type
                            decls(other, s).exists {
                              case (_, _, SymbolicName(ns, _), _) =>
                                ns == res.n1.namespace
                              case (_, _, ConcreteName(ns, name, _), _) =>
                                ns == res.n1.namespace && name == res.n1.name // TODO: If ref is a symbolic name, then the names do not need to be equal.. ConcreteName should not be reserved. I.e.
                            })) {
                              // TODO: Do the merge as well.
                              Some((other, hole, rule))
                          } else {
                            None
                          }
                        } else {
                          None
                        }
                      )
                    })
                }
                )
            })
          })
        })
      }

    // Now flatten all the nested lists
    val tuples: List[(Rule, Res, Scope, (Pattern, Sort, List[Scope]), (Rule, TermVar, Rule))] = ruleWithRefsWithScopesWithPointsWithRules.flatMap { case (rule, ress) =>
      ress.flatMap { case (res, scopes) =>
        scopes.flatMap { case (scope, holes) =>
          holes.flatMap { case (hole, applicables) =>
            applicables.flatMap { case applicable =>
              List((rule, res, scope, hole, applicable))
            }
          }
        }
      }
    }

    if (tuples.nonEmpty) {
      val (rule, res, _, (_, _, _), (r1, x, r2)) = tuples.random
      val (merged, nameBinding) = r1.mergex(x, r2)

      // The merge may have changed the name of the Res-constraint that we are trying to fix, so apply same name substitution
      val newRes = res.substitute(nameBinding)

      println(rule)
      println(merged)

      val resolved = resolve(merged, newRes, merged.constraints)

      if (resolved.isDefined) {
        println(resolved)
        resolved
      } else {
        println("Could not resolve. Naming conflict?")
        None
      }
    } else {
      None
    }
  }

  // Merge fragments in such a way that we close resolution constraints (TODO: after resolving, we should canonicalize the constraints)
  def buildToResolve(rules: List[Rule]): List[Rule] = {
    val generated = rulesWithRes(rules).flatMap { case (rule, ress) =>
      buildToResolve(rules, rule)
    }

    generated ++ rules
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
        // Remove resolution constraint and substitute the unknown name by the resolvd name
        val resultingConstraints = (rule.constraints - res ++ conditions)
          .substituteName(Map(d -> n))

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
