package nl.tudelft.fragments

object Concretor {
  // Assign concrete names to symbolic names satisfying the equality and disequality conditions
  def concretize(rule: Rule, solution: Substitution): Pattern = {
    // Apply the given solution to the pattern
    val solved = rule.pattern
      .substituteType(solution._1)
      .substituteName(solution._2)

    // Use a new name provider to keep the numbers low
    val nameProvider = NameProvider(0)

    // Convert symbolic names based on equality and disequality conditions
    val partially = solved
      .substituteConcrete(nameEq(filterEqs(solution), nameProvider))
      .substituteConcrete(nameDiseq(filterDiseqs(solution), nameProvider))

    // Convert remaining symbolic names. This is a lazy solution; we can use names sparingly/randomly.
    partially
      .substituteConcrete(
        partially.names.map((_, ConcreteName("n" + nameProvider.next))).toMap
      )
  }

  // Generate a binding from symbolcic names to concrete names satisfying the equality constraints
  def nameEq(eqs: List[Eq], nameProvider: NameProvider): ConcreteBinding = eqs match {
    case Eq(SymbolicName(n1), ConcreteName(n2)) :: _ =>
      val binding = Map(SymbolicName(n1) -> ConcreteName(n2))

      nameEq(eqs.substituteConcrete(binding), nameProvider) ++ binding
    case Eq(ConcreteName(n2), SymbolicName(n1)) :: _ =>
      val binding = Map(SymbolicName(n1) -> ConcreteName(n2))

      nameEq(eqs.substituteConcrete(binding), nameProvider) ++ binding
    case Eq(SymbolicName(n), _) :: _ =>
      val next = "n" + nameProvider.next
      val binding = Map(SymbolicName(n) -> ConcreteName(next))

      nameEq(eqs.substituteConcrete(binding), nameProvider) ++ binding
    case Eq(_, SymbolicName(n)) :: _ =>
      val next = "n" + nameProvider.next
      val binding = Map(SymbolicName(n) -> ConcreteName(next))

      nameEq(eqs.substituteConcrete(binding), nameProvider) ++ binding
    case _ :: tail =>
      nameEq(tail, nameProvider)
    case Nil =>
      Map.empty
  }

  // Give each name in the DisEq a different name. This is a lazy solution; we can use names sparingly/randomly.
  def nameDiseq(diseqs: List[Diseq], nameProvider: NameProvider): ConcreteBinding =
    diseqsToNames(diseqs)
      .map((_, ConcreteName("n" + nameProvider.next)))
      .toMap

  // Get the equality conditions
  def filterEqs(substitution: Substitution) =
    substitution._3.flatMap {
      case x: Eq =>
        Some(x)
      case _ =>
        None
    }

  // Get the disequality conditions
  def filterDiseqs(substitution: Substitution) =
    substitution._3.flatMap {
      case x: Diseq =>
        Some(x)
      case _ =>
        None
    }

  // Get the symbolic names in the disequality conditions
  def diseqsToNames(diseqs: List[Diseq]): List[SymbolicName] =
    diseqs.flatMap { case Diseq(n1@SymbolicName(_), n2@SymbolicName(_)) =>
      List(n1, n2)
    }.distinct
}
