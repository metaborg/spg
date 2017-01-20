package nl.tudelft.fragments

import nl.tudelft.fragments.spoofax.Language

object Solver {
  /**
    * Gets a constraint and a state. Returns `Nil` if the constraint cannot be
    * solved or a list of states if it can be solved in multiple ways.
    *
    * @param c
    * @param state
    * @param solveResolve
    * @param language
    * @return
    */
  def rewrite(c: Constraint, state: State, solveResolve: Boolean)(implicit language: Language): List[State] = c match {
    case CTrue() =>
      state
    case CTypeOf(n, t) if n.vars.isEmpty =>
      if (state.typeEnv.contains(n)) {
        state.addConstraint(CEqual(state.typeEnv(n), t))
      } else {
        state.addBinding(n -> t)
      }
    case CEqual(t1, t2) =>
      t1.unify(t2).map(state.substitute)
    case CInequal(t1, t2) if !Unifier.canUnify(t1, t2) =>
      state
    case CResolve(n1, n2) =>
      if (solveResolve) {
        val declarations = Graph(state.constraints).res(state.resolution)(n1)

        declarations.flatMap(declaration =>
          declaration.unify(n2).map(unifier =>
            state
              .substitute(unifier)
              .copy(resolution = state.resolution + (n1 -> declaration))
          )
        )
      } else {
        Nil
      }
    case CAssoc(n@SymbolicName(_, _), s@Var(_)) if Graph(state.constraints).associated(n).nonEmpty =>
      Graph(state.constraints).associated(n).map(scope =>
        state.substitute(Map(s -> scope))
      )
    case FSubtype(t1, t2) if (t1.vars ++ t2.vars).isEmpty && !state.subtypeRelation.domain.contains(t1) && !state.subtypeRelation.isSubtype(t2, t1) =>
      val closure = for (ty1 <- state.subtypeRelation.subtypeOf(t1); ty2 <- state.subtypeRelation.supertypeOf(t2))
        yield (ty1, ty2)

      state.copy(subtypeRelation = state.subtypeRelation ++ closure)
    case CSubtype(t1, t2) if (t1.vars ++ t2.vars).isEmpty && state.subtypeRelation.isSubtype(t1, t2) =>
      state
    case CDistinct(Declarations(scope, namespace)) if scope.vars.isEmpty =>
      val names = Graph(state.constraints).declarations(scope, namespace)
      val combis = for (List(a, b, _*) <- names.combinations(2).toList) yield (a, b)

      state.addInequalities(combis)
    case _ =>
      Nil
  }

  // Solve as many constraints as possible. Returns a List[State] of possible resuting states.
  def solveAny(state: State)(implicit language: Language): List[State] = state.constraints.filter(_.isProper) match {
    case Nil =>
      List(state)
    case _ =>
      for (c <- state.constraints) {
        val result = rewrite(c, state.removeConstraint(c), solveResolve = false)

        if (result.nonEmpty) {
          return result.flatMap(solveAny)
        }
      }

      List(state)
  }

  // Solve all constraints. Returns `Nil` if it is not possible to solve all constraints.
  def solvePrivate(state: State)(implicit language: Language): List[State] = state.constraints.filter(_.isProper) match {
    case Nil =>
      List(state)
    case _ =>
      for (constraint <- state.constraints) {
        val result = rewrite(constraint, state.removeConstraint(constraint), solveResolve = true)

        if (result.nonEmpty) {
          return result.flatMap(solve)
        }
      }

      Nil
  }

  // Solve constraints after sorting on priority
  def solve(state: State)(implicit language: Language): List[State] = {
    val sortedState = state.copy(
      constraints = state.constraints.sortBy(_.priority)
    )

    solvePrivate(sortedState)
  }

  // Solve the given resolve constraint
  def solveResolve(state: State, resolve: CResolve)(implicit language: Language): List[State] = resolve match {
    case CResolve(n1, n2) =>
      val declarations = Graph(state.constraints).res(state.resolution)(n1)

      declarations.flatMap(declaration =>
        declaration.unify(n2).map(unifier =>
          state
            .removeConstraint(resolve)
            .substitute(unifier)
            .copy(resolution = state.resolution + (n1 -> declaration))
        )
      )
  }
}
