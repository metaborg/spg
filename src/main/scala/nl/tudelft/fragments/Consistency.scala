package nl.tudelft.fragments

object Consistency {
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

    typeOf.groupBy(_.n).values.forall(typeOfs =>
      typeOfs.map(_.t).pairs.foldLeftWhile((typeBinding, nameBinding)) {
        case ((typeBinding, nameBinding), (t1, t2)) =>
          t1.unify(t2, typeBinding, nameBinding)
      }.isDefined
    )
  }
}
