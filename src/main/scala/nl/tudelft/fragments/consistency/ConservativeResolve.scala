package nl.tudelft.fragments.consistency

import nl.tudelft.fragments.spoofax.Language
import nl.tudelft.fragments.{CResolve, Graph, State, Var}

/**
  * Only allow a reference if it can resolve to a declaration such that the resulting program is consistent. This is
  * conservative, as a reference may be consistent if it cannot yet be resolved, but it prevents generating references
  * that cannot be resolved by only generating references that can be resolved.
  *
  * ConservativeResolve subsumes ResolveLight, i.e. if ConservativeResolve is used, it makes no sense to also use
  * ResolveLight.
  */
object ConservativeResolve {
  def isConsistent(state: State)(implicit language: Language): Boolean = {
    val resolveConstraints = state.resolve

    // TODO: In a Call(e, m), resolving m depends on the value of e. Currently, we only solve references that do not lead to a ScopeVar, as these are likely dependent on other references and hence need not be resolvable yet.

    val groundResolveConstraints = resolveConstraints
      .filter(resolve => !reachableScopeVar(state, resolve))

    val states = groundResolveConstraints.foldLeft(List(state)) {
      case (states, groundResolve) =>
        states.flatMap(state =>
          rewriteResolve(state.removeConstraint(groundResolve), groundResolve)
        )
    }

    states.nonEmpty
  }

  // Can the resolve reach a scope var? Then we can assume it's part of type-dependent name resolution
  def reachableScopeVar(state: State, resolve: CResolve)(implicit language: Language): Boolean = {
    val graph = Graph(state.facts)

    graph.reachableVarScopes(state.resolution)(graph.scope(resolve.n1)).nonEmpty
  }

  // Solve a CResolve constraint by resolving a reference to any of the declarations
  def rewriteResolve(state: State, resolve: CResolve)(implicit language: Language): List[State] = {
    if (state.resolution.contains(resolve.n1)) {
      List(state.substituteName(Map(resolve.n2.asInstanceOf[Var] -> state.resolution(resolve.n1))))
    } else {
      val choices = Graph(state.facts).res(state.resolution)(resolve.n1)

      choices.map(dec =>
        state
          .substituteName(Map(resolve.n2.asInstanceOf[Var] -> dec))
          .copy(resolution = state.resolution + (resolve.n1 -> dec))
      )
    }
  }
}
