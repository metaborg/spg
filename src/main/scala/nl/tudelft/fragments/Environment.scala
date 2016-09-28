package nl.tudelft.fragments

// Environment of declarations (TODO: should be sets instead of lists)
case class Environment(declarations: List[Pattern] = Nil) {
  // This environment shadows given environment e. It contains:
  // - all names from this environment
  // - the symbolic names from the hidden environment
  // - the concrete names from the hidden environment that do not occur in this enviroment.
  def shadows(that: Environment) = {
    val shadowed: List[Pattern] = that.declarations.filter {
      case _: SymbolicName =>
        true
      case ConcreteName(ns, n, p) =>
        !declarations.exists {
          case ConcreteName(ns2, n2, p2) if ns == ns2 && n2 == n =>
            true
          case _ =>
            false
        }
    }

    Environment(declarations ++ shadowed)
  }

  // Union two environments by unioning their sets of declarations
  def union(that: Environment) =
    Environment((declarations union that.declarations).distinct)
}
