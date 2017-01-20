package nl.tudelft.fragments

import nl.tudelft.fragments.consistency.{ConservativeSubtype, DeclarationAddability, ResolveLight}
import nl.tudelft.fragments.spoofax.Language

object Consistency {
  // Check for consistency. The higher the level, the stricter the check.
  def check(rule: Rule, level: Int = 2)(implicit language: Language): Boolean = {
    if (level >= 0) {
      // There should not be an inconsistency (e.g. in CEqual and CTypeOf constraints)
      val states = solve(rule.state)

      if (level >= 1) {
        val consistencyCheck = states.exists(state =>
          /*DeclarationAddability.isConsistent(Rule.fromState(state)) &&*/ ResolveLight.isConsistent(state) /*ConservativeResolve.isConsistent(state)*/ && ConservativeSubtype.isConsistent(state)
        )

        states.nonEmpty && consistencyCheck
      } else {
        states.nonEmpty
      }
    } else {
      true
    }
  }

  // Rewrite constraints, returning Left(None) if we cannot process the constraint, Left(Some(states)) if we can process the state, and Right if we find an inconsistency
  def rewrite(c: Constraint, state: State)(implicit language: Language): Either[List[State], String] = c match {
    case CFalse() =>
      Right(s"Unable to solve CFalse()")
    case CTrue() =>
      Left(List(state))
    case CTypeOf(n, t) if n.vars.isEmpty =>
      if (state.typeEnv.contains(n)) {
        Left(List(state.addConstraint(CEqual(state.typeEnv(n), t))))
      } else {
        Left(List(state.copy(typeEnv = state.typeEnv + (n -> t))))
      }
    case CEqual(t1, t2) =>
      if (t1.unify(t2).isEmpty) {
        Right(s"Unable to unify $t1 with $t2")
      } else {
        Left(List(state.substituteType(t1.unify(t2).get)))
      }
    case CInequal(t1, t2) =>
      if (!Unifier.canUnify(t1, t2)) {
        Left(List(state))
      } else {
        if (t1 == t2) {
          Right(s"Term $t1 equals $t2 violating inequality")
        } else {
          Left(Nil)
        }
      }
    case FSubtype(t1, t2) if (t1.vars ++ t2.vars).isEmpty =>
      if (state.subtypeRelation.domain.contains(t1)) {
        Right(s"$t1 already has a supertype, cannot have multiple supertypes")
      } else if (state.subtypeRelation.isSubtype(t2, t1)) {
        Right(s"$t2 is already a subtype of $t1, cannot have cyclic subytping")
      } else {
        val closure = for (ty1 <- state.subtypeRelation.subtypeOf(t1); ty2 <- state.subtypeRelation.supertypeOf(t2))
          yield (ty1, ty2)

        Left(List(state.copy(subtypeRelation = state.subtypeRelation ++ closure)))
      }
    case CAssoc(n@SymbolicName(_, _), s@Var(_)) if Graph(state.constraints).associated(n).nonEmpty =>
      Left(Graph(state.constraints).associated(n).map(scope =>
        state.substituteScope(Map(s -> scope))
      ))
    case _ =>
      Left(Nil)
  }

  // Solve constraints by type. Returns `None` if constraints contain a consistency or `Some(state)` with the resulting state.
  def solve(state: State)(implicit language: Language): List[State] = {
    for (constraint <- state.constraints) {
      val result = rewrite(constraint, state.removeConstraint(constraint))

      result match {
        case Left(Nil) =>
          /* noop */
        case Left(states) =>
          return states.flatMap(solve)
        case Right(_) =>
          return Nil
      }
    }

    List(state)
  }
}
