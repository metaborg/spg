package nl.tudelft.fragments

import scala.util.Random

object Generator {
  // Single-step generation by trying all possible rules until one succeeds
  def generate(rules: List[Rule], current: Rule, ts: List[Type]): Option[Rule] = {
    // Pick a random hole
    val holes = current.pattern.vars

    if (holes.nonEmpty) {
      val hole = holes.random

      // Shuffle the rules
      val randomRules = Random.shuffle(rules)

      for (rule <- randomRules) {
        // Merge rule into current at hole
        val merged = current.merge(hole, rule)

        // Check if the result is consistent
        if (Solver.solve(merged.constraints, ts).isDefined) {
          return Some(merged)
        }
      }

      None
    } else {
      None
    }
  }

  // Repeat single-step derivation
  def repeat(rules: List[Rule], current: Rule, ts: List[Type], n: Int): Option[Rule] = {
    if (current.pattern.vars.isEmpty) {
      Some(current)
    } else {
      if (n > 0) {
        val rule = generate(rules, current, ts)

        if (rule.isDefined) {
          repeat(rules, rule.get, ts, n - 1)
        } else {
          None
        }
      } else {
        None
      }
    }
  }

  // TODO: `repeat` does not yet backtrack to choices made in `generate`. When repeat fails, the full generation fails.
  // TODO: add scopes and references
}
