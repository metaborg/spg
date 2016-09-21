package nl.tudelft.fragments.strategies

import nl.tudelft.fragments.Rule
import nl.tudelft.fragments.spoofax.Language
import nl.tudelft.fragments.spoofax.models.Signatures

object Strategy3 {
  implicit val language = Language.load("/Users/martijn/Projects/scopes-frames/L3", "org.metaborg:L3:0.1.0-SNAPSHOT", "L3")

  // Make the various language specifications implicitly available
  implicit val productions = language.productions
  implicit val signatures = language.signatures
  implicit val specification = language.specification
  implicit val printer = language.printer
  implicit val rules = specification.rules

  def main(args: Array[String]): Unit = {
    for (i <- 1 to 100) {
      println(gen(rules.random))
    }
  }

  def gen(rule: Rule)(implicit rules: List[Rule], signatures: Signatures): Option[Rule] = {
    if (rule.recurse.isEmpty && rule.resolve.isEmpty) {
      Some(rule)
    } else if (rule.recurse.nonEmpty) {
      val choices = rules
        .shuffle
        .view
        .flatMap(other => {
          rule
            .merge(rule.recurse.head, other, 2)
            .filter(_.state.pattern.size < 30)
        })

      for (choice <- choices) {
        val complete = choices.headOption.flatMap(gen)

        if (complete.isDefined) {
          return complete
        }
      }

      None
    } else {
      None
    }
  }
}
