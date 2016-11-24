package nl.tudelft.fragments.consistency

import nl.tudelft.fragments.spoofax.Language
import nl.tudelft.fragments.{Graph, State}

/**
  * Check that for every resolve, at least one of the following must be satisfied:
  * - there exists a reachable declaration (same namespace)
  * - there exists a reachable scope such that there exists a recurse constraint that is parametrized with this scope.
  * - there exists a reachable scope that is a scope variable.
  *
  * This encodes the idea that if there is no such recurse constraint, we can never add a declaration for the reference.
  * Satisfying this constraint is no guarantee that the program is consistent though, as there may not be a
  * transformation that adds a declaration.
  */
object ResolveLight {
  def isConsistent(state: State)(implicit language: Language): Boolean = {
    val graph = Graph(state.facts)
    val recurse = state.recurse

    state.resolve.forall(resolve => {
      val scope = graph.scope(resolve.n1)

      // There exists a reachable declaration (same namespace)
      val decExists = graph
        .res(state.resolution)(resolve.n1)
        .nonEmpty

      // There exists a recurse such that it is parametrized with one of the reachable ground scopes
      val reachable = graph
        .reachableScopes(state.resolution)(scope)

      val recurseExists = recurse.exists(recurse => {
        recurse.scopes.exists(scope =>
          reachable.contains(scope)
        )
      })

      // There exists a reachable scope var
      val scopeVarExists = graph
        .reachableVarScopes(state.resolution)(scope)
        .nonEmpty

      decExists || recurseExists || scopeVarExists
    })
  }
}
