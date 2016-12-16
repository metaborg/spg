package nl.tudelft.fragments.example

import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

import nl.tudelft.fragments.{CGenRecurse, CResolve, Config, Constraint, Generator, Rule}

object Tiger extends App {
  val tigerConfig = new Config {
    override def scoreFn(rule: Rule): Int = {
      def scoreConstraint(c: Constraint): Int = c match {
        case _: CResolve =>
          3
        case _: CGenRecurse =>
          if (rule.pattern.size < 10) {
            -2
          } else {
            2
          }
        case _ =>
          0
      }

      rule.constraints.map(scoreConstraint).sum
    }

    override def sizeLimit: Int =
      60

    override def resolveProbability: Double =
      0.1
  }

  new Generator().generate("/Users/martijn/Projects/metaborg-tiger/org.metaborg.lang.tiger", tigerConfig).subscribe(program => {
    println("===================================")
    println(DateTimeFormatter.ISO_DATE_TIME.format(ZonedDateTime.now()))
    println("-----------------------------------")
    println(program)
  })
}
