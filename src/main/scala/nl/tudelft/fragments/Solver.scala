package nl.tudelft.fragments

object Solver {
  type TypeSub = Map[TypeVar, Type]

  // Solve one constraint
  def solve(c: Constraint, cs: List[Constraint], ts: List[Type]): Option[TypeSub] = c match {
    case TypeOf(_, t@TypeVar(_)) =>
      for (ty <- ts) {
        val result = solve(cs.substituteType(Map(t -> ty)), ts)

        if (result.isDefined) {
          return Some(result.get ++ Map(t -> ty))
        }
      }

      None
    case TypeEquals(t1, t2) =>
      for (u <- t1.unify(t2); r <- solve(cs.substituteType(u), ts))
        yield r
    case _ =>
      Some(Map())
  }

  // Solve all constraints
  def solve(C: List[Constraint], ts: List[Type]): Option[TypeSub] = C match {
    case x :: xs => solve(x, xs, ts)
    case Nil => Some(Map.empty)
  }
}
