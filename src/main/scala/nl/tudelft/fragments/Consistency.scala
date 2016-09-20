package nl.tudelft.fragments

import nl.tudelft.fragments.spoofax.Language
import nl.tudelft.fragments.spoofax.models.Sort

object Consistency {
  // Check for consistency. The higher the level, the stricter the check.
  def check(rule: Rule, level: Int = 2)(implicit language: Language): Boolean = {
    if (level >= 0) {
      // Solve CEqual and CTypeOf constraints; should result in single state.
      val states = solve(rule.state)

      if (level >= 1 && states.nonEmpty) {
        // Conservative subtype check: only allow x <? y if x <! y.
        val subtypingResult = conservativeSubtyping(states.head)

        // Every unresolved reference for which no recurse constraint with a reachable scope exists, must consistently resolve to any of the reachable declarations
        if (level >= 2) {
          val resolveResult = checkResolveScope(rule)
//          val resolveResult = checkResolveAddability(rule)

          states.nonEmpty && subtypingResult && resolveResult
        } else {
          states.nonEmpty && subtypingResult
        }
      } else {
        states.nonEmpty
      }
    } else {
      true
    }
  }

  // Rewrite constraints, returning Left(None) if we cannot process the constraint, Left(Some(states)) if we can process the state, and Right if we find an inconsistency
  def rewrite(c: Constraint, state: State): Either[Option[State], String] = c match {
    case CFalse() =>
      Right(s"Unable to solve CFalse()")
    case CTypeOf(n, t) if n.vars.isEmpty =>
      if (state.typeEnv.contains(n)) {
        Left(Some(state.addConstraint(CEqual(state.typeEnv(n), t))))
      } else {
        Left(Some(state.copy(typeEnv = state.typeEnv + (n -> t))))
      }
    case CEqual(t1, t2) =>
      if (t1.unify(t2).isEmpty) {
        Right(s"Unable to unify $t1 with $t2")
      } else {
        Left(Some(state.substitute(t1.unify(t2).get)))
      }
    case CInequal(t1, t2) if t1.vars.isEmpty && t2.vars.isEmpty =>
      if (t1 == t2) {
        Right(s"Terms $t1 equals $t2 violating inequality")
      } else {
        Left(Some(state))
      }
    case FSubtype(t1, t2) if (t1.vars ++ t2.vars).isEmpty =>
      if (state.subtypeRelation.domain.contains(t1)) {
        Right(s"$t1 already has a supertype, cannot have multiple supertypes")
      } else if (state.subtypeRelation.isSubtype(t2, t1)) {
        Right(s"$t2 is already a subtype of $t1, cannot have cyclic subytping")
      } else {
        val closure = for (ty1 <- state.subtypeRelation.subtypeOf(t1); ty2 <- state.subtypeRelation.supertypeOf(t2))
          yield (ty1, ty2)

        Left(Some(state.copy(subtypeRelation = state.subtypeRelation ++ closure)))
      }
    case _ =>
      Left(None)
  }

  // Solve constraints by type. Returns `None` if constraints contain a consistency or `Some(state)` with the resulting state.
  def solve(state: State): Option[State] = state match {
    case State(pattern, remaining, all, ts, resolution, subtype, conditions) =>
      for (c <- remaining) {
        val result = rewrite(c, State(pattern, remaining - c, all, ts, resolution, subtype, conditions))

        result match {
          case Left(None) =>
          /* noop */
          case Left(Some(result)) =>
            return solve(result)
          case Right(_) =>
            return None
        }
      }

      Some(state)
  }

  // Check that all subtyping constraints can be satisfied
  def conservativeSubtyping(state: State) = {
    // Build the subtype relation
    val subtypeRelation = state.constraints.foldLeft(state.subtypeRelation) {
      case (subtypeRelation, FSubtype(t1, t2)) if (t1.vars ++ t2.vars).isEmpty && !subtypeRelation.domain.contains(t1) && !subtypeRelation.isSubtype(t2, t1) =>
        val closure = for (ty1 <- subtypeRelation.subtypeOf(t1); ty2 <- subtypeRelation.supertypeOf(t2))
          yield (ty1, ty2)

        subtypeRelation ++ closure
      case (subtypeRelation, _) =>
        subtypeRelation
    }

    assert(subtypeRelation == state.subtypeRelation)

    // Verify the CSubtype constraints according to the subtype relation
    val validSubtyping = state.constraints.forall {
      case CSubtype(t1, t2) if (t1.vars ++ t2.vars).isEmpty =>
        subtypeRelation.isSubtype(t1, t2)
      case _ =>
        true
    }

    validSubtyping
  }

