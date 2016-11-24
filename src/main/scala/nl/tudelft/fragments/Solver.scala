package nl.tudelft.fragments

import nl.tudelft.fragments.spoofax.Language

object Solver {
  def rewrite(c: Constraint, state: State)(implicit language: Language): List[State] = c match {
    case CTrue() =>
      state
    case CTypeOf(n, t) if n.vars.isEmpty =>
      if (state.typeEnv.contains(n)) {
        state.addConstraint(CEqual(state.typeEnv(n), t))
      } else {
        state.copy(typeEnv = state.typeEnv + (n -> t))
      }
    case CEqual(t1, t2) =>
      t1.unify(t2).map(state.substitute)
    case CInequal(t1, t2) if t1.vars.isEmpty && t2.vars.isEmpty && t1 != t2 =>
      state
    case CResolve(n1, n2@Var(_)) if Graph(state.facts).res(state.resolution)(n1).nonEmpty =>
      if (state.resolution.contains(n1)) {
        state.substitute(Map(n2 -> state.resolution(n1)))
      } else {
        val choices = Graph(state.facts).res(state.resolution)(n1)

        choices.map { case dec =>
          state
            .substitute(Map(n2 -> dec))
            .copy(resolution = state.resolution + (n1 -> dec))
        }
      }
    case CAssoc(n@SymbolicName(_, _), s@ScopeVar(_)) if Graph(state.facts).associated(n).nonEmpty =>
      Graph(state.facts).associated(n).map(scope =>
        state.substituteScope(Map(s -> scope))
      )
    case FSubtype(t1, t2) if (t1.vars ++ t2.vars).isEmpty && !state.subtypeRelation.domain.contains(t1) && !state.subtypeRelation.isSubtype(t2, t1) =>
      val closure = for (ty1 <- state.subtypeRelation.subtypeOf(t1); ty2 <- state.subtypeRelation.supertypeOf(t2))
        yield (ty1, ty2)

      state.copy(subtypeRelation = state.subtypeRelation ++ closure)
    case CSubtype(t1, t2) if (t1.vars ++ t2.vars).isEmpty && state.subtypeRelation.isSubtype(t1, t2) =>
      state
    case CDistinct(Declarations(scope, namespace)) if scope.vars.isEmpty /* TODO: vars.isEmpty does not ensure groundness! */ =>
      val names = Graph(state.facts).declarations(scope, namespace)
      val combis = for (List(a, b, _*) <- names.combinations(2).toList) yield (a, b)

      state.addInequalities(combis)
    case _ =>
      Nil
  }

  // Solve a resolve constraint (x |-> d) by resolving x to z
  def resolve(rule: Rule, resolve: CResolve, z: Pattern): Rule = resolve match {
    case CResolve(n1, n2) =>
      val substitutedState = rule.state
        .copy(constraints = rule.state.constraints - CResolve(n1, n2))
        .substitute(Map(n2.asInstanceOf[Var] -> z))

      val resolvedState = substitutedState.copy(
        resolution = substitutedState.resolution + (n1 -> z)
      )

      rule.copy(state = resolvedState)
  }

  // Solve as many constraints as possible. Returns a List[State] of possible resuting states.
  def solveAny(state: State)(implicit language: Language): List[State] = state.constraints match {
    case Nil =>
      List(state)
    case _ =>
      for (c <- state.constraints) {
        val result = rewrite(c, state.removeConstraint(c))

        if (result.nonEmpty) {
          return result.flatMap(solveAny)
        }
      }

      List(state)
  }

