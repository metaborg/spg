package nl.tudelft.fragments

import nl.tudelft.fragments.spoofax.Language

object Consistency {
  /**
    * Unsound and complete check if the partial program can be completed.
    *
    * @param rule
    * @return
    */
  def check(rule: Rule)(implicit l: Language): Boolean = {
    val graph = Graph(rule.constraints)

    rule.resolve.forall(resolve => {
      val scope = graph.scope(resolve.n1)

      reachableDeclaration(graph, rule, resolve.n1) || reachableScopeVar(graph, rule, scope) || reachableRecurse(graph, rule, scope)
    })
  }

  private def reachableDeclaration(graph: Graph, rule: Rule, ref: Pattern) = {
    graph.res(rule.state.resolution)(ref).nonEmpty
  }

  private def reachableScopeVar(graph: Graph, rule: Rule, scope: Pattern) = {
    graph.reachableVarScopes(rule.state.resolution)(scope).nonEmpty
  }

  private def reachableRecurse(graph: Graph, rule: Rule, scope: Pattern) = {
    (rule.recurse.flatMap(_.scopes) intersect graph.reachableScopes(rule.state.resolution)(scope)).nonEmpty
  }
}
