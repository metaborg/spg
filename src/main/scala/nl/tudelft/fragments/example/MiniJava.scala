package nl.tudelft.fragments.example

import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

import nl.tudelft.fragments.{CGenRecurse, CResolve, Config, Constraint, Generator, Rule, TermAppl}

object MiniJava extends App {
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
          6
        case _ =>
          0
      }

      // An ArrayAssign inside the MainClass is always inconsistent
      if (mainClassAssign()) {
        Integer.MAX_VALUE
      } else {
        rule.constraints.map(scoreConstraint).sum
      }
    }

    override def sizeLimit: Int =
      60

    override def resolveProbability: Double =
      0.1
  }

  new Generator().generate("/Users/martijn/Projects/MiniJava", miniJavaConfig).subscribe(program => {
    println("===================================")
    println(DateTimeFormatter.ISO_DATE_TIME.format(ZonedDateTime.now()))
    println("-----------------------------------")
    println(program)
  })
}
