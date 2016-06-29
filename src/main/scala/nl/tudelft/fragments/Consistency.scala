package nl.tudelft.fragments

object Consistency {
  // TODO: Scope reachability checking!

  // Check for consistency
  def check(state: State): Boolean = {
    val result = checkTypeEquals(state.constraints).map { case (typeBinding, nameBinding) =>
      checkTypeOf(state.constraints, typeBinding, nameBinding)
    }

    result.isDefined && result.get && checkSubtyping(state)
  }

  // Check if there is still a way for the resolve constraints to be satisfied
  def checkResolutions(state: State): Boolean = {
    val noDec = (resolveConstraint: CResolve) => {
      Solver
        .rewrite(resolveConstraint, state.copy(constraints = state.constraints - resolveConstraint))
        .exists(Consistency.check)
    }

    val noRecurse = (resolveConstraint: CResolve) => {
      val scope = Graph(state.facts).scope(resolveConstraint.n1)
      val reachable = Graph(state.facts).reachableScopes(state.resolution)(scope.get)

      !state.constraints
        .filter(_.isInstanceOf[CGenRecurse])
        .map(_.asInstanceOf[CGenRecurse])
        .exists(_.scopes.exists(reachable.contains(_)))
    }

    val noEdge = (resolveConstraint: CResolve) => {
      val scope = Graph(state.facts).scope(resolveConstraint.n1)
      val reachable = Graph(state.facts).reachableScopes(state.resolution)(scope.get)

      !state.constraints
        .filter(_.isInstanceOf[CGDirectEdge])
        .map(_.asInstanceOf[CGDirectEdge])
        .exists(_.s2.isInstanceOf[ScopeVar]) // TODO: Currently, all scopes are "unknown" (i.e. ScopeVar). This needs fixing!
    }

    val resolveConstraints = state.constraints
      .filter(_.isInstanceOf[CResolve])
      .asInstanceOf[List[CResolve]]

    !resolveConstraints.exists(resolveConstraint =>
      noDec(resolveConstraint) && noRecurse(resolveConstraint) && noEdge(resolveConstraint)
    )
  }

  // Check for cycles in the combination of Supertype constraints and the built-up subtyping relation
  def checkSubtyping(state: State): Boolean = {
    val supertypeConstraints = state.constraints.flatMap {
      case c: FSubtype =>
        Some(c)
      case _ =>
        None
    }

    // Imaginary complete relation, if all supertype constraint would be added
    val completeRelation = supertypeConstraints.foldLeft(state.subtypeRelation) {
      case (subtypeRelation, FSubtype(t1, t2)) =>
        val closure = for (ty1 <- subtypeRelation.subtypeOf(t1); ty2 <- subtypeRelation.supertypeOf(t2))
          yield (ty1, ty2)

        subtypeRelation ++ closure
    }

    // Check for cyclic inheritance in the imaginary relation
    for ((ty1, ty2) <- completeRelation.bindings) {
      if (completeRelation.isSubtype(ty2, ty1)) {
        assert(false, "Inconsistent subtyping relation in state = " + state)
        return false
      }
    }

    true
  }

  // Check if the TypeEquals unify
  def checkTypeEquals(C: List[Constraint]): Option[(TypeBinding, NameBinding)] = {
    val typeEquals = C.flatMap {
      case c: CEqual =>
        Some(c)
      case _ =>
        None
    }

    typeEquals.foldLeftWhile((Map.empty[TypeVar, Type], Map.empty[NameVar, Name])) {
      case ((typeBinding, nameBinding), CEqual(t1, t2)) =>
        t1.unify(t2, typeBinding, nameBinding)
    }
  }

  // Check if the TypeOf for same name unify
  def checkTypeOf(C: List[Constraint], typeBinding: TypeBinding = Map.empty, nameBinding: NameBinding = Map.empty): Boolean = {
    val typeOf = C.flatMap {
      case c: CTypeOf =>
        Some(c)
      case _ =>
        None
    }

    val uniqueTypeOf = typeOf.distinct

    uniqueTypeOf.groupBy(_.n).values.forall(typeOfs =>
      typeOfs.map(_.t).pairs.foldLeftWhile((typeBinding, nameBinding)) {
        case ((typeBinding, nameBinding), (t1, t2)) =>
          t1.unify(t2, typeBinding, nameBinding)
      }.isDefined
    )
  }

  // Check if the naming conditions are consistent
  def checkNamingConditions(C: List[Constraint]): Boolean = {
    // Remove duplicates (the algorithm fails on duplicate disequality constraints) TODO: is this still the case? improve this!
    val unique = C.distinct

    val eqs: List[Eq] = unique
      .filter(_.isInstanceOf[Eq])
      .map(_.asInstanceOf[Eq])

    val disEqs = unique
      .filter(_.isInstanceOf[Diseq])
      .map(_.asInstanceOf[Diseq])

    val (_, updatedDisEqs) = solveNaming(eqs, disEqs)

    !updatedDisEqs.exists {
      case Diseq(n1, n2) => n1 == n2
    }
  }

  // Eliminate naming equalities by substituting
  def solveNaming(eqs: List[Eq], disEqs: List[Diseq]): (List[Eq], List[Diseq]) = eqs match {
    case Eq(n1, n2) :: xs =>
      solveNaming(xs.map(_.substitute(n1, n2)), disEqs.map(_.substitute(n1, n2)))
    case Nil =>
      (Nil, disEqs)
  }
}
