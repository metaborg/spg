package nl.tudelft.fragments.example

import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

import nl.tudelft.fragments.{CGenRecurse, CResolve, Config, Constraint, Generator, Rule, TermAppl}

object MiniJava {
  val miniJavaConfig = new Config {
    override def scoreFn(rule: Rule): Int = {
      def mainClassAssign(): Boolean = {
        val mainClass = rule.pattern.collect {
          case t@TermAppl("MainClass", _) =>
            List(t)
          case _ =>
            Nil
        }

        val mainClassArrayAssign = () => mainClass.flatMap(_.collect {
          case t@TermAppl("ArrayAssign", _) =>
            List(t)
          case _ =>
            Nil
        })

        val mainClassAssign = () => mainClass.flatMap(_.collect {
          case t@TermAppl("Assign", _) =>
            List(t)
          case _ =>
            Nil
        })

        mainClassArrayAssign().nonEmpty || mainClassAssign().nonEmpty
      }

      def scoreConstraint(c: Constraint): Int = c match {
        case _: CResolve =>
          3
        case _: CGenRecurse =>
          if (rule.pattern.size < 20) {
            0
          } else {
            // TODO: We are more interested in the _change_ in the number of recurse constraints, then their total amount. If there are 2 choices, and one adds twice as many recurse constraints, it's a lot worse, but this is not realized if 2 is negligible on the number of recurses we already have..
            10
          }
        case _ =>
          0
      }

      // An ArrayAssign inside the MainClass is always inconsistent
      if (mainClassAssign()) {
        Integer.MAX_VALUE
      } else {
        1 + rule.constraints.map(scoreConstraint).sum
      }
    }

    override def choose(rule: Rule, rules: List[Rule]): List[(Rule, Int)] = {
      // Score every next move
      val scoredRules = rules.zipWith(scoreFn).filter(_._2 < Integer.MAX_VALUE)

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
      80
  }

  def main(args: Array[String]): Unit = {
    new Generator().generate("/Users/martijn/Projects/MiniJava", "trans/static-semantics.nabl2", miniJavaConfig, 100, false).subscribe(program => {
      println("===================================")
      println(DateTimeFormatter.ISO_DATE_TIME.format(ZonedDateTime.now()))
      println("-----------------------------------")
      println(program)
    })
  }
}
