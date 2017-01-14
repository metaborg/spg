package nl.tudelft.fragments

object Unifier {
  /**
    * Compute whether two patterns can be unified. This is a syntactic check,
    * it does not recognize that e.g. `A(X, b) != A(a, X)` cannot be unified.
    *
    * @return
    */
  def canUnify(t1: Pattern, t2: Pattern): Boolean = (t1, t2) match {
    case (t1: TermAppl, t2: TermAppl) if t1.cons == t2.cons && t1.arity == t2.arity =>
      (t1.children, t2.children).zipped.forall {
        case (p1, p2) =>
          canUnify(p1, p2)
      }
    case (Var(_), _) =>
      true
    case (_, _: Var) =>
      canUnify(t2, t1)
    case _ =>
      false
  }
}
