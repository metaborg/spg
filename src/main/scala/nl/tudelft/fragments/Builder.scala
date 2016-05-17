package nl.tudelft.fragments

object Builder {
  // Complete the given rule
  def build(rules: List[Rule], current: Rule, size: Int, up: Map[Sort, Int], down: Map[Sort, Int]): Option[Rule] = {
    val holes = current.pattern.vars

    // If we have a complete program, return
    if (holes.isEmpty && current.pattern.asInstanceOf[TermAppl].cons == "Program") {
      return Some(current)
    }

    // TODO: consult the inverse signature. If we have a class and we are at size = 17, then we will never get a program,
    // TODO:   since this requires Program(_, Cons(x, _)) which is size 21 and hence exceeds limit 20. So no need to go over the 5 possibilities
    // TODO: another example: if we have a method at size = 11, then to get a program we need Program(_, Cons(Class(_, _, _, Cons(x, _)), _))
    // TODO:   which has size = 21 and hence exceeds limit 20. So no need to go over the 8 possibilities here.
    // TODO: and we can do the same thing for going down: given a

    // TODO: Handle root cleanly. Is it logical to force generating the root first?
    if (current.pattern.asInstanceOf[TermAppl].cons != "Program") {
      // Consistency check (TODO: handle lists uniformly)
      if (up.contains(current.sort)) {
        if (up(current.sort) + current.pattern.size > size) {
          return None
        }
      }

      // Merge this rule into another rule
      val applicable = rules
        .filter(rule => rule.pattern.vars.exists(_.sort.unify(current.sort).isDefined))
        .filter(rule => rule.pattern.size + current.pattern.size + rule.pattern.vars.length + current.pattern.vars.length <= size)

      for (randomRule <- applicable.randomSubset(20)) {
        val randomRuleHoles = randomRule.pattern.vars.filter(hole => hole.sort.unify(current.sort).isDefined)
        val randomHole = randomRuleHoles.random

        val merged = randomRule
          .merge(randomHole, current)
          .substituteSort(randomHole.sort.unify(current.sort).get)

        // Check consistency
        if (Consistency.check(merged.constraints)) {
          val result = build(rules, merged, size, up, down)

          // Return if defined
          if (result.isDefined) {
            return result
          }
        }
      }
    } else {
      // Consistency check
      val minimal = holes.map(hole => down.getOrElse(hole.sort, 0)).sum
      if (minimal + current.pattern.size > size) {
        return None
      }

      val hole: TermVar = holes.random
      
      // Filter rules that are a) syntactically valid and b) balance the size
      val applicable = rules
        .filter(rule => rule.sort.unify(hole.sort).isDefined)
        .filter(rule => rule.pattern.size+rule.pattern.vars.length <= (2*size-current.pattern.size-current.pattern.vars.length)/current.pattern.vars.length)

      for (randomRule <- applicable.randomSubset(20)) {
        val merged = current
          .merge(hole, randomRule)
          .substituteSort(randomRule.sort.unify(hole.sort).get)

        // Check consistency
        if (Consistency.check(merged.constraints)) {
          val result = build(rules, merged, size, up, down)

          // Return if defined
          if (result.isDefined) {
            return result
          }
        }
      }
    }

    None
  }

  // 1. Pick two random rules
  // 2. Combine the rules to produce new rule
  // 3. Divide term size among holes (with root being an implicit hole if it is not Program)
  // 4. Only check for consistency, until the program is finished
  // 5. When it's finished, solve. If it is solvable, output.
}
