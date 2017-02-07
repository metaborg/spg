package org.metaborg.spg.core

import org.metaborg.spg.core.lexical.LexicalGenerator
import org.metaborg.spg.core.resolution.{Graph, Occurrence}
import org.metaborg.spg.core.solver._
import org.metaborg.spg.core.spoofax.Language
import org.metaborg.spg.core.spoofax.models.Strategy
import org.metaborg.spg.core.spoofax.models.Strategy._
import org.metaborg.spg.core.terms.{Pattern, TermAppl, TermString, Var}

case class Concretor(language: Language) {
  val generator = new LexicalGenerator(language.productions)

  def computeNamingConstraints(state: Program)(implicit language: Language): List[NamingConstraint] = {
    val graph = Graph(state.constraints)

    //graph.namingConstraints(state.resolution)
    Nil
  }

  // Replace TermVars in pattern by concrete names satisfying the solution
  def concretize(program: Program)(implicit language: Language): Pattern = {
    // Compute equality constraints
    val namingConstraints = computeNamingConstraints(program)

    // Compute inequality constraints
//    val inequalityConsraints = program.inequalities.map { case (p1, p2) =>
//      Diseq(p1, p2)
//    }

    // Combine the constraints
    val constraints = namingConstraints //++ inequalityConsraints

    // Equality constraints
    val equalityConstraints = filterEqs(constraints)

    // Disequality constraints
    val disequalityConstraints = filterDiseqs(constraints)

    // Replace names in Eq constraints
    val r1 = program.pattern.substitute(
      nameEq(equalityConstraints, Map.empty, nameProvider).get.map {
        case (n1, n2) =>
          Var(n1) -> TermString(n2)
      }
    )

    // Replace names in Diseq constraints
    val r2 = r1.substitute(
      nameDiseq(filterDiseqs(disequalityConstraints), nameProvider)
    )

    // Replace TermAppl("NameVar", List(_)) by a random identifier
    val nameVarToIdentifier = new Strategy {
      override def apply(p: Pattern): Option[Pattern] = p match {
        case TermAppl("NameVar", _) =>
          Some(TermString("n" + nameProvider.next))
        case _ =>
          None
      }
    }

    val r3 = topdown(attempt(nameVarToIdentifier))(r2).get

    // Replace variables by a random value that satisfies their sort
    val r4 = r3.substitute(
      r3.vars.map(v => {
        val sortOpt = language.signatures.sortForPattern(r3, v)
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

    topdown(attempt(conssToCons))(r4).get
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
        case Eq(o1@Occurrence(_, Var(a), _), o2@Occurrence(_, Var(b), _)) if !binding.contains(a) && !binding.contains(b) =>
          val name = "n" + nameProvider.next

          return nameEq(eqs - eq, binding + (a -> name) + (b -> name), nameProvider)
        // x == y, s(x) == s(y)
        case Eq(s1@Occurrence(_, Var(a), _), s2@Occurrence(_, Var(b), _)) if binding.contains(a) && binding.contains(b) && binding(a) == binding(b) =>
          return nameEq(eqs - eq, binding, nameProvider)
        // x == y, s(x) exists, s(y) undefined
        case Eq(s1@Occurrence(_, Var(n1), _), s2@Occurrence(_, Var(n2), _)) if binding.contains(n1) && !binding.contains(n2) =>
          return nameEq(eqs - eq, binding + (n2 -> binding(n1)), nameProvider)
        // x == y, s(x) undefined, s(y) exists
        case Eq(s1@Occurrence(_, Var(n1), _), s2@Occurrence(_, Var(n2), _)) if !binding.contains(n1) && binding.contains(n2) =>
          return nameEq(eqs - eq, binding + (n1 -> binding(n2)), nameProvider)
        // x == "a", s(x) undefined
        case Eq(s1@Occurrence(_, Var(n1), _), c2@Occurrence(_, TermString(n2), _)) if !binding.contains(n1) =>
          return nameEq(eqs - eq, binding + (n1 -> n2), nameProvider)
        // x == "a", s(x) == "a"
        case Eq(s1@Occurrence(_, Var(n1), _), c2@Occurrence(_, TermString(n2), _)) if binding.contains(n1) && binding(n1) == n2 =>
          return nameEq(eqs - eq, binding, nameProvider)
        // "a" == "a"
        case Eq(c1@Occurrence(_, TermString(n1), _), c2@Occurrence(_, TermString(n2), _)) if n1 == n2 =>
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
      .map(s => (s.name.asInstanceOf[Var], TermString("n" + nameProvider.next)))
      .toMap

  // Get the symbolic names in the disequality conditions
  def diseqsToNames(diseqs: List[Diseq]): List[Occurrence] =
    diseqs.flatMap {
      case Diseq(n1@Occurrence(_, _, _), n2@Occurrence(_, _, _)) =>
        List(n1, n2)
      case Diseq(n1@Occurrence(_, _, _), _) =>
        List(n1)
      case Diseq(_, n2@Occurrence(_, _, _)) =>
        List(n2)
    }.distinct

  // Get the equality conditions
  def filterEqs(substitution: List[NamingConstraint]) =
    substitution.collect { case x: Eq => x }

  // Get the disequality conditions
  def filterDiseqs(substitution: List[NamingConstraint]) =
    substitution.collect { case x: Diseq => x }
}
