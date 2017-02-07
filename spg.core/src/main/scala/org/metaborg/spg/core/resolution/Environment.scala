package org.metaborg.spg.core.resolution

import org.metaborg.spg.core.terms.Pattern

/**
  * Environment of declarations
  */
case class Environment(declarations: Set[Occurrence]) {
  /**
    * Create a new environment by having this environment shadow the given
    * environment.
    *
    * When environment e1 shadows environment e2, we create a new environment
    * with all declarations d1 in e1 and all declarations d2 in e2 for which
    * no ground occurrence with the same name exists in d1.
    *
    * @param e2
    * @return
    */
  def shadows(reference: Pattern, e2: Environment) = {
    val shadowed = e2.declarations.filter(d2 =>
      !declarations.exists(d1 =>
        (d1.isGround && d1.name == d2.name) && (!d2.isGround && d1.name == reference)
      )
    )

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
  def apply(declarations: Traversable[Occurrence]): Environment = {
    Environment(declarations.toSet)
  }

  def apply(): Environment = {
    Environment(Set.empty)
  }
}
