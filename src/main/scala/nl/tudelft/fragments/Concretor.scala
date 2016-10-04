package nl.tudelft.fragments

import nl.tudelft.fragments.lexical.LexicalGenerator
import nl.tudelft.fragments.spoofax.Language
import nl.tudelft.fragments.spoofax.models._

case class Concretor(language: Language) {
  val generator = new LexicalGenerator(language.productions)

  /**
    * The rule's resolution may not be valid due to shadowing. This method computes Eq and Diseq
    * constraints for names that should be equal (or inequal) when made concrete.
    *
    * For every resolution x |-> y, we create an Eq(x, y), and for all other names y that are
    * reachable from x, we create a Diseq(x, y).
    */
  def computeNamingConstraints(state: State): List[NamingConstraint] = {
    state.resolution.bindings.foldLeft(List.empty[NamingConstraint]) {
      case (namingConstraints, (ref, dec)) =>
        val reachableDeclarations = Graph(state.facts)(language).res(state.resolution)(ref)

        Eq(ref, dec) :: namingConstraints ++ reachableDeclarations
          .filter(_ != dec)
          .map(newDec => Diseq(dec, newDec))
    }
  }

  // Replace TermVars in pattern by concrete names satisfying the solution
  def concretize(rule: Rule, state: State): Pattern = {
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
    val r1 = rule.pattern.substitute(
      nameEq(equalityConstraints, Map.empty, nameProvider).map {
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
        val sort = language.signatures.sortForPattern(r2, v)
        val value = sort.map(generator.generate)

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

  // Generate a binding from SymbolicNames to String satisfying the equality constraints
  def nameEq(eqs: List[Eq], binding: Map[String, String], nameProvider: NameProvider): Map[String, String] = eqs match {
    case Eq(s1@SymbolicName(_, n1), s2@SymbolicName(_, n2)) :: tail =>
      val name = binding.getOrElse(n1, binding.getOrElse(n2, "n" + nameProvider.next))

      nameEq(tail, binding + (n1 -> name) + (n2 -> name), nameProvider)
    case Eq(c1@ConcreteName(_, n1, _), c2@ConcreteName(_, n2, _)) :: tail if n1 == n2 =>
      nameEq(tail, binding + (n1 -> n1), nameProvider)
    case Nil =>
      binding
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
