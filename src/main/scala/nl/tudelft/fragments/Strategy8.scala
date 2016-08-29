package nl.tudelft.fragments

import com.typesafe.scalalogging.Logger
import nl.tudelft.fragments.spoofax._
import nl.tudelft.fragments.spoofax.models._
import org.slf4j.LoggerFactory

object Strategy8 {
  val logger = Logger(LoggerFactory.getLogger(this.getClass))
//  val language = Language.load("/Users/martijn/Projects/metaborg-pascal/org.metaborg.lang.pascal", "org.metaborg:org.metaborg.lang.pascal:0.1.0-SNAPSHOT", "Pascal")
//  val language = Language.load("/Users/martijn/Projects/scopes-frames/L3", "org.metaborg:L3:0.1.0-SNAPSHOT", "L3")
  val language = Language.load("/Users/martijn/Projects/MiniJava", "org.metaborg:MiniJava:0.1.0-SNAPSHOT", "MiniJava")

  // Make the various language specifications implicitly available
  implicit val productions = language.productions
  implicit val signatures = language.signatures
  implicit val specification = language.specification
  implicit val printer = language.printer
  implicit val rules = specification.rules

  def main(args: Array[String]): Unit = {
    val startRules = language.startRules

    logger.info("Start growing")

    // Randomly combine rules to build larger rules
    val base = repeat(grow, 200)(rules)

    logger.info("Start building")

    // Start from a start rule and build a complete program
    for (i <- 0 to 10000) {
      val result = build(startRules.random, base.shuffle, 200)

      result match {
        case Left(rule) =>
          val states = Solver.solve(rule.state)

          if (states.nonEmpty) {
            val state = states.random

            // Use solution to create a concrete pattern
            val concretePattern = Concretor.concretize(rule, state)

            // Turn solved state (State) into a stratego term (IStrategoTerm)
            val concreteTerm = Converter.toTerm(concretePattern)

            // Turn a stratego term (IStrategoTerm) into concrete syntax (String)
            val syntax = printer(concreteTerm)

            println(syntax.stringValue())
            println("===")
          }
        case _ =>

      }
    }
  }

  // Randomly merge rules in a rules into larger consistent rules
  def grow(rules: List[Rule])(implicit signatures: List[Signature]): List[Rule] = {
    val ruleA = rules.random
    val ruleB = rules.random

    if (ruleA.recurse.nonEmpty) {
      val recurse = ruleA.recurse.random

      // TODO: During 'grow' we should not check consistency of resolve constraints
      ruleA
        .merge(recurse, ruleB)
        .map(_ :: rules)
        .getOrElse(rules)
    } else {
      rules
    }
  }

  // Build a complete program by growing a partial program
  def build(partial: Rule, rules: List[Rule], fuel: Int)(implicit signatures: List[Signature]): Either[Rule, Int] = {
//    print(".")
//    println(partial)

    if (partial.recurse.isEmpty) {
      if (Solver.solve(partial.state).nonEmpty) {
        Left(partial)
      } else {
        Right(-1)
      }
    } else {
      val recurse = partial.recurse.random

      val maxSize = 40
      val remSize = maxSize - partial.pattern.size
      val divSize = remSize / partial.recurse.size

      // Testing something..
      if (divSize > 2) {
        val mergedRules = (for {rule <- rules.shuffle if rule.pattern.size <= divSize} yield {
          partial.merge(recurse, rule)
        }).flatten

        var remainingFuel = fuel

        for (mergedRule <- mergedRules) {
          remainingFuel = remainingFuel - 1

          val complete = build(mergedRule, rules, remainingFuel)

          if (complete.isLeft) {
            return complete
          } else {
            remainingFuel = complete.asInstanceOf[Right[_, Int]].b

            if (remainingFuel < 0) {
              return Right(remainingFuel)
            }
          }
        }

        Right(remainingFuel)
      } else {
        Right(fuel)
      }
    }
  }
}