  // Solve all constraints. Returns `Nil` if it is not possible to solve all constraints.
  def solvePrivate(state: State)(implicit language: Language): List[State] = state.constraints match {
    case Nil =>
      List(state)
    case _ =>
      for (constraint <- state.constraints) {
        val result = rewrite(constraint, state.removeConstraint(constraint))

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
}

/**
  * Representation of the solver state
  *
  * TODO: Remove distinction between constraints and facts..
  *
  * @param constraints     The (remaining) proper constraints
  * @param facts           The known facts
  * @param typeEnv         The typing environment
  */
case class State(pattern: Pattern, constraints: List[Constraint], facts: List[Constraint], typeEnv: TypeEnv, resolution: Resolution, subtypeRelation: SubtypeRelation, inequalities: List[(Pattern, Pattern)]) {
  /**
    * Get all wrapped constraints
    *
    * @return
    */
  def wrapped(): List[Constraint] =
    constraints.filter(_.isInstanceOf[WrappedConstraint])

  /**
    * Get all CResolve constraints
    *
    * @return
    */
  def resolve =
    constraints
      .filter(_.isInstanceOf[CResolve])
      .asInstanceOf[List[CResolve]]

  /**
    * Get all CGenRecurse constraints
    *
    * @return
    */
  def recurse =
    constraints
      .filter(_.isInstanceOf[CGenRecurse])
      .asInstanceOf[List[CGenRecurse]]

  def merge(recurse: CGenRecurse, state: State): State = {
    State(
      pattern =
        pattern.substitute(Map(recurse.pattern.asInstanceOf[Var] -> state.pattern)),
      constraints =
        (constraints ++ state.constraints) - recurse,
      facts =
        facts ++ state.facts,
      typeEnv =
        typeEnv ++ state.typeEnv,
      resolution =
        resolution ++ state.resolution,
      subtypeRelation =
        subtypeRelation ++ state.subtypeRelation,
      inequalities =
        inequalities ++ state.inequalities
    )
  }

  def removeConstraint(constraint: Constraint): State =
    copy(constraints = constraints - constraint)

  def addConstraint(constraint: Constraint): State =
    copy(constraints = constraint :: constraints)

  def addInequalities(inequals: List[(Pattern, Pattern)]) =
    copy(inequalities = inequals ++ inequalities)

  /**
    * Substitute a variable by a pattern.
    *
    * @param v
    * @param p
    * @return
    */
  def substitute(v: Var, p: Pattern): State =
    copy(pattern.substitute(Map(v -> p)), constraints.substitute(Map(v -> p)), facts.substitute(Map(v -> p)), typeEnv.substitute(Map(v -> p)), resolution.substitute(Map(v -> p)), subtypeRelation.substitute(Map(v -> p)), inequalities.substitute(Map(v -> p)))

  /**
    * Substitute the given map of variables to patterns.
    *
    * @param binding
    * @return
    */
  def substitute(binding: TermBinding): State =
    copy(pattern.substitute(binding), constraints.substitute(binding), facts.substitute(binding), typeEnv.substitute(binding), resolution.substitute(binding), subtypeRelation.substitute(binding), inequalities.substitute(binding))

  def substituteScope(binding: ScopeBinding): State =
    copy(pattern.substituteScope(binding), constraints.substituteScope(binding), facts.substituteScope(binding))

  def substituteSort(binding: SortBinding): State =
    copy(constraints = constraints.substituteSort(binding), facts = facts.substituteSort(binding))

  def freshen(nameBinding: Map[String, String]): (Map[String, String], State) =
    pattern.freshen(nameBinding).map { case (nameBinding, pattern) =>
      constraints.freshen(nameBinding).map { case (nameBinding, constraints) =>
        facts.freshen(nameBinding).map { case (nameBinding, all) =>
          typeEnv.freshen(nameBinding).map { case (nameBinding, typeEnv) =>
            resolution.freshen(nameBinding).map { case (nameBinding, resolution) =>
              subtypeRelation.freshen(nameBinding).map { case (nameBinding, subtypeRelation) =>
                (nameBinding, copy(pattern, constraints, all, typeEnv, resolution, subtypeRelation))
              }
            }
          }
        }
      }
    }
}

object State {
  // TODO: This is legacy..
  def apply(pattern: Pattern, constraints: List[Constraint]): State = {
    val (proper, facts) = constraints.partition(_.isProper)

    State(pattern, proper, facts, TypeEnv(), Resolution(), SubtypeRelation(), Nil)
  }

  def apply(constraints: List[Constraint]): State = {
    val (proper, facts) = constraints.partition(_.isProper)

    State(null, proper, facts, TypeEnv(), Resolution(), SubtypeRelation(), Nil)
  }
}

/**
  * Representation of a typing environment
  *
  * @param bindings Bindings from names to types
  */
case class TypeEnv(bindings: Map[Pattern, Pattern] = Map.empty) {
  def contains(n: Pattern): Boolean =
    bindings.contains(n)

  def apply(n: Pattern) =
    bindings(n)

  def +(e: (Pattern, Pattern)) =
    TypeEnv(bindings + e)

  def ++(typeEnv: TypeEnv) =
    TypeEnv(bindings ++ typeEnv.bindings)

  def substitute(termBinding: TermBinding): TypeEnv =
    TypeEnv(
      bindings.map { case (name, typ) =>
        name -> typ.substitute(termBinding)
      }
    )

  def freshen(nameBinding: Map[String, String]): (Map[String, String], TypeEnv) = {
    val freshBindings = bindings.toList.mapFoldLeft(nameBinding) { case (nameBinding, (name, typ)) =>
      name.freshen(nameBinding).map { case (nameBinding, name) =>
        typ.freshen(nameBinding).map { case (nameBinding, typ) =>
          (nameBinding, name -> typ)
        }
      }
    }

    freshBindings.map { case (nameBinding, bindings) =>
      (nameBinding, TypeEnv(bindings.toMap))
    }
  }

  override def toString =
    "TypeEnv(Map(" + bindings.map { case (name, typ) => s"""Binding($name, $typ)""" }.mkString(", ") + "))"
}

case class Resolution(bindings: Map[Pattern, Pattern] = Map.empty) {
  def contains(n: Pattern): Boolean =
    bindings.contains(n)

  def apply(n: Pattern) =
    bindings(n)

  def get(n: Pattern) =
    bindings.get(n)

  def +(e: (Pattern, Pattern)) =
    Resolution(bindings + e)

  def ++(resolution: Resolution) =
    Resolution(bindings ++ resolution.bindings)

  def substitute(binding: TermBinding): Resolution =
    Resolution(
      bindings.map { case (t1, t2) =>
        t1.substitute(binding) -> t2.substitute(binding)
      }
    )

  def size =
    bindings.size

  def freshen(nameBinding: Map[String, String]): (Map[String, String], Resolution) = {
    val freshBindings = bindings.toList.mapFoldLeft(nameBinding) { case (nameBinding, (n1, n2)) =>
      n1.freshen(nameBinding).map { case (nameBinding, n1) =>
        n2.freshen(nameBinding).map { case (nameBinding, n2) =>
          (nameBinding, n1 -> n2)
        }
      }
    }

    freshBindings.map { case (nameBinding, bindings) =>
      (nameBinding, Resolution(bindings.toMap))
    }
  }

  override def toString =
    "Resolution(Map(" + bindings.map { case (n1, n2) => s"Tuple2($n1, $n2)" }.mkString(", ") + "))"
}

case class SubtypeRelation(bindings: List[(Pattern, Pattern)] = Nil) {
  def contains(n: Pattern): Boolean =
    bindings.exists(_._1 == n)

  def domain: List[Pattern] =
    bindings.map(_._1)

  def ++(subtypeRelation: SubtypeRelation) =
    SubtypeRelation(bindings ++ subtypeRelation.bindings)

  def ++(otherBindings: List[(Pattern, Pattern)]) =
    SubtypeRelation(bindings ++ otherBindings)

  def +(pair: (Pattern, Pattern)) =
    SubtypeRelation(pair :: bindings)

  // Returns all t2 such that t1 <= t2
  def supertypeOf(t1: Pattern): List[Pattern] =
    t1 :: bindings.filter(_._1 == t1).map(_._2)

  // Returns all t1 such that t1 <= t2
  def subtypeOf(t2: Pattern): List[Pattern] =
    t2 :: bindings.filter(_._2 == t2).map(_._1)

  // Get all t2 such that t1 <: t2
  def get(ty: Pattern): List[Pattern] =
    bindings.filter(_._1 == ty).map(_._2)

  // Checks whether t1 <= t2
  def isSubtype(ty1: Pattern, ty2: Pattern): Boolean =
    ty1 == ty2 || get(ty1).contains(ty2)

  def substitute(termBinding: TermBinding): SubtypeRelation =
    SubtypeRelation(
      bindings.map { case (t1, t2) =>
        t1.substitute(termBinding) -> t2.substitute(termBinding)
      }
    )

  def freshen(nameBinding: Map[String, String]): (Map[String, String], SubtypeRelation) = {
    val freshBindings = bindings.mapFoldLeft(nameBinding) { case (nameBinding, (t1, t2)) =>
      t1.freshen(nameBinding).map { case (nameBinding, t1) =>
        t2.freshen(nameBinding).map { case (nameBinding, t2) =>
          (nameBinding, t1 -> t2)
        }
      }
    }

    freshBindings.map { case (nameBinding, bindings) =>
      (nameBinding, SubtypeRelation(bindings))
    }
  }

  override def toString =
    "SubtypeRelation(List(" + bindings.map { case (n1, n2) => s"""Binding($n1, $n2)""" }.mkString(", ") + "))"
}
