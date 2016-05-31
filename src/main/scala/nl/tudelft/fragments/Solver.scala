package nl.tudelft.fragments

import scala.util.Random
import Graph._

object Solver {
  // Rewrite the given constraint in the given state to a list of new states
  def rewrite(c: Constraint, state: State): List[State] = c match {
    case TypeOf(n@SymbolicName(_, _), t) =>
      if (state.typeEnv.contains(n)) {
        state.addConstraint(TypeEquals(state.typeEnv(n), t))
      } else {
        State(state.constraints, state.facts, state.typeEnv + (n -> t), state.nameConstraints)
      }
    case TypeOf(n@ConcreteName(_, _, _), t) =>
      if (state.typeEnv.contains(n)) {
        state.addConstraint(TypeEquals(state.typeEnv(n), t))
      } else {
        State(state.constraints, state.facts, state.typeEnv + (n -> t), state.nameConstraints)
      }
    case TypeEquals(t1, t2) =>
      t1.unify(t2).map {
        case (typeBinding, nameBinding) =>
          state
            .substituteType(typeBinding)
            .substituteName(nameBinding)
      }
    // TODO: stable graph thingy
    case Res(n1@SymbolicName(_, _), n2@NameVar(_)) =>
      // TODO: What if there are no resolvable declarations? Are we allowed to process the constraint?
      // TODO: effect of resolution on incomplete scope graphs?
      for ((_, _, dec, cond) <- Random.shuffle(resolves(Nil, n1, state.facts, state.nameConstraints))) yield {
        // TODO: conditions should be first-class constraints, and they should be conistent
        State(
          state.constraints.substituteName(Map(n2 -> dec)),
          state.facts.substituteName(Map(n2 -> dec)),
          state.typeEnv,
          cond ++ state.nameConstraints
        )
      }
    // TODO: stable graph thingy
    case Res(n1@ConcreteName(_, _, _), n2@NameVar(_)) =>
      for ((_, _, dec, cond) <- Random.shuffle(resolves(Nil, n1, state.facts, state.nameConstraints))) yield {
        State(
          state.constraints.substituteName(Map[NameVar, Name](n2 -> dec)),
          state.facts.substituteName(Map(n2 -> dec)),
          state.typeEnv,
          cond ++ state.nameConstraints
        )
      }
    case AssocConstraint(n@SymbolicName(_, _), s@ScopeVar(_)) =>
      associated(n, state.facts).map(scope =>
        state.substituteScope(Map(s -> scope))
      )
    case _ =>
      Nil
  }

  // Solve constraints until no more constraints can be solved and return the resulting states
  def solvePartial(state: State): List[State] = state match {
    case State(Nil, all, ts, conditions) =>
      state
    case State(remaining, all, ts, conditions) =>
      for (c <- remaining) {
        val result = rewrite(c, State(remaining - c, all, ts, conditions))

        // As soon as a rewrite works, stick to it.
        if (result.nonEmpty) {
          return result.flatMap(solvePartial)
        }
      }

      state
  }

  // Solve all constraints, but give TypeEquals(_, _) and TypeOf(SymbolicName(_), _) precedence
  def solve2(state: State): List[State] = state match {
    case State(Nil, all, ts, conditions) =>
      state
    case State(remaining, all, ts, conditions) =>
      for (c <- remaining) {
        val result = rewrite(c, State(remaining - c, all, ts, conditions))

        // As soon as a rewrite works, stick to it.
        if (result.nonEmpty) {
          return result.flatMap(solve2)
        }
      }

      Nil
  }

  // Solve all constraints with the given solver state
  def solve(constraints: List[Constraint], state: State): List[State] =
    solve2(State(constraints.filter(_.isProper), constraints, state.typeEnv, constraints.filter(_.isInstanceOf[NamingConstraint])))

  // Solve all constraints with an empty solver state
  def solve(constraints: List[Constraint]): List[State] =
    solve2(State(constraints.filter(_.isProper), constraints, TypeEnv(), constraints.filter(_.isInstanceOf[NamingConstraint])))
}

/**
  * Representation of the solver state
  *
  * @param constraints      The (remaining) proper constraints
  * @param facts            The known facts
  * @param typeEnv          The typing environment
  * @param nameConstraints  The naming constraints
  */
case class State(constraints: List[Constraint], facts: List[Constraint], typeEnv: TypeEnv, nameConstraints: List[Constraint]) {
  def merge(state: State): State = {
    State(
      constraints =
        constraints ++ state.constraints,
      facts =
        facts ++ state.facts,
      typeEnv =
        typeEnv ++ typeEnv,
      nameConstraints =
        nameConstraints ++ nameConstraints
    )
  }

  def addConstraint(constraint: Constraint): State =
    copy(constraint :: constraints)

  def substituteScope(binding: ScopeBinding): State =
    copy(constraints.substituteScope(binding), facts.substituteScope(binding))

  def substituteType(binding: TypeBinding): State =
    copy(constraints.substituteType(binding), facts.substituteType(binding), typeEnv.substituteType(binding))

  def substituteName(binding: NameBinding): State =
    copy(constraints.substituteName(binding), facts.substituteName(binding), typeEnv.substituteName(binding))

  def freshen(nameBinding: Map[String, String]): (Map[String, String], State) =
    constraints.freshen(nameBinding).map { case (nameBinding, constraints) =>
      facts.freshen(nameBinding).map { case (nameBinding, all) =>
        typeEnv.freshen(nameBinding).map { case (nameBinding, typeEnv) =>
          nameConstraints.freshen(nameBinding).map { case (nameBinding, nameConstraints) =>
            (nameBinding, copy(constraints, all, typeEnv, nameConstraints))
          }
        }
      }
    }
}

object State {
  def apply(constraints: List[Constraint]) = {
    val (proper, facts) = constraints.partition(_.isProper)

    State(proper, facts, TypeEnv(), Nil)
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
}
