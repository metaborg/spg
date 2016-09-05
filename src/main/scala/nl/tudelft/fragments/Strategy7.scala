package nl.tudelft.fragments

import nl.tudelft.fragments.spoofax.Language
import nl.tudelft.fragments.spoofax.models.{Signature, Signatures, SortAppl}

object Strategy7 {
  implicit val language = Language.load("/Users/martijn/Projects/scopes-frames/L3", "org.metaborg:L3:0.1.0-SNAPSHOT", "L3")

  // Make the various language specifications implicitly available
  implicit val productions = language.productions
  implicit val signatures = language.signatures
  implicit val specification = language.specification
  implicit val printer = language.printer
  implicit val rules = specification.rules

  def main(args: Array[String]): Unit = {
    val startRules = rules.filter(_.sort == SortAppl("Start"))

    //println(repeat((x: List[Rule]) => build(x.random), 10)(startRules))
    println(build(startRules.random))
  }

  def build(partial: Rule)(implicit rules: List[Rule], signatures: Signatures): Option[Rule] = {
    if (partial.recurse.isEmpty) {
      Some(partial)
    } else {
      for (recurse <- partial.recurse) {
        for (rule <- rules) {
          val merged = partial
            .merge(recurse, rule)
            .filter(_.pattern.size <= 20)

          if (merged.isDefined && merged.get.recurse.size <= 5) {
            val continue = build(merged.get)

            if (continue.isDefined) {
              return continue
            }
          }
        }
      }

      None
    }
  }
}
