package nl.tudelft.fragments.consistency

import nl.tudelft.fragments.spoofax.Language
import nl.tudelft.fragments.{CGenRecurse, CResolve, Consistency, Graph, Pattern, Rule, Solver, State}

/**
  * Check that for every resolve, at least one of the following must be satisfied:
  * - there exists a reachable declaration (same namespace)
  *  - and resolving to this declaration is consistent
  * - there exists a reachable scope such that there exists a recurse constraint that is parametrized with this scope.
  * - there exists a reachable scope that is a scope variable.
  *
  * This encodes the idea that if there is no such recurse constraint, we can never add a declaration for the reference.
  * Satisfying this constraint is no guarantee that the program is consistent though, as there may not be a
  * transformation that adds a declaration.
  */
object ResolveLight {
  def isConsistent(state: State)(implicit language: Language): Boolean = {
    val graph = Graph(state.constraints)
    val recurse = state.recurse

    state.resolve.forall(resolve => {
      val scope = graph.scope(resolve.n1)

      /*if (declarationExists(state, resolve, graph, recurse) && !resolveableDeclarationExists(state, resolve, graph, recurse)) {
        println(s"More stringent on $resolve in ${Rule.fromState(state)}")
      }*/

      //declarationExists(state, resolve, graph, recurse) ||
      resolveableDeclarationExists(state, resolve, graph, recurse) ||
        reachableRecurse(state, resolve, graph, recurse, scope) ||
        scopeVarExists(state, resolve, graph, recurse, scope)
    })
  }

  /**
    * There exists a reachable declaration such that when we resolve to it, the
    * resulting rule is also consistent (i.e. recursively invoke consistency).
    */
  def resolveableDeclarationExists(state: State, resolve: CResolve, graph: Graph, recurse: List[CGenRecurse])(implicit language: Language) =
    Solver.solveResolve(state, resolve).exists(state =>
      Consistency.check(Rule.fromState(state))
    )

  /**
    * There exists a reachable declaration (same namespace).
    */
  def declarationExists(state: State, resolve: CResolve, graph: Graph, recurse: List[CGenRecurse]) =
    graph
      .res(state.resolution)(resolve.n1)
      .nonEmpty

  /**
    * There exists a recurse such that it is parametrized with one of the
    * reachable ground scopes.
    */
  def reachableRecurse(state: State, resolve: CResolve, graph: Graph, recurse: List[CGenRecurse], scope: Pattern) = {
    val reachable = graph
      .reachableScopes(state.resolution)(scope)

    recurse.exists(recurse => {
      recurse.scopes.exists(scope =>
        reachable.contains(scope)
      )
    })
  }

  /**
    * Get the recurse constraints that are parametrized with one of the
    * reachable ground scopes.
    */
  def reachableRecurses(state: State, resolve: CResolve, graph: Graph, recurses: List[CGenRecurse], scope: Pattern): List[CGenRecurse] = {
    val reachable = graph.reachableScopes(state.resolution)(scope)

    recurses.filter(recurse =>
      recurse.scopes.exists(scope =>
        reachable.contains(scope)
      )
    )
  }

  /**
    * There exists a reachable scope var.
    */
  def scopeVarExists(state: State, resolve: CResolve, graph: Graph, recurse: List[CGenRecurse], scope: Pattern) = {
    graph
      .reachableVarScopes(state.resolution)(scope)
      .nonEmpty
  }
}
