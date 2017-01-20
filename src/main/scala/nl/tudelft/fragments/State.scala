package nl.tudelft.fragments

/**
  * Representation of the solver state
  *
  * @param typeEnv         The typing environment
  */
case class State(pattern: Pattern, constraints: List[Constraint], typeEnv: TypeEnv, resolution: Resolution, subtypeRelation: SubtypeRelation, inequalities: List[(Pattern, Pattern)]) {
  /**
    * Get all CResolve constraints
    *
    * @return
    */
  def resolve = constraints
    .filter(_.isInstanceOf[CResolve])
    .asInstanceOf[List[CResolve]]

  /**
    * Get all CGenRecurse constraints
    *
    * @return
    */
  def recurse = constraints
    .filter(_.isInstanceOf[CGenRecurse])
    .asInstanceOf[List[CGenRecurse]]

  def merge(recurse: CGenRecurse, state: State): State = {
    State(
      pattern =
        pattern.substitute(Map(recurse.pattern.asInstanceOf[Var] -> state.pattern)),
      constraints =
        (constraints - recurse) ++ state.constraints,
      typeEnv =
        typeEnv ++ state.typeEnv,
      resolution =
        resolution ++ state.resolution,
      subtypeRelation =
        subtypeRelation ++ state.subtypeRelation,
      inequalities =
        inequalities ++ state.inequalities
    )
  }

  /**
    * Create a new state in which the given constraint is removed.
    *
    * @param constraint
    * @return
    */
  def `-`(constraint: Constraint): State =
    copy(constraints = constraints - constraint)

  /**
    * Create a new state to which the given constraint is added.
    *
    * @param constraint
    * @return
    */
  def `+`(constraint: Constraint): State =
    copy(constraints = constraint :: constraints)

  def addBinding(nameType: (Pattern, Pattern)): State =
    copy(typeEnv = typeEnv + nameType)

  def addInequalities(inequals: List[(Pattern, Pattern)]) =
    copy(inequalities = inequals ++ inequalities)

  /**
    * Substitute the given map of variables to patterns.
    *
    * @param binding
    * @return
    */
  def substitute(binding: TermBinding): State =
    copy(pattern.substitute(binding), constraints.substitute(binding), typeEnv.substitute(binding), resolution.substitute(binding), subtypeRelation.substitute(binding), inequalities.substitute(binding))

  /**
    * Substitute the given type.
    *
    * @param binding
    * @return
    */
  def substituteType(binding: TermBinding): State =
    copy(pattern, constraints.substitute(binding), typeEnv.substitute(binding), resolution, subtypeRelation, inequalities)

  /**
    * Substitute the given map of variables to patterns.
    *
    * @param binding
    * @return
    */
  def substituteName(binding: TermBinding): State =
    copy(pattern, constraints.substitute(binding), typeEnv.substitute(binding), resolution, subtypeRelation, inequalities)

  /**
    * Substitute only in the pattern.
    *
    * @param binding
    * @return
    */
  def substitutePattern(binding: TermBinding): State =
    copy(pattern.substitute(binding), constraints.substitute(binding), typeEnv, resolution, subtypeRelation, inequalities)

  def substituteScope(binding: TermBinding): State =
    copy(pattern, constraints.substituteScope(binding), typeEnv.substituteScope(binding), resolution, subtypeRelation, inequalities)

  def substituteSort(binding: SortBinding): State =
    copy(constraints = constraints.substituteSort(binding))

  def freshen(nameBinding: Map[String, String]): (Map[String, String], State) =
    pattern.freshen(nameBinding).map { case (nameBinding, pattern) =>
      constraints.freshen(nameBinding).map { case (nameBinding, constraints) =>
        typeEnv.freshen(nameBinding).map { case (nameBinding, typeEnv) =>
          resolution.freshen(nameBinding).map { case (nameBinding, resolution) =>
            subtypeRelation.freshen(nameBinding).map { case (nameBinding, subtypeRelation) =>
              (nameBinding, copy(pattern, constraints, typeEnv, resolution, subtypeRelation))
            }
          }
        }
      }
    }
}

object State {
  def apply(pattern: Pattern, constraints: List[Constraint]): State = {
    State(pattern, constraints, TypeEnv(), Resolution(), SubtypeRelation(), Nil)
  }

  def apply(constraints: List[Constraint]): State = {
    State(null, constraints, TypeEnv(), Resolution(), SubtypeRelation(), Nil)
  }
}
