package nl.tudelft.fragments

/**
  * Language-dependent generation configuration.
  */
abstract class Config {
  /**
    * Given a rule, assign it a score.
    *
    * @param r
    * @return
    */
  def scoreFn(r: Rule): Int

  /**
    * Limit at which to abandon a term.
    *
    * @return
    */
  def sizeLimit: Int

  /**
    * Probability with which the generator solves a single resolve constraint.
    *
    * @return
    */
  def resolveProbability: Double
}

/**
  * A config with some sane defaults
  */
object DefaultConfig extends Config {
  override def scoreFn(rule: Rule): Int = {
    def scoreConstraint(c: Constraint): Int = c match {
      case _: CResolve =>  3
      case _: CGenRecurse =>  6
      case _: CTrue => 0
      case _ =>  1
    }

    rule.constraints.map(scoreConstraint).sum
  }

  override def sizeLimit: Int =
    70

  override def resolveProbability: Double =
    0.1
}
