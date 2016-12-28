package nl.tudelft.fragments

/**
  * Language-dependent generation configuration.
  */
abstract class Config {
  /**
    * Given a rule, assign it a score on the interval [1, infinity].
    *
    * @param r
    * @return
    */
  def scoreFn(r: Rule): Int

  /**
    * Given the current rule and possible next rules, return a subset of the
    * next rules.
    *
    * @param rule  Current rule
    * @param rules Possible next rules
    * @return
    */
  def choose(rule: Rule, rules: List[Rule]): List[(Rule, Int)]

  /**
    * Limit at which to abandon a term.
    *
    * @return
    */
  def sizeLimit: Int

  /**
    * The reciprocal of the probability by which the generator solves a resolve
    * constraint. A higher number means less probability.
    *
    * @return
    */
  def resolveProbability: Int
}

/**
  * A config with some sane defaults
  */
object DefaultConfig extends Config {
  override def scoreFn(rule: Rule): Int = {
    def scoreConstraint(c: Constraint): Int = c match {
      case _: CResolve => 3
      case _: CGenRecurse => 6
      case _: CTrue => 0
      case _ => 1
    }

    rule.constraints.map(scoreConstraint).sum
  }

  override def choose(rule: Rule, rules: List[Rule]): List[(Rule, Int)] =
    ???

  override def sizeLimit: Int =
    70

  override def resolveProbability: Int =
    1
}