  // Check that unresolved references that won't get new declarations, can resolve to any of the declarations
  def checkResolveScope(rule: Rule)(implicit language: Language): Boolean = {
    val resolve = rule.resolve
    val recurse = rule.recurse
    val graph = Graph(rule.state.facts)

    for (CResolve(n1, n2) <- resolve) {
      val scopes = graph.reachableScopes(rule.state.resolution)(graph.scope(n1))
      val incomplete = scopes.exists(scope => recurse.exists(recurse => recurse.scopes.contains(scope)))
      val scopeVarReachable = canReachScopeVar(rule, graph.scope(n1))

      if (!incomplete && !scopeVarReachable) {
        val declarations = graph.res(rule.state.resolution)(n1)
        val compatible = declarations.exists { case (declaration, namingConstraint) =>
          Consistency.check(
            Solver.resolve(rule, CResolve(n1, n2), declaration)
          )
        }

        if (!compatible) {
          return false
        }
      }
    }

    true
  }

  /**
    * If there is:
    * - no reachable declaration
    * - no way to add a reachable declaration
    *
    * Then we will never add a declaration. If this is also the case for
    * ScopeVar, then we will not "solve" a declaration either. If both
    * is the case, the fragment is inconsistent.
    *
    */
  def checkResolveAddability(rule: Rule)(implicit language: Language): Boolean = {
    val resolve = rule.resolve
    val graph = Graph(rule.state.facts)

    for (CResolve(n1, _) <- resolve) {
      val scope = graph.scope(n1)
      val name = n1.asInstanceOf[Name]

      // If there is no reachable ScopeVar (TODO: This currently ignores well-formedness)
      if (!canReachScopeVar(rule, scope)) {
        // TODO: Maybe, there is a reachable declaration, but resolving to this declaration yields an inconsistent fragment (e.g. resolving to the main class).

        // And no reachable declaration
        if (graph.res(rule.state.resolution)(n1).isEmpty) {
          // TODO: Take namespace into account
          // And no way to add a reachable declaration
          if (!Check.declarationability(language.specification.rules, rule, scope, name.namespace, language.specification.params.wf)) {
            // TODO: And no way to add a reachable ScopeVar
            if (true) {
              return false
            }
          }
        }
      }
    }

    true
  }














  // For references for which we cannot add new declarations, we can compute consistency relative to the possible resolutions
  def decidedDeclarationsConsistency(rule: Rule)(implicit language: Language): Boolean =
    rule.resolve.forall {
      case c@CResolve(n1, n2: TermVar) =>
        val g = Graph(rule.state.facts)
        val s = g.scope(n1)

        if (!canAddDeclaration(Nil, rule, s, n1.asInstanceOf[Name].namespace, language.specification.rules) && !canReachScopeVar(rule, s)) {
          val declarations = g.res(rule.state.resolution)(n1)

          // There must exist a declaration that, if n1 resolves to it, yields a consistent fragment
          val consistent = declarations.exists { case (dec, cond) =>
            // Resolve n1 to dec by substituting n2 by dec
            val newRule = rule.copy(
              state = rule.state.copy(
                constraints = rule.state.constraints - c
              )
            ).substitute(Map(n2 -> dec))

            val newRule2 = newRule.copy(state =
              newRule.state.copy(
                resolution = newRule.state.resolution + (n1 -> dec),
                nameConstraints = cond ++ newRule.state.nameConstraints
              )
            )

            // Check consistency of the resulting rule
            Consistency.check(newRule2)
          }

          consistent
        } else {
          true
        }
      case c@CResolve(n1, n2: TermVar) =>
        println(rule)
        assert(false)
        false
    }

