package nl.tudelft.fragments

object Solver {
  import Graph._

  // Solve one constraint
  def solve(c: Constraint, cs: List[Constraint], all: List[Constraint], ts: List[Type]): Option[Substitution] = c match {
    case TypeOf(_, t@TypeVar(_)) =>
      for (ty <- ts) {
        val result = solve(cs.substituteType(Map(t -> ty)), all, ts)

        if (result.isDefined) {
          return Some((result.get._1 ++ Map(t -> ty), result.get._2, result.get._3))
        }
      }

      None
    case TypeEquals(t1, t2) =>
      for (u <- t1.unify(t2); r <- solve(cs.substituteType(u), all, ts))
        yield (r._1 ++ u, r._2, r._3)
    case Res(n1, n2) =>
      for ((_, dec, cond) <- resolves(n1, all)) {
        val result = solve(cs.substituteName(Map(n2 -> dec)), all, ts)

        if (result.isDefined) {
          return Some((result.get._1, result.get._2 ++ Map(n2 -> dec), result.get._3 ++ cond))
        }
      }

      None
    case _ =>
      solve(cs, all, ts)
  }

  // Solve all constraints
  def solve(C: List[Constraint], all: List[Constraint], ts: List[Type]): Option[Substitution] = C match {
    case x :: xs => solve(x, xs, all, ts)
    case Nil => Some(Map[TypeVar, Type](), Map[NameVar, NameVar](), List.empty)
  }

  // Solve all constraints
  def solve(constraints: List[Constraint], ts: List[Type]): Option[Substitution] =
    solve(constraints, constraints, ts)
}

object Graph {
  // Parent scope
  def parent(s: Scope, all: List[Constraint]): List[Scope] =
    all.flatMap {
      case Par(`s`, p) =>
        Some(p)
      case _ =>
        None
    }

  // Declarations
  def declarations(s: Scope, all: List[Constraint]): List[NameVar] =
    all.flatMap {
      case Dec(`s`, n) =>
        Some(n)
      case _ =>
        None
    }

  // Scope for name
  def scope(n: NameVar, all: List[Constraint]): List[Scope] =
    all.flatMap {
      case Dec(s, `n`) =>
        Some(s)
      case Ref(`n`, s) =>
        Some(s)
      case _ =>
        None
    }

  // Reachable scopes
  def path(s: Scope, all: List[Constraint]): List[(Path, Scope)] =
    parent(s, all).flatMap(path(_, all)).map { case (path, scope) =>
      (s :: path, scope)
    }

  // Reachable declarations
  def reachable(s: Scope, all: List[Constraint]): List[(Path, NameVar)] =
    path(s, all).flatMap { case (path, scope) =>
      declarations(scope, all).map(dec => (path, dec))
    }

  // Visible declarations
  def visible(s: Scope, all: List[Constraint]): List[(Path, NameVar, List[Diseq])] =
    reachable(s, all).map { case (path, name) =>
      val condition = reachable(s, all)
        .filter(_._1.length < path.length)
        .map { case (_, other) => Diseq(name, other) }

      (path, name, condition)
    }

  // Resolution
  def resolves(n: NameVar, all: List[Constraint]): List[(Path, NameVar, List[Diseq])] =
    scope(n, all).flatMap(visible(_, all))
}

case class Diseq(n1: NameVar, n2: NameVar)
