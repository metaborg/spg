package nl.tudelft.fragments

import nl.tudelft.fragments.spoofax.models.{Signature, Sort}

object Consistency {
  implicit val signatures = Strategy8.signatures

  implicit val rules = Strategy8.rules

  // Check for consistency
  def check(rule: Rule)(implicit signatures: List[Signature]): Boolean = {
    // Solve CEqual and CTypeOf constraints; should result in single state.
    val states = solve(rule.state)

    //val typeEqualsCheck = checkTypeEquals(rule.state.constraints)
    //val typeOfCheck = typeEqualsCheck.exists(checkTypeOf(rule.state.constraints, _))
    //typeEqualsCheck.isDefined && typeOfCheck && ...

    // If states is empty, CEqual or CTypeOf are not consistent
    states.nonEmpty &&
    // Detect CFalse
    notFalse(rule) &&
    // Conservative subtype check: only allow x <? y if x <! y.
    conservativeSubtyping(states.head)
    // Detect cyclic inheritance
    //checkCyclicSubtyping(states.head)
    // && checkResolve(rule) && decidedDeclarationsConsistency(rule) && canSatisfyType(rule)*/
  }

  // Rewrite constraints, returning Left(None) if we cannot process the constraint, Left(Some(states)) if we can process the state, and Right if we find an inconsistency
  def rewrite(c: Constraint, state: State): Either[Option[List[State]], String] = c match {
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
    case FSubtype(t1, t2) if (t1.vars ++ t2.vars).isEmpty =>
      if (state.subtypeRelation.domain.contains(t1)) {
        Right(s"$t1 is already a subtype of $t2, cannot have multiple supertypes")
      } else if (state.subtypeRelation.isSubtype(t2, t1)) {
        Right(s"$t2 is already a subtype of $t2, cannot have cyclic subytping")
      } else {
        val closure = for (ty1 <- state.subtypeRelation.subtypeOf(t1); ty2 <- state.subtypeRelation.supertypeOf(t2))
          yield (ty1, ty2)

        Left(Some(state.copy(subtypeRelation = state.subtypeRelation ++ closure)))
      }
    case CSubtype(t1, t2) if (t1.vars ++ t2.vars).isEmpty && state.subtypeRelation.isSubtype(t1, t2) =>
      Left(Some(state))
    case _ =>
      Left(None)
  }

  // Solve constraints by type. Returns a List[State] of possible resuting states, or `Nil` if it is not possible to solve all constraints of given type.
  def solve(state: State): List[State] = state match {
    case State(pattern, remaining, all, ts, resolution, subtype, conditions) =>
      for (c <- remaining) {
        val result = rewrite(c, State(pattern, remaining - c, all, ts, resolution, subtype, conditions))

        result match {
          case Left(None) =>
            /* noop */
          case Left(Some(states)) =>
            return states.flatMap(solve)
          case Right(_) =>
            return Nil
        }
      }

      List(state)
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

    // Verify the CSubtype constraints according to the subtype relation
    val validSubtyping = state.constraints.forall {
      case CSubtype(t1, t2) if (t1.vars ++ t2.vars).isEmpty =>
        subtypeRelation.isSubtype(t1, t2)
      case _ =>
        true
    }

    validSubtyping
  }

  // A false constraint can never be solved
  def notFalse(rule: Rule) =
    !rule.constraints.exists(_.isInstanceOf[CFalse])

  // For references for which we cannot add new declarations, we can compute consistency relative to the possible resolutions
  def decidedDeclarationsConsistency(rule: Rule): Boolean =
    rule.resolve.forall {
      case c@CResolve(n1, n2: TermVar) =>
        val g = Graph(rule.state.facts)
        val s = g.scope(n1).get

        if (!canAddDeclaration(Nil, rule, s, n1.asInstanceOf[Name].namespace, rules) && !canReachScopeVar(rule, s)) {
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
  def canAddDeclaration(seenSort: List[Sort], rule: Rule, scope: Scope, ns: String, bases: List[Rule])(implicit signatures: List[Signature]): Boolean = {
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
          }
          }
      )
    )

    rulesForRecurseConstraints.exists(rule => {
      // Does this new rule add a declaration?
      val graph = Graph(rule.state.facts)
      val env = graph.env(graph.wellFormedness, Nil, Nil, rule.state.resolution)(rule.scopes.head) // TODO: head is arbitrary
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
  def checkCyclicSubtyping(state: State): Boolean = {
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
    val typeEquals = C.collect {
      case c: CEqual => c
    }

    typeEquals.foldLeftWhile(Map.empty[TermVar, Pattern]) {
      case (termBinding, CEqual(t1, t2)) =>
        t1.unify(t2, termBinding)
    }
  }

  // Check if the TypeOf for same name unify
  def checkTypeOf(C: List[Constraint], termBinding: TermBinding = Map.empty): Boolean = {
    val typeOf = C.collect { case x: CTypeOf => x }
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
