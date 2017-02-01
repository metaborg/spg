package org.metaborg.spg.core.resolution

import org.metaborg.spg.core.solver.{Diseq, NamingConstraint}
import org.metaborg.spg.core.Pattern

/**
  * Environment of declarations
  */
case class Environment(declarations: Set[(List[NamingConstraint], Pattern)] = Set()) {
  /**
    * Create a new environment by having this environment shadow the given
    * environment.
    *
    * When environment e1 shadows environment e2, we create a new environment
    * in which all declarations d2 in e2 are accompanied by constraints that
    * prevent them from shadowing declarations d1 in n1.
    *
    * @param e2
    * @return
    */
  def shadows(e2: Environment) = {
    val shadowed = e2.declarations.map {
      case (namingConstraint, d2) =>
        val inequalities = declarations.map {
          case (_, d1) =>
            Diseq(d1, d2)
        }

        (namingConstraint ++ inequalities, d2)
    }

    Environment(declarations ++ shadowed)
  }

  /**
    * Union two environments.
    *
    * We union two environments by creating a new environment that contains the
    * union of the declarations in each environment.
    *
    * @param that
    * @return
    */
  def union(that: Environment) =
    Environment(declarations union that.declarations)
}

object Environment {
  def apply(declarations: Traversable[(List[NamingConstraint], Pattern)]): Environment = {
    Environment(declarations.toSet)
  }
}
