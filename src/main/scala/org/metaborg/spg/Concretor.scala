package org.metaborg.spg

import org.metaborg.spg.lexical.LexicalGenerator
import org.metaborg.spg.resolution.Graph
import org.metaborg.spg.solver._
import org.metaborg.spg.spoofax.Language
import org.metaborg.spg.spoofax.models.Strategy

case class Concretor(language: Language) {
  val generator = new LexicalGenerator(language.productions)

  def computeNamingConstraints(state: State): List[NamingConstraint] = {
    val graph = Graph(state.constraints)

    graph.namingConstraints(state.resolution)
  }

  // Replace TermVars in pattern by concrete names satisfying the solution
  def concretize(state: State): Pattern = {
    // Use a new name provider to keep the numbers low
    val nameProvider = NameProvider(0)

    // Compute equality constraints
    val namingConstraints = computeNamingConstraints(state)

    // Compute inequality constraints
    val inequalityConsraints = state.inequalities.map { case (p1, p2) =>
      Diseq(p1, p2)
    }

    // Combine the constraints
    val constraints = namingConstraints ++ inequalityConsraints

    // Equality constraints
    val equalityConstraints = filterEqs(constraints)

    // Disequality constraints
    val disequalityConstraints = filterDiseqs(constraints)

    // Replace names in Eq constraints
    val r1 = state.pattern.substitute(
      nameEq(equalityConstraints, Map.empty, nameProvider).get.map {
        case (n1, n2) =>
          Var(n1) -> TermString(n2)
      }
    )

    // Replace names in Diseq constraints
    val r2 = r1.substitute(
      nameDiseq(filterDiseqs(disequalityConstraints), nameProvider)
    )

    // Convert remaining TermVars based on their sort
    val result2 = r2.substitute(
      r2.vars.map(v => {
        val sortOpt = language.signatures.sortForPattern(r2, v)
        val value = sortOpt.map(generator.generate)

        v -> TermString(value.getOrElse(throw new RuntimeException("Could not determine Sort for TermVar")))
      }).toMap
    )

    // Replace "Conss" by "Cons" for pretty printing
    val conssToCons = new Strategy {
      override def apply(p: Pattern): Option[Pattern] = p match {
        case TermAppl("Conss", children) =>
          Some(TermAppl("Cons", children))
        case _ =>
          None
      }
    }

    Strategy.topdown(Strategy.`try`(conssToCons))(result2).get
  }

  /**
    * Solve naming constraints eqs. Every constraint must be solvable under
    * every binding. Failure to solve one constraint means the system is
    * inconsistent.
    *
    * @param eqs
    * @param binding
    * @param nameProvider
    * @return
    */
  def nameEq(eqs: List[Eq], binding: Map[String, String], nameProvider: NameProvider): Option[Map[String, String]] = {
    if (eqs.isEmpty) {
      return Some(binding)
    }

    for (eq <- eqs) {
      eq match {
        // x == y, s(x) undefined, s(y) undefined
        case Eq(s1@SymbolicName(_, n1), s2@SymbolicName(_, n2)) if !binding.contains(n1) && !binding.contains(n2) =>
          val name = "n" + nameProvider.next

          return nameEq(eqs - eq, binding + (n1 -> name) + (n2 -> name), nameProvider)
        // x == y, s(x) == s(y)
        case Eq(s1@SymbolicName(_, n1), s2@SymbolicName(_, n2)) if binding.contains(n1) && binding.contains(n2) && binding(n1) == binding(n2) =>
          return nameEq(eqs - eq, binding, nameProvider)
        // x == y, s(x) exists, s(y) undefined
        case Eq(s1@SymbolicName(_, n1), s2@SymbolicName(_, n2)) if binding.contains(n1) && !binding.contains(n2) =>
          return nameEq(eqs - eq, binding + (n2 -> binding(n1)), nameProvider)
        // x == y, s(x) undefined, s(y) exists
        case Eq(s1@SymbolicName(_, n1), s2@SymbolicName(_, n2)) if !binding.contains(n1) && binding.contains(n2) =>
          return nameEq(eqs - eq, binding + (n1 -> binding(n2)), nameProvider)
        // x == "a", s(x) undefined
        case Eq(s1@SymbolicName(_, n1), c2@ConcreteName(_, n2, _)) if !binding.contains(n1) =>
          return nameEq(eqs - eq, binding + (n1 -> n2), nameProvider)
        // x == "a", s(x) == "a"
        case Eq(s1@SymbolicName(_, n1), c2@ConcreteName(_, n2, _)) if binding.contains(n1) && binding(n1) == n2 =>
          return nameEq(eqs - eq, binding, nameProvider)
        // "a" == "a"
        case Eq(c1@ConcreteName(_, n1, _), c2@ConcreteName(_, n2, _)) if n1 == n2 =>
          return nameEq(eqs - eq, binding + (n1 -> n1), nameProvider)
        case _ =>
          None
      }
    }

    None
  }

  // Give each name in the DisEq a different name. This is a liberal solution; we can use names sparingly/randomly.
  // TODO: DisEq constraints may contain ConcreteNames which we can then no longer use!
  def nameDiseq(diseqs: List[Diseq], nameProvider: NameProvider): TermBinding =
    diseqsToNames(diseqs)
      .map(s => (Var(s.name), TermString("n" + nameProvider.next)))
      .toMap

  // Get the symbolic names in the disequality conditions
  def diseqsToNames(diseqs: List[Diseq]): List[SymbolicName] =
    diseqs.flatMap {
      case Diseq(n1@SymbolicName(_, _), n2@SymbolicName(_, _)) =>
        List(n1, n2)
      case Diseq(n1@SymbolicName(_, _), _) =>
        List(n1)
      case Diseq(_, n2@SymbolicName(_, _)) =>
        List(n2)
    }.distinct

  // Get the equality conditions
  def filterEqs(substitution: List[Constraint]) =
    substitution.collect { case x: Eq => x }

  // Get the disequality conditions
  def filterDiseqs(substitution: List[Constraint]) =
    substitution.collect { case x: Diseq => x }
}
