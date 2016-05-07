package nl.tudelft.fragments

import scala.util.Random

object Generator {
  def generate(rules: List[Rule], current: Rule, maxSize: Int, ts: List[Type]): Option[Rule] = {
    val holes = current.pattern.vars

    if (holes.isEmpty) {
      Some(current)
    } else if (maxSize > 0) {
      // Pick a random hole
      val hole = holes.random

      // Compute applicable rules
      val applicable = rules
        // Filter and instantiate rules with polymorphic sorts
        .flatMap(rule =>
          rule.sort.unify(hole.sort).map(rule.substituteSort)
        )
        // Filter rules that are not too large
        .filter(_.pattern.vars.length < maxSize/holes.length)

      // Shuffle the rules
      val random = Random.shuffle(applicable)

      for (rule <- random) {
        // Merge rule into current at hole
        val merged = current.merge(hole, rule)

        // Check if the result is consistent
        if (Solver.solve(merged.constraints, ts).isDefined) {
          val complete = generate(rules, merged, maxSize-(merged.pattern.size-current.pattern.size), ts)

          if (complete.isDefined) {
            return complete
          }
        }
      }

      None
    } else {
      None
    }
  }
}
