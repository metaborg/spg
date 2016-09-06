package nl.tudelft.fragments

import nl.tudelft.fragments.spoofax.Language
import nl.tudelft.fragments.spoofax.models.{Signature, Signatures, SortAppl}

// Build programs top-down
object Strategy6 {
  implicit val language = Language.load("/Users/martijn/Projects/scopes-frames/L3", "org.metaborg:L3:0.1.0-SNAPSHOT", "L3")

  // Make the various language specifications implicitly available
  implicit val productions = language.productions
  implicit val signatures = language.signatures
  implicit val specification = language.specification
  implicit val printer = language.printer
  implicit val rules = specification.rules

  def main(args: Array[String]): Unit = {
    val startRules = rules.filter(_.sort == SortAppl("Start"))

    build(startRules.random)
  }

  def build(partial: Rule)(implicit rules: List[Rule], signatures: Signatures): List[Rule] = {
    if (partial.pattern.size > 15) {
      Nil
    } else {
      if (partial.recurse.isEmpty) {
        println("Complete program: " + partial)

        val states = Solver.solve(partial.state)

        if (states.isEmpty) {
          //println("Could not solve it..")

          Nil
        } else {
          println("Solved it!")

          List(partial)
        }
      } else {
        val recurse = partial.recurse.random

        rules
          .flatMap(partial.merge(recurse, _, 2))
          .map(rule => {
            if (rule.recurse.isEmpty) {
              if (Solver.solve(rule.state).isEmpty) {
                //println("Consistent but not solvable: " + rule)
              }
            }

            rule
          })
          .flatMap(build)
      }
    }
  }
}
