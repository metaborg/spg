package nl.tudelft.fragments.example

import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

import nl.tudelft.fragments.{CGenRecurse, CResolve, Config, Constraint, Generator, Rule}

import scala.util.Random

object Tiger {
  val tigerConfig = new Config {
    override def scoreFn(rule: Rule): Int = {
      def scoreConstraint(c: Constraint): Int = c match {
        case _: CResolve =>
          3
        case _: CGenRecurse =>
          if (rule.pattern.size < 20) {
            0
          } else {
            10
          }
        case _ =>
          0
      }

      1 + rule.constraints.map(scoreConstraint).sum
    }

    override def choose(rule: Rule, rules: List[Rule]): List[(Rule, Int)] = {
      // Score every next move
      val scoredRules = rules.zipWith(scoreFn)

      if (scoredRules.isEmpty) {
        return Nil
      }

      // The smallest score gets probability x. All other scores are expressed in x.
      val smallest = scoredRules.map(_._2).sorted.head

      // Compute reciprocal of weight
      val weights = scoredRules.map {
        case (rule, score) =>
          (rule, smallest.toFloat/score)
      }

      // Compute probability (solve for x)
      val p = 1.toFloat/weights.map(_._2).sum

      // Multiply weight by probability of smallest to get probability. Multiply by 100 for int scale.
      weights.map {
        case (rule, weight) =>
          (rule, (weight * p * 100).toInt)
      }
    }

    override def sizeLimit: Int =
      60

    override def resolveProbability: Int =
      20
  }

  def main(args: Array[String]): Unit = {
    new Generator().generate("/Users/martijn/Projects/metaborg-tiger/org.metaborg.lang.tiger", tigerConfig, false).subscribe(program => {
      println("===================================")
      println(DateTimeFormatter.ISO_DATE_TIME.format(ZonedDateTime.now()))
      println("-----------------------------------")
      println(program)
    })
  }
}
