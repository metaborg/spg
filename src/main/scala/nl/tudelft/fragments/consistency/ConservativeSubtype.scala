package nl.tudelft.fragments.consistency

import nl.tudelft.fragments.spoofax.Language
import nl.tudelft.fragments.{CSubtype, FSubtype, Graph, State}

/**
  * 2. Only allow A <.? B if this follows from the subtyping relation. This encodes the observation that the subtyping
  * relation is not complete until the program is complete and hence we cannot decide if two types will be subtypes of
  * each other until the very last moment. However, most types will not occur in a subtyping relation.
  *
  * We take a conservative approach where a fragment is deemed inconsistent if two types are required to be subtypes and
  * this does not follow from the current subtyping relation.
  */
object ConservativeSubtype {
  def isConsistent(state: State)(implicit language: Language): Boolean = {
    // Build the subtype relation
    val subtypeRelation = state.constraints.foldLeft(state.subtypeRelation) {
      case (subtypeRelation, FSubtype(t1, t2)) if (t1.vars ++ t2.vars).isEmpty && !subtypeRelation.domain.contains(t1) && !subtypeRelation.isSubtype(t2, t1) =>
        val closure = for (ty1 <- subtypeRelation.subtypeOf(t1); ty2 <- subtypeRelation.supertypeOf(t2))
          yield (ty1, ty2)

        subtypeRelation ++ closure
      case (subtypeRelation, _) =>
        subtypeRelation
    }

    // Verify the CSubtype constraints according to the subtype relation
    val validSubtyping = state.constraints.forall {
      case CSubtype(t1, t2) if (t1.vars ++ t2.vars).isEmpty =>
        subtypeRelation.isSubtype(t1, t2)
      case _ =>
        true
    }

    validSubtyping
  }
}
