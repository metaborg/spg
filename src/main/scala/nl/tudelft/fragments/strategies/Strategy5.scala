package nl.tudelft.fragments.strategies

import nl.tudelft.fragments.spoofax.Language
import nl.tudelft.fragments.spoofax.models.{Signatures, SortAppl}
import nl.tudelft.fragments.{Consistency, Rule, Solver, _}

object Strategy5 {
  implicit val language = Language.load("/Users/martijn/Projects/scopes-frames/L3", "org.metaborg:L3:0.1.0-SNAPSHOT", "L3")

  // Make the various language specifications implicitly available
  implicit val productions = language.productions
  implicit val signatures = language.signatures
  implicit val specification = language.specification
  implicit val printer = language.printer
  implicit val rules = specification.rules

  def main(args: Array[String]): Unit = {
    val kb = repeat(gen, 1000)(rules)

    for (i <- 1 to 100) {
      val rule = kb.filter(_.sort == SortAppl("Start")).random

      val term = complete(kb, rule)

      println(term)

      if (term.isDefined) {
        val solutions = Solver
          .solve(term.get.state)
          .map(state => term.get.copy(state = state))

        if (solutions.nonEmpty) {
          println("Solved all constraints in the following " + solutions.length + " ways:")

          for (solution <- solutions) {
            println(solution)
          }
        } else {
          println("Unable to solve the constraints")
        }
      }

      /*
      if (term.isDefined) {
        val concrete = Concretor.concretize(term.get, term.get.state.facts)

        val aterm = Converter.toTerm(concrete)

        println(print(aterm))
      }
      */
    }
  }

  def gen(rules: List[Rule])(implicit signatures: Signatures): List[Rule] = {
    // Pick a random rule
    val rule = rules.random

    // Pick a random recurse constraint
    val recurseOpt = rule.recurse.randomOption

    // Lazily merge a random other rule $r \in rules$ into $rule$, solving $recurse$
    val mergedOpt = recurseOpt.flatMap(recurse =>
      rules.shuffle.view
        .flatMap(rule.merge(recurse, _, 2))
        .find(_.pattern.size < 10)
    )

    // Solve any constraints, create new rules out of all possible solvings
    val newRules = mergedOpt.map(merged =>
      Solver
        .solveAny(merged.state)
        .map(state => merged.copy(state = state))
    )

    // Check the new rules for consistency
    val validNewRules = newRules.map(rules =>
      rules.filter(rule => Consistency.check(rule))
    )

    val result = validNewRules
      .map(_ ++ rules)
      .getOrElse(rules)

    result
  }

  // Complete the given rule by solving resolution & recurse constraints
  def complete(rules: List[Rule], rule: Rule, limit: Int = 10)(implicit signatures: Signatures): Option[Rule] =
    if (limit == 0) {
      None
    } else {
      println(rule)

      rule.recurse match {
        case Nil =>
          Some(rule)
        case _ =>
          for (recurse <- rule.recurse) {
            val choices = rules
              .flatMap(rule.merge(recurse, _, 2))
              .filter(choice => choice.pattern.size + choice.recurse.size <= 20)

            if (choices.isEmpty) {
              return None
            } else {
              for (choice <- choices) {
                val deeper = complete(rules, choice, limit - 1)

                if (deeper.isDefined) {
                  return deeper
                }
              }
            }
          }

          None
      }
    }
}
