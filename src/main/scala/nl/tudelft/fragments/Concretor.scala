package nl.tudelft.fragments

import nl.tudelft.fragments.lexical.LexicalGenerator
import nl.tudelft.fragments.spoofax.Specification
import nl.tudelft.fragments.spoofax.models._

object Concretor {
  // Replace TermVars in pattern by concrete names satisfying the solution
  def concretize(rule: Rule, solution: State)(implicit signatures: Signatures, productions: List[Production]): Pattern = {
    // Use a new name provider to keep the numbers low
    val nameProvider = NameProvider(0)

    // - solution.nameConstraints contains constraints that the names must satisfy for `solution.resolution` to occur
    val partially = rule.pattern
      .substitute(
        nameEq(filterEqs(solution.nameConstraints), Map.empty, nameProvider)
          .map { case (n1, n2) => TermVar(n1) -> TermString(n2) }
      )

    //    // Convert symbolic names based on equality and disequality conditions
    //    val partially = rule.pattern
    //      .substituteConcrete(nameEq(filterEqs(solution.nameConstraints), nameProvider))
    //      .substituteConcrete(nameDiseq(filterDiseqs(solution.nameConstraints), nameProvider))
    //
    //    // Convert remaining symbolic names. This is a lazy solution; we can use names sparingly/randomly.
    //    partially
    //      .substituteConcrete(
    //        partially.names.map(s => (s, ConcreteName(s.namespace, "n" + nameProvider.next, 0))).toMap
    //      )

    // TODO: We just assume TermVars to be names, but is this correct?
    // TODO: We just assign increasing names, but we can re-use names (and other strategies)

    // Create lexical generator
    val generator = new LexicalGenerator(productions)

    // Convert remaining TermVars based on their sort
    val result = partially.substitute(
      partially.vars.map(v => {
        val sort = signatures.sortForPattern(partially, v)
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

    Strategy.topdown(Strategy.`try`(conssToCons))(result).get
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

  // Generate a binding from symbolic names to concrete names satisfying the equality constraints
  //  def nameEq(eqs: List[Eq], nameProvider: NameProvider): TermBinding = eqs match {
  //    case Eq(s@SymbolicName(_, _), c@ConcreteName(_, _, _)) :: _ =>
  //      val binding = Map(s -> c)
  //
  //      nameEq(eqs.substitute(binding), nameProvider) ++ binding
  //    case Eq(c@ConcreteName(_, _, _), s@SymbolicName(_, _)) :: _ =>
  //      val binding = Map(s -> c)
  //
  //      nameEq(eqs.substituteConcrete(binding), nameProvider) ++ binding
  //    case Eq(s@SymbolicName(ns, _), _) :: _ =>
  //      val next = "n" + nameProvider.next
  //      val binding = Map(s -> ConcreteName(ns, next, 0))
  //
  //      nameEq(eqs.substituteConcrete(binding), nameProvider) ++ binding
  //    case Eq(_, s@SymbolicName(ns, n)) :: _ =>
  //      val next = "n" + nameProvider.next
  //      val binding = Map(s -> ConcreteName(ns, next, 0))
  //
  //      nameEq(eqs.substituteConcrete(binding), nameProvider) ++ binding
  //    case _ :: tail =>
  //      nameEq(tail, nameProvider)
  //    case Nil =>
  //      Map.empty
  //  }

  // Give each name in the DisEq a different name. This is a lazy solution; we can use names sparingly/randomly.
  // TODO: DisEq constraints may contain ConcreteNames which we can then no longer use!
  //  def nameDiseq(diseqs: List[Diseq], nameProvider: NameProvider): ConcreteBinding =
  //    diseqsToNames(diseqs)
  //      .map(s => (s, ConcreteName(s.namespace, "n" + nameProvider.next, 0)))
  //      .toMap

  // Get the equality conditions
  def filterEqs(substitution: List[Constraint]) =
    substitution.collect { case x: Eq => x }

  // Get the disequality conditions
  def filterDiseqs(substitution: List[Constraint]) =
    substitution.collect { case x: Diseq => x }

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
}
