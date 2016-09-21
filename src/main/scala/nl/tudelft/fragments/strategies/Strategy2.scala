package nl.tudelft.fragments.strategies

import nl.tudelft.fragments.spoofax.{Language, Specification}
import nl.tudelft.fragments.spoofax.models.Signatures
import nl.tudelft.fragments.{Builder, CResolve, Graph, Rule, _}

import scala.util.Random

object Strategy2 {
  def main(args: Array[String]): Unit = {
    implicit val language = Language.load("/Users/martijn/Projects/scopes-frames/L3", "org.metaborg:L3:0.1.0-SNAPSHOT", "L3")

    implicit val signatures = language.signatures
    implicit val printer = language.printer
    implicit val specification = language.specification
    implicit val rules = specification.rules

    val kb = repeat(gen, 4000)(rules)

    val closedRules = kb.filter(_.recurse.isEmpty)
    println(closedRules)

    for (rule <- closedRules) {
      val resolvedRule = rule.resolve.foldLeft(List(rule)) { case (rules, res@CResolve(n1, n2)) =>
        rules.flatMap(Builder.resolve(_, res, null))
      }

      resolvedRule.map(println)
    }
  }

  def gen(rules: List[Rule])(implicit signatures: List[CGDecl]): List[Rule] = {
    // Pick a random rule
    val rule = rules.random

    // Pick a random recurse constraint
    val recurseOpt = rule.recurse.randomOption

    // Lazily merge a random other rule $r \in rules$ into $rule$, solving $recurse$
    val mergedOpt = recurseOpt.flatMap(recurse =>
      Random.shuffle(rules).view
        .flatMap(rule.merge(recurse, _))
        .find(_.pattern.size < 20)
    )

    // Attempt to resolve a resolution constraint in the merged fragment
    mergedOpt.map(merged => {
      // Get resolution constraints
      val ress = merged
        .resolve
        .shuffle
        .view

      // Pair each resolution constraint with the possible declarations
      val declarations = ress
        .flatMap(res =>
          Graph(merged.state.facts).resolves(Nil, res.n1, merged.state.nameConstraints).map(dec =>
            (res, dec)
          )
        )
        .view

      // Resolve the resolution constraint
      val resolved = declarations
        .flatMap { case (res, dec) =>
          Builder.resolve(merged, res, dec._3).map(resolved => (res, dec, resolved))
        }

      // Resolve & check consistency
      resolved.headOption.map(_._3).map(_ :: rules)
        .getOrElse(merged :: rules)
    }).getOrElse(rules)
  }
}
