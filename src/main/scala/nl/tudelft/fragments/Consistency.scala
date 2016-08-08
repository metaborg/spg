package nl.tudelft.fragments

object Consistency {
  // Check for consistency
  def check(rule: Rule): Boolean = {
    val result = checkTypeEquals(rule.state.constraints).map { case (termBinding) =>
      checkTypeOf(rule.state.constraints, termBinding)
    }

    result.isDefined && result.get && checkSubtyping(rule.state) && checkResolve(rule)
  }

  // Consistency of resolve constraints
  def checkResolve(rule: Rule): Boolean = {
    val noDec = (resolveConstraint: CResolve) => {
      !Solver
        .rewrite(resolveConstraint, rule.state.copy(constraints = rule.state.constraints - resolveConstraint))
        .map(state => rule.copy(state = state))
        .exists(Consistency.check)
    }

    val noRecurse = (resolveConstraint: CResolve) => {
      val scope = Graph(rule.state.facts).scope(resolveConstraint.n1)
      val reachable = Graph(rule.state.facts).reachableScopes(rule.state.resolution)(scope.get)

      !rule.state.constraints
        .filter(_.isInstanceOf[CGenRecurse])
        .map(_.asInstanceOf[CGenRecurse])
        .exists(_.scopes.exists(reachable.contains(_)))
    }

    val noRoot = (resolveConstraint: CResolve) => {
      // TODO: This is a hack that only works for L3
      if (rule.state.pattern.asInstanceOf[TermAppl].cons == "Program") {
        true
      } else {
        val scope = Graph(rule.state.facts).scope(resolveConstraint.n1)
        val reachable = Graph(rule.state.facts).reachableScopes(rule.state.resolution)(scope.get)

        !rule.scopes.exists(reachable.contains(_))
      }
    }

    val noEdge = (resolveConstraint: CResolve) => {
      val scope = Graph(rule.state.facts).scope(resolveConstraint.n1)
      val reachable = Graph(rule.state.facts).reachableScopes(rule.state.resolution)(scope.get)

      !reachable.exists(scope =>
        Graph(rule.state.facts).edges(scope).exists {
          case (_, _: ScopeVar) =>
            true
          case _ =>
            false
        }
      )
    }

    val resolveConstraints = rule.state.constraints
      .filter(_.isInstanceOf[CResolve])
      .asInstanceOf[List[CResolve]]

    val check = resolveConstraints.forall(resolveConstraint => {
      val a = noDec(resolveConstraint)
      val b = noRecurse(resolveConstraint)
      val c = noRoot(resolveConstraint)
      val d = noEdge(resolveConstraint)

      !(a && b && c && d)
    })

    check
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
  def checkTypeEquals(C: List[Constraint]): Option[TermBinding] = {
    val typeEquals = C.flatMap {
      case c: CEqual =>
        Some(c)
      case _ =>
        None
    }

    typeEquals.foldLeftWhile(Map.empty[Var, Pattern]) {
      case (termBinding, CEqual(t1, t2)) =>
        t1.unify(t2, termBinding)
    }
  }

  // Check if the TypeOf for same name unify
  def checkTypeOf(C: List[Constraint], termBinding: TermBinding = Map.empty): Boolean = {
    val typeOf = C.flatMap {
      case c: CTypeOf =>
        Some(c)
      case _ =>
        None
    }

    val uniqueTypeOf = typeOf.distinct

    uniqueTypeOf.groupBy(_.n).values.forall(typeOfs =>
      typeOfs.map(_.t).pairs.foldLeftWhile(termBinding) {
        case (termBinding, (t1, t2)) =>
          t1.unify(t2, termBinding)
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
    //case Eq(n1, n2) :: xs =>
    //  solveNaming(xs.map(_.substitute(Map(n1 -> n2))), disEqs.map(_.substitute(Map(n1 -> n2))))
    case Nil =>
      (Nil, disEqs)
  }
}
