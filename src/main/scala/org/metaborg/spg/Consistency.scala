package org.metaborg.spg

import org.metaborg.spg.spoofax.Language

object Consistency {
  /**
    * Unsound and complete check if the partial program can be completed.
    *
    * @param rule
    * @return
    */
  def check(rule: Rule)(implicit l: Language): Boolean = {
    constraintsCheck(rule) && resolveCheck(rule)
  }

  /**
    * Check the satisfiability of the constraints.
    *
    * @param rule
    * @return
    */
  def constraintsCheck(rule: Rule) = rule.constraints.forall {
    case CFalse() =>
      false
    case CEqual(t1, t2) =>
      t1.unify(t2).nonEmpty
    case CInequal(t1, t2) =>
      t1.unify(t2).isEmpty
    case FSubtype(t1, t2) if t1.vars ++ t2.vars == Nil =>
      lazy val supertypeExists = rule.state.subtypeRelation.domain.contains(t1)
      lazy val cyclicSubtype = rule.state.subtypeRelation.isSubtype(t2, t1)

      !supertypeExists && !cyclicSubtype
    case _ =>
      true
  }

  /**
    * Check if for every reference at least one of the following holds:
    *  - there is a reachable declaration
    *  - there is a reachable scope variable
    *  - there is a reachable scope that is the parameter to a scope variable
    *
    * @param rule
    * @param l
    * @return
    */
  private def resolveCheck(rule: Rule)(implicit l: Language) = {
    def reachableDeclaration(graph: Graph, rule: Rule, ref: Pattern) = {
      graph.res(rule.state.resolution)(ref).nonEmpty
    }

    def reachableScopeVar(graph: Graph, rule: Rule, scope: Pattern) = {
      graph.reachableVarScopes(rule.state.resolution)(scope).nonEmpty
    }

    def reachableRecurse(graph: Graph, rule: Rule, scope: Pattern) = {
      (rule.recurse.flatMap(_.scopes) intersect graph.reachableScopes(rule.state.resolution)(scope)).nonEmpty
    }

    val graph = Graph(rule.constraints)

    rule.resolve.forall(resolve => {
      val scope = graph.scope(resolve.n1)

      reachableDeclaration(graph, rule, resolve.n1) || reachableScopeVar(graph, rule, scope) || reachableRecurse(graph, rule, scope)
    })
  }
}
