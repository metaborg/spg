package nl.tudelft.fragments

import scala.util.Random
import Graph._

// TODO: Solver does not take stable graph into account!
// TODO: Naming conditions should be first-class facts, and they should be conistent
// TODO: A resolution constraint can currently be wrongly solved if there are no declarations that it may resolve to!!!
object Solver {
  // Rewrite the given constraint in the given state to a list of new states
  def rewrite(c: Constraint, state: State): List[State] = c match {
    case TypeOf(n@SymbolicName(_, _), t) =>
      if (state.typeEnv.contains(n)) {
        state.addConstraint(TypeEquals(state.typeEnv(n), t))
      } else {
        state.copy(typeEnv = state.typeEnv + (n -> t))
      }
    case TypeOf(n@ConcreteName(_, _, _), t) =>
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
    case Res(n1@SymbolicName(_, _), n2@NameVar(_)) =>
      for ((_, _, dec, cond) <- Random.shuffle(resolves(Nil, n1, state.facts, state.nameConstraints))) yield {
        state
          .substituteName(Map(n2 -> dec))
          .copy(nameConstraints = cond ++ state.nameConstraints)
      }
    case Res(n1@ConcreteName(_, _, _), n2@NameVar(_)) =>
      for ((_, _, dec, cond) <- Random.shuffle(resolves(Nil, n1, state.facts, state.nameConstraints))) yield {
        state
          .substituteName(Map(n2 -> dec))
          .copy(nameConstraints = cond ++ state.nameConstraints)
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
    case State(_, Nil, _, _, _) =>
      state
    case State(pattern, remaining, all, ts, conditions) =>
      // Do not solve resolution constraints during solvePartial, because we do not want to make "new" decisions, only
      // propagate "existing" knowledge. For example, we may be able to solve one resolution, but then fail on the
      // second, since there is not yet a corresponding declaration.
      val nonRes = remaining.filter(!_.isInstanceOf[Res])

      for (c <- nonRes) {
        val result = rewrite(c, State(pattern, remaining - c, all, ts, conditions))

        // As soon as a rewrite works, stick to it.
        if (result.nonEmpty) {
          return result.flatMap(solvePartial)
        }
      }

      state
  }

  // Solve all constraints, but give TypeEquals(_, _) and TypeOf(SymbolicName(_), _) precedence
  def solve2(state: State): List[State] = state match {
    case State(_, Nil, _, _, _) =>
      state
    case State(_, remaining, all, ts, conditions) =>
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
case class State(pattern: Pattern, constraints: List[Constraint], facts: List[Constraint], typeEnv: TypeEnv, nameConstraints: List[Constraint]) {
  def merge(hole: TermVar, state: State): State = {
    State(
      pattern =
        pattern.substituteTerm(Map(hole -> state.pattern)),
      constraints =
        constraints ++ state.constraints,
      facts =
        facts ++ state.facts,
      typeEnv =
        typeEnv ++ state.typeEnv,
      nameConstraints =
        nameConstraints ++ state.nameConstraints
    )
  }

  def addConstraint(constraint: Constraint): State =
    copy(constraints = constraint :: constraints)

  def substituteScope(binding: ScopeBinding): State =
    copy(pattern.substituteScope(binding), constraints.substituteScope(binding), facts.substituteScope(binding))

  def substituteType(binding: TypeBinding): State =
    copy(pattern.substituteType(binding), constraints.substituteType(binding), facts.substituteType(binding), typeEnv.substituteType(binding))

  def substituteName(binding: NameBinding): State =
    copy(pattern.substituteName(binding), constraints.substituteName(binding), facts.substituteName(binding), typeEnv.substituteName(binding))

  def freshen(nameBinding: Map[String, String]): (Map[String, String], State) =
    pattern.freshen(nameBinding).map { case (nameBinding, pattern) =>
      constraints.freshen(nameBinding).map { case (nameBinding, constraints) =>
        facts.freshen(nameBinding).map { case (nameBinding, all) =>
          typeEnv.freshen(nameBinding).map { case (nameBinding, typeEnv) =>
            nameConstraints.freshen(nameBinding).map { case (nameBinding, nameConstraints) =>
              (nameBinding, copy(pattern, constraints, all, typeEnv, nameConstraints))
            }
          }
        }
      }
    }
}

object State {
  def apply(constraints: List[Constraint], facts: List[Constraint], typeEnv: TypeEnv, nameConstraints: List[Constraint]): State = {
    State(null, constraints, facts, typeEnv, nameConstraints)
  }

  def apply(constraints: List[Constraint]): State = {
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

  override def toString =
    "TypeEnv(List(" + bindings.map { case (name, typ) => s"""Binding($name, $typ)""" }.mkString(", ") + "))"
}
