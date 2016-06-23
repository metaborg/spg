package nl.tudelft.fragments

import nl.tudelft.fragments.Graph._

// TODO: Solver does not take stable graph into account!
// TODO: Naming conditions should be first-class facts, and they should be conistent
object Solver {
  def rewrite(c: Constraint, state: State): List[State] = c match {
    case True() =>
      state
    case TypeOf(n, t) =>
      if (state.typeEnv.contains(n)) {
        state.addConstraint(TypeEquals(state.typeEnv(n), t))
      } else {
        state.copy(typeEnv = state.typeEnv + (n -> t))
      }
    case TypeEquals(t1, t2) =>
      t1.unify(t2).map {
        case (typeBinding, nameBinding) =>
          state
            .substituteType(typeBinding)
            .substituteName(nameBinding)
      }
    case Res(n1, n2@NameVar(_)) if resolves(Nil, n1, state.facts, state.nameConstraints).nonEmpty =>
      if (state.resolution.contains(n1)) {
        state.substituteName(Map(n2 -> state.resolution(n1)._2))
      } else {
        val choices = resolves(Nil, n1, state.facts, state.nameConstraints)

        choices.map { case (_, path, dec, cond) =>
          state
            .substituteName(Map(n2 -> dec))
            .copy(
              resolution = state.resolution + (n1 ->(path, dec)),
              nameConstraints = cond ++ state.nameConstraints
            )
        }
      }
    case AssocConstraint(n@SymbolicName(_, _), s@ScopeVar(_)) if associated(n, state.facts).nonEmpty =>
      associated(n, state.facts).map(scope =>
        state.substituteScope(Map(s -> scope))
      )
    case Supertype(t1, t2) if (t1.vars ++ t2.vars).isEmpty && !state.subtypeRelation.domain.contains(t1) && !state.subtypeRelation.isSupertype(t2, t1) =>
      val closure = for (ty1 <- state.subtypeRelation.subtypeOf(t1); ty2 <- state.subtypeRelation.supertypeOf(t2))
        yield (ty1, ty2)

      state.copy(subtypeRelation = state.subtypeRelation ++ closure)
    case Subtype(t1, t2) if (t1.vars ++ t2.vars).isEmpty && state.subtypeRelation.isSupertype(t1, t2) =>
      state
    case _ =>
      None
  }

  // Solve constraints until no more constraints can be solved and return the resulting states
  def solvePartial(state: State): List[State] = state match {
    case State(_, Nil, _, _, _, _, _) =>
      state
    case State(pattern, remaining, all, ts, resolution, subtype, conditions) =>
      // Do not solve resolution constraints during solvePartial, because we do not want to make "new" decisions, only
      // propagate "existing" knowledge. For example, we may be able to solve one resolution, but then fail on the
      // second, since there is not yet a corresponding declaration.
      val nonRes = remaining.filter(!_.isInstanceOf[Res])

      for (c <- nonRes) {
        val result = rewrite(c, State(pattern, remaining - c, all, ts, resolution, subtype, conditions))

        // As soon as a rewrite works, stick to it.
        if (result.nonEmpty) {
          return result.flatMap(solvePartial)
        }
      }

      state
  }

  // Solve all constraints, but give TypeEquals(_, _) and TypeOf(SymbolicName(_), _) precedence
  def solve2(state: State): List[State] = state match {
    case State(_, Nil, _, _, _, _, _) =>
      state
    case State(_, remaining, all, ts, resolution, subtype, conditions) =>
      for (c <- remaining) {
        val result = rewrite(c, State(remaining - c, all, ts, resolution, subtype, conditions))

        // As soon as a rewrite works, stick to it.
        if (result.nonEmpty) {
          return result.flatMap(solve2)
        }
      }

      None
  }

  // Solve all constraints in the given state
  def solve(state: State): List[State] =
    solve2(state)

  // Solve all constraints with an empty state (DEPRECATED)
  def solve(constraints: List[Constraint]): List[State] =
    solve2(State(constraints.filter(_.isProper), constraints, TypeEnv(), Resolution(), SubtypeRelation(), constraints.filter(_.isInstanceOf[NamingConstraint])))
}

/**
  * Representation of the solver state
  *
  * @param constraints     The (remaining) proper constraints
  * @param facts           The known facts
  * @param typeEnv         The typing environment
  * @param nameConstraints The naming constraints
  */
case class State(pattern: Pattern, constraints: List[Constraint], facts: List[Constraint], typeEnv: TypeEnv, resolution: Resolution, subtypeRelation: SubtypeRelation, nameConstraints: List[Constraint]) {
  def merge(recurse: Recurse, state: State): State = {
    State(
      pattern =
        pattern.substituteTerm(Map(recurse.pattern.asInstanceOf[TermVar] -> state.pattern)),
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
      nameConstraints =
        nameConstraints ++ state.nameConstraints
    )
  }

  def addConstraint(constraint: Constraint): State =
    copy(constraints = constraint :: constraints)

  def substituteScope(binding: ScopeBinding): State =
    copy(pattern.substituteScope(binding), constraints.substituteScope(binding), facts.substituteScope(binding))

  def substituteType(binding: TypeBinding): State =
    copy(pattern.substituteType(binding), constraints.substituteType(binding), facts.substituteType(binding), typeEnv.substituteType(binding), subtypeRelation = subtypeRelation.substituteType(binding))

  def substituteName(binding: NameBinding): State =
    copy(pattern.substituteName(binding), constraints.substituteName(binding), facts.substituteName(binding), typeEnv.substituteName(binding), subtypeRelation = subtypeRelation.substituteName(binding))

  def substituteSort(binding: SortBinding): State =
    copy(constraints = constraints.substituteSort(binding), facts = facts.substituteSort(binding))

  def freshen(nameBinding: Map[String, String]): (Map[String, String], State) =
    pattern.freshen(nameBinding).map { case (nameBinding, pattern) =>
      constraints.freshen(nameBinding).map { case (nameBinding, constraints) =>
        facts.freshen(nameBinding).map { case (nameBinding, all) =>
          typeEnv.freshen(nameBinding).map { case (nameBinding, typeEnv) =>
            resolution.freshen(nameBinding).map { case (nameBinding, resolution) =>
              subtypeRelation.freshen(nameBinding).map { case (nameBinding, subtypeRelation) =>
                nameConstraints.freshen(nameBinding).map { case (nameBinding, nameConstraints) =>
                  (nameBinding, copy(pattern, constraints, all, typeEnv, resolution, subtypeRelation, nameConstraints))
                }
              }
            }
          }
        }
      }
    }
}

