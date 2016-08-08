package nl.tudelft.fragments

// Environment of declarations
case class Environment(declarations: List[(Pattern, List[NamingConstraint])] = Nil) {
  // This environment shadows given environment e
  def shadows(that: Environment) = {
    val shadowed = that.declarations.map {
      case (x, c2) => (x, c2 ++ unequalTo(x, declarations))
    }

    Environment(declarations ++ shadowed)
  }

  // Union two environments
  def union(that: Environment) =
    Environment(declarations union that.declarations)

  // Create constraints x != y for all y in declarations
  private def unequalTo(x: Pattern, declarations: List[(Pattern, List[NamingConstraint])]) =
    declarations
      .filter { case (y, c1) =>
        y.isInstanceOf[Name] && x.isInstanceOf[Name] && x.asInstanceOf[Name].namespace == y.asInstanceOf[Name].namespace
      }
      .map { case (y, c1) =>
        Diseq(x, y)
      }
}
