package nl.tudelft.fragments

object Consistency {
  // TODO: Scope reachability checking!

  // Check for consistency
  def check(C: List[Constraint]): Boolean = {
    val result = checkTypeEquals(C).map { case (typeBinding, nameBinding) =>
      checkTypeOf(C, typeBinding, nameBinding)
    }

    result.isDefined && result.get
  }

  // Check if the TypeEquals unify
  def checkTypeEquals(C: List[Constraint]): Option[(TypeBinding, NameBinding)] = {
    val typeEquals = C.flatMap {
      case c: TypeEquals =>
        Some(c)
      case _ =>
        None
    }

    typeEquals.foldLeftWhile((Map.empty[TypeVar, Type], Map.empty[NameVar, Name])) {
      case ((typeBinding, nameBinding), TypeEquals(t1, t2)) =>
        t1.unify(t2, typeBinding, nameBinding)
    }
  }

  // Check if the TypeOf for same name unify
  def checkTypeOf(C: List[Constraint], typeBinding: TypeBinding = Map.empty, nameBinding: NameBinding = Map.empty): Boolean = {
    val typeOf = C.flatMap {
      case c: TypeOf =>
        Some(c)
      case _ =>
        None
    }

    val uniqueTypeOf = typeOf.distinct

    uniqueTypeOf.groupBy(_.n).values.forall(typeOfs =>
      typeOfs.map(_.t).pairs.foldLeftWhile((typeBinding, nameBinding)) {
        case ((typeBinding, nameBinding), (t1, t2)) =>
          t1.unify(t2, typeBinding, nameBinding)
      }.isDefined
    )
  }

  // Check if the naming conditions are consistent
  def checkNamingConditions(C: List[Constraint]): Boolean = {
    // Remove duplicates (the algorithm fails on duplicate disequality constraints)
    val unique = C.distinct

    val eqs: List[Eq] = unique
      .filter(_.isInstanceOf[Eq])
      .map(_.asInstanceOf[Eq])

    val disEqs = unique
      .filter(_.isInstanceOf[Diseq])
      .map(_.asInstanceOf[Diseq])

    val (_, updatedDisEqs) = solveNaming(eqs, disEqs)

    !updatedDisEqs.exists {
      case Diseq(n1, n2) => n1 == n2
    }
  }

  // Eliminate naming equalities by substituting
  def solveNaming(eqs: List[Eq], disEqs: List[Diseq]): (List[Eq], List[Diseq]) = eqs match {
    case Eq(n1, n2) :: xs =>
      solveNaming(xs.map(_.substitute(n1, n2)), disEqs.map(_.substitute(n1, n2)))
    case Nil =>
      (Nil, disEqs)
  }
}