  // TODO. Now a dummy implementation to see effect. But this ignores e.g. Program(Nil, Assign(_, _)), which suffers from the same problem
  def canSatisfyType(rule: Rule): Boolean = {
    rule.pattern match {
      case TermAppl("Program", List(TermAppl("Nil", Nil), x)) =>
        val qvar = rule.pattern.find {
          case TermAppl("QVar", _) => true
          case _ => false
        }

        qvar match {
          case Some(_) => false
          case _ => true
        }
      case _ =>
        true
    }
  }

  /**
    * Check if we can reach a ScopeVar from the scope. TODO: Note that this ignores well-formedness..
    *
    * @param rule
    * @return
    */
  def canReachScopeVar(rule: Rule, scope: Scope)(implicit language: Language): Boolean = {
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
  def canAddDeclaration(seenSort: List[Sort], rule: Rule, scope: Scope, ns: String, bases: List[Rule])(implicit language: Language): Boolean = {
    val graph = Graph(rule.state.facts)

    // TODO: reachableScopes does not return ScopeVars. What if a ScopeVar is reachable? Then we cannot answer the question?
    val reachableScopes = graph.reachableScopes(rule.state.resolution)(scope)
    val recurseForReachableScopes = reachableScopes.flatMap(scope => recurseForScope(scope, rule.recurse))
    val rulesForRecurseConstraints = recurseForReachableScopes.flatMap(recurse =>
      bases.flatMap(base =>
        rule
          // Consistency checking disabled, because then we end in an infinite loop
          .mergex(recurse, base, 1)
          .map { case (_, nameBinding, sortBinding, typeBinding, scopeBinding) =>
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
          }
      )
    )

    rulesForRecurseConstraints.exists(rule => {
      // Does this new rule add a declaration?
      val graph = Graph(rule.state.facts)
      val env = graph.env(language.specification.params.wf, Nil, Nil, rule.state.resolution)(rule.scopes.head) // TODO: head is arbitrary
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

  //  // Consistency of resolve constraints
  //  def checkResolve(rule: Rule)(implicit language: Language): Boolean = {
  //    val noDec = (resolveConstraint: CResolve) => {
  //      !Solver
  //        .rewrite(resolveConstraint, rule.state.copy(constraints = rule.state.constraints - resolveConstraint))
  //        .map(state => rule.copy(state = state))
  //        .exists(Consistency.check)
  //    }
  //
  //    val noRecurse = (resolveConstraint: CResolve) => {
  //      val scope = Graph(rule.state.facts).scope(resolveConstraint.n1)
  //      val reachable = Graph(rule.state.facts).reachableScopes(rule.state.resolution)(scope.get)
  //
  //      !rule.state.constraints
  //        .filter(_.isInstanceOf[CGenRecurse])
  //        .map(_.asInstanceOf[CGenRecurse])
  //        .exists(_.scopes.exists(reachable.contains(_)))
  //    }
  //
  //    val noRoot = (resolveConstraint: CResolve) => {
  //      // TODO: This is a hack that only works for L3
  //      if (rule.state.pattern.asInstanceOf[TermAppl].cons == "Program") {
  //        true
  //      } else {
  //        val scope = Graph(rule.state.facts).scope(resolveConstraint.n1)
  //        val reachable = Graph(rule.state.facts).reachableScopes(rule.state.resolution)(scope.get)
  //
  //        !rule.scopes.exists(reachable.contains(_))
  //      }
  //    }
  //
  //    val noEdge = (resolveConstraint: CResolve) => {
  //      val scope = Graph(rule.state.facts).scope(resolveConstraint.n1)
  //      val reachable = Graph(rule.state.facts).reachableScopes(rule.state.resolution)(scope.get)
  //
  //      !reachable.exists(scope =>
  //        Graph(rule.state.facts).edges(scope).exists {
  //          case (_, _: ScopeVar) =>
  //            true
  //          case _ =>
  //            false
  //        }
  //      )
  //    }
  //
  //    val resolveConstraints = rule.state.constraints
  //      .filter(_.isInstanceOf[CResolve])
  //      .asInstanceOf[List[CResolve]]
  //
  //    val check = resolveConstraints.forall(resolveConstraint => {
  //      val a = noDec(resolveConstraint)
  //      val b = noRecurse(resolveConstraint)
  //      val c = noRoot(resolveConstraint)
  //      val d = noEdge(resolveConstraint)
  //
  //      !(a && b && c && d)
  //    })
  //
  //    check
  //  }


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