object State {
  def apply(constraints: List[Constraint], facts: List[Constraint], typeEnv: TypeEnv, resolution: Resolution, subtypeRelation: SubtypeRelation, nameConstraints: List[Constraint]): State = {
    State(null, constraints, facts, typeEnv, resolution, subtypeRelation, nameConstraints)
  }

  def apply(pattern: Pattern, constraints: List[Constraint]): State = {
    val (proper, facts) = constraints.partition(_.isProper)

    State(pattern, proper, facts, TypeEnv(), Resolution(), SubtypeRelation(), Nil)
  }

  def apply(constraints: List[Constraint]): State = {
    val (proper, facts) = constraints.partition(_.isProper)

    State(proper, facts, TypeEnv(), Resolution(), SubtypeRelation(), Nil)
  }
}

/**
  * Representation of a typing environment
  *
  * @param bindings Bindings from names to types
  */
case class TypeEnv(bindings: Map[Name, Type] = Map.empty) {
  def contains(n: Name): Boolean =
    bindings.contains(n)

  def apply(n: Name) =
    bindings(n)

  def +(e: (Name, Type)) =
    TypeEnv(bindings + e)

  def ++(typeEnv: TypeEnv) =
    TypeEnv(bindings ++ typeEnv.bindings)

  def substituteType(typeBinding: TypeBinding): TypeEnv =
    TypeEnv(
      bindings.map { case (name, typ) =>
        name -> typ.substituteType(typeBinding)
      }
    )

  def substituteName(nameBinding: NameBinding): TypeEnv =
    TypeEnv(
      bindings.map { case (name, typ) =>
        name -> typ.substituteName(nameBinding)
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
    "TypeEnv(List(" + bindings.map { case (name, typ) => s"""Binding($name, $typ)""" }.mkString(", ") + "))"
}

case class Resolution(bindings: Map[Name, (Path, Name)] = Map.empty) {
  def contains(n: Name): Boolean =
    bindings.contains(n)

  def apply(n: Name) =
    bindings(n)

  def get(n: Name) =
    bindings.get(n)

  def +(e: (Name, (Path, Name))) =
    Resolution(bindings + e)

  def ++(resolution: Resolution) =
    Resolution(bindings ++ resolution.bindings)

  def freshen(nameBinding: Map[String, String]): (Map[String, String], Resolution) = {
    val freshBindings = bindings.toList.mapFoldLeft(nameBinding) { case (nameBinding, (n1, (path, n2))) =>
      n1.freshen(nameBinding).map { case (nameBinding, n1) =>
        n2.freshen(nameBinding).map { case (nameBinding, n2) =>
          (nameBinding, n1 -> (path, n2))
        }
      }
    }

    freshBindings.map { case (nameBinding, bindings) =>
      (nameBinding, Resolution(bindings.toMap))
    }
  }

  override def toString =
    "Resolution(List(" + bindings.map { case (n1, n2) => s"""Binding($n1, $n2)""" }.mkString(", ") + "))"
}

case class SubtypeRelation(bindings: List[(Type, Type)] = Nil) {
  def contains(n: Type): Boolean =
    bindings.exists(_._1 == n)

  def domain: List[Type] =
    bindings.map(_._1)

  def ++(subtypeRelation: SubtypeRelation) =
    SubtypeRelation(bindings ++ subtypeRelation.bindings)

  def ++(otherBindings: List[(Type, Type)]) =
    SubtypeRelation(bindings ++ otherBindings)

  // Returns all t2 such that t1 is a subtype of t2
  def supertypeOf(t1: Type): List[Type] =
    bindings.filter(_._1 == t1).map(_._2)

  // Returns all t1 that are a subtype of t2
  def subtypeOf(t2: Type): List[Type] =
    bindings.filter(_._2 == t2).map(_._1)

  // Checks whether t2 is the supertype of t1
  def isSupertype(t1: Type, t2: Type) =
    t1 == t2 || bindings.exists {
      case (ty1, ty2) =>
        ty1 == t1 && ty2 == t2
    }

  def substituteType(typeBinding: TypeBinding): SubtypeRelation =
    SubtypeRelation(
      bindings.map { case (t1, t2) =>
        t1.substituteType(typeBinding) -> t2.substituteType(typeBinding)
      }
    )

  def substituteName(nameBinding: NameBinding): SubtypeRelation =
    SubtypeRelation(
      bindings.map { case (t1, t2) =>
        t1.substituteName(nameBinding) -> t2.substituteName(nameBinding)
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
