package nl.tudelft.fragments

import scala.util.Random

object Solver {
  import Graph._

    // Solve single constraint
  def rewrite(c: Constraint, cs: List[Constraint], all: List[Constraint], ts: TypeEnv, conditions: List[Constraint]): Solution = c match {
    case TypeOf(n@SymbolicName(_, _), t) =>
      if (ts.contains(n)) {
        List((TypeEquals(ts(n), t) :: cs, all, ts, conditions))
      } else {
        List((cs, all, ts + (n -> t), conditions))
      }
    case TypeOf(n@ConcreteName(_, _, _), t) =>
      if (ts.contains(n)) {
        List((TypeEquals(ts(n), t) :: cs, all, ts, conditions))
      } else {
        List((cs, all, ts + (n -> t), conditions))
      }
    case TypeEquals(t1, t2) =>
      t1.unify(t2).map { case (typeBinding, nameBinding) =>
        (cs.substituteType(typeBinding).substituteName(nameBinding), all.substituteType(typeBinding).substituteName(nameBinding), ts.substituteType(typeBinding), conditions)
      }
    // TODO: stable graph thingy
    case Res(n1@SymbolicName(_, _), n2@NameVar(_)) =>
      // TODO: What if there are no resolvable declarations? Are we allowed to process the constraint?
      // TODO: effect of resolution on incomplete scope graphs?
      for ((_, _, dec, cond) <- Random.shuffle(resolves(Nil, n1, all, conditions))) yield {
        // TODO: conditions should be first-class constraints, and they should be conistent
        (cs.substituteName(Map(n2 -> dec)), all.substituteName(Map(n2 -> dec)), ts, cond ++ conditions)
      }
    // TODO: stable graph thingy
    case Res(n1@ConcreteName(_, _, _), n2@NameVar(_)) =>
      for ((_, _, dec, cond) <- Random.shuffle(resolves(Nil, n1, all, conditions))) yield {
        (cs.substituteName(Map[NameVar, Name](n2 -> dec)), all.substituteName(Map(n2 -> dec)), ts, cond ++ conditions)
      }
    case AssocConstraint(n@SymbolicName(_, _), s@ScopeVar(_)) =>
      associated(n, all).map(scope =>
        (cs.substituteScope(Map(s -> scope)), all.substituteScope(Map(s -> scope)), ts, conditions)
      )
    case _ =>
      Nil
  }

  // Solve all constraints non-deterministically, but give TypeEquals(_, _) and TypeOf(SymbolicName(_), _) precedence
  def solve2(C: List[Constraint], all: List[Constraint], ts: TypeEnv, conditions: List[Constraint]): Solution =
    if (C.nonEmpty) {
      // Give `TypeOf(SymbolicName(_), _)` precedence
      if (C.exists(c => c.isInstanceOf[TypeOf] && c.asInstanceOf[TypeOf].n.isInstanceOf[SymbolicName])) {
        val c = C.find(c => c.isInstanceOf[TypeOf] && c.asInstanceOf[TypeOf].n.isInstanceOf[SymbolicName]).get
        val result = rewrite(c, C-c, all, ts, conditions)

        result.flatMap((solve2 _).tupled)
      }
      // Give `TypeOf(ConcreteName(_), _)` precedence
      else if (C.exists(c => c.isInstanceOf[TypeOf] && c.asInstanceOf[TypeOf].n.isInstanceOf[ConcreteName])) {
        val c = C.find(c => c.isInstanceOf[TypeOf] && c.asInstanceOf[TypeOf].n.isInstanceOf[ConcreteName]).get
        val result = rewrite(c, C-c, all, ts, conditions)

        result.flatMap((solve2 _).tupled)
      }
      // Give `TypeEquals(_, _)` precedence
      else if (C.exists(_.isInstanceOf[TypeEquals])) {
        val c = C.find(_.isInstanceOf[TypeEquals]).get
        val result = rewrite(c, C-c, all, ts, conditions)

        result.flatMap((solve2 _).tupled)
      }
      // Try the other constraints
      else {
        for (c <- C) {
          val result = rewrite(c, C-c, all, ts, conditions)

          if (result.nonEmpty) {
            return result.flatMap((solve2 _).tupled)
          }
        }

        None
      }
    } else {
      List((C, all, ts, conditions))
    }

  // Solve all constraints
  def solve(constraints: List[Constraint]): Solution =
    solve2(constraints.filter(_.isProper), constraints, TypeEnv(), constraints.filter(_.isInstanceOf[NamingConstraint]))
}

case class TypeEnv(bindings: Map[Name, Type] = Map.empty) {
  def contains(n: Name): Boolean =
    bindings.contains(n)

  def apply(n: Name) =
    bindings(n)

  def +(e: (Name, Type)) =
    TypeEnv(bindings + e)

  def substituteType(typeBinding: TypeBinding) =
    TypeEnv(bindings.map { case (nameVar, typ) =>
      nameVar -> typ.substituteType(typeBinding)
    })
}
