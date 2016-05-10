package nl.tudelft.fragments

object Concretor {
  // Concretize the rule by a) choosing a substitution (e.g. there may be many solutions = substitutions) and b) assigning concrete names and types
  def concretize(rule: Rule, solution: Substitution): Pattern = {
    // Apply solution (substitution) to the pattern
    val solved = rule.pattern
      .substituteType(solution._1)
      .substituteName(solution._2)

    // Get all names
    val names = solved.names

    val eqs = solution._3.flatMap {
      case x: Eq =>
        Some(x)
      case _ =>
        None
    }

    // 2. Assign different names to names in Diseq conditions
    // 3. Assign names to the remaining SymbolicNames by randomly either
    //   a) choosing an existing name or
    //   b) choosing a new name.

    solved
      .substituteConcrete(name(eqs, NameProvider(0)))
  }

  // Generate a binding from symbolcic names to concrete names satisfying the equality constraints
  def name(eqs: List[Eq], nameProvider: NameProvider): ConcreteBinding = eqs match {
    case Eq(SymbolicName(n1), ConcreteName(n2)) :: _ =>
      val binding = Map(SymbolicName(n1) -> ConcreteName(n2))

      name(eqs.substituteConcrete(binding), nameProvider) ++ binding
    case Eq(ConcreteName(n2), SymbolicName(n1)) :: _ =>
      val binding = Map(SymbolicName(n1) -> ConcreteName(n2))

      name(eqs.substituteConcrete(binding), nameProvider) ++ binding
    case Eq(SymbolicName(n), _) :: _ =>
      val next = "n" + nameProvider.next
      val binding = Map(SymbolicName(n) -> ConcreteName(next))

      name(eqs.substituteConcrete(binding), nameProvider) ++ binding
    case Eq(_, SymbolicName(n)) :: _ =>
      val next = "n" + nameProvider.next
      val binding = Map(SymbolicName(n) -> ConcreteName(next))

      name(eqs.substituteConcrete(binding), nameProvider) ++ binding
    case _ :: tail =>
      name(tail, nameProvider)
    case Nil =>
      Map.empty
  }
}
