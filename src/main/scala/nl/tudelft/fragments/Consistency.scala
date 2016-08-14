package nl.tudelft.fragments

import nl.tudelft.fragments.spoofax.{Signatures, Specification}

object Consistency {
  implicit val signatures = Strategy8.signatures

  implicit val rules = Strategy8.rules

  // Check for consistency
  def check(rule: Rule)(implicit signatures: List[Signatures.Decl]): Boolean = {
    val typeEqualsCheck = checkTypeEquals(rule.state.constraints).map { case (termBinding) =>
      checkTypeOf(rule.state.constraints, termBinding)
    }

    val resolveCheck = rule.resolutionConstraints.forall(resolve => {
      val graph = Graph(rule.state.facts)

      // TODO: This is not correct for e.g. Program(_, QVar(_, x)). x cannot be resolved, and we cannot add a declaration for x, but x leads to an import, so we cannot reason about this!
      !(
        // No reachable declarations
        graph.res(rule.state.resolution)(resolve.n1).isEmpty &&
        // Cannot add a reachable declaration
        !canAddDeclaration(Nil, rule, graph.scope(resolve.n1).get, resolve.n1.asInstanceOf[Name].namespace, rules) &&
        // TODO: Cannot add a reachable ScopeVar -- in this case, we don't know whether the fragment is consistent
        !canReachScopeVar(rule, graph.scope(resolve.n1).get)
      )
    })

    typeEqualsCheck.isDefined && typeEqualsCheck.get && checkSubtyping(rule.state) && checkResolve(rule) && resolveCheck
  }

  /**
    * Check if we can reach a ScopeVar from the scope. TODO: Note that this
    * ignores well-formedness..
    *
    * @param rule
    * @return
    */
  def canReachScopeVar(rule: Rule, scope: Scope): Boolean = {
    val graph = Graph(rule.state.facts)
    val reachableScopes = graph.reachableScopes(rule.state.resolution)(scope)

    reachableScopes.exists(scope =>
      graph.edges(scope).exists(_._2.vars.nonEmpty)
    )
  }

  /**
    * Check if a transformation permits adding a declaration from the given
    * scope for the given namespace.
    *
    * If a ScopeVar is reachable from the scope, we cannot determine the
    * answer.
    *
    * @return None if the answer cannot be computed; Some(Boolean) otherwise.
    */
  def canAddDeclaration(seenSort: List[Sort], rule: Rule, scope: Scope, ns: String, bases: List[Rule])(implicit signatures: List[Signatures.Decl]): Boolean = {
    val graph = Graph(rule.state.facts)

    // TODO: reachableScopes does not return ScopeVars. What if a ScopeVar is reachable? Then we cannot answer the question?
    val reachableScopes = graph.reachableScopes(rule.state.resolution)(scope)
    val recurseForReachableScopes = reachableScopes.flatMap(scope => recurseForScope(scope, rule.recurse))
    val rulesForRecurseConstraints = recurseForReachableScopes.flatMap(recurse =>
      bases.flatMap(base =>
        rule
          // Consistency checking disabled, because then we end in an infinite loop
          .mergex(recurse, base, checkConsistency = false)
          .map { case (_, nameBinding, sortBinding, typeBinding, scopeBinding) => {
            // TODO: Not sure if this substituting is okay? Do we need to use nameBinding as well?
            val r = base
              //.substitute(typeBinding)
              .substituteSort(sortBinding)
              .substituteScope(scopeBinding)

            // TODO: Sanity check. Remove in future.
            r.recurse.exists(recurse =>
              if (recurse.pattern.isInstanceOf[TermAppl]) {
                println("Error")

                true
              } else {
                false
              }
            )

            r
          }}
      )
    )

    rulesForRecurseConstraints.exists(rule => {
      // Does this new rule add a declaration?
      val graph = Graph(rule.state.facts)
      val env = graph.env(graph.wellFormedness, Nil, Nil, rule.state.resolution)(rule.scopes(0)) // TODO: head is arbitrary
      val declarations = env.declarations
      val declarationsCorrectNs = declarations.filter(d => d._1.isInstanceOf[Name] && d._1.asInstanceOf[Name].namespace == ns)

      if (declarationsCorrectNs.nonEmpty) {
        true
      } else {
        if (!seenSort.contains(rule.sort)) {
          // TODO: We pass along the sort, but totally ignore the well-formedness
          // TODO: head is arbitrary now, we should pass along the actual scope in the list
          canAddDeclaration(rule.sort :: seenSort, rule, rule.scopes.head, ns, bases)
        } else {
          false
        }
      }
    })
  }

  // Compute the recurse constraints that take the scope
  def recurseForScope(scope: Scope, recurse: List[CGenRecurse]) =
    recurse.filter(_.scopes.exists(_ == scope))

  // Compute all sorts that we can get to through recurse constraints
  def sortClosure(rule: Rule) = {
    val sorts = rule.recurse.map(_.sort)

    for (sort <- sorts) {

    }
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
        //assert(false, "Inconsistent subtyping relation in state = " + state)
        // TODO: We get here sometimes because we resolve a parent reference too early to the class itself, which is cyclic-inheritance, which fails
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

    typeEquals.foldLeftWhile(Map.empty[TermVar, Pattern]) {
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
