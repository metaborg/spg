package nl.tudelft.fragments.strategies

import com.typesafe.scalalogging.Logger
import nl.tudelft.fragments.spoofax._
import nl.tudelft.fragments.spoofax.models._
import nl.tudelft.fragments.{CResolve, Concretor, Consistency, Graph, Rule, Solver}
import org.slf4j.LoggerFactory

object Strategy8 {
  val logger = Logger(LoggerFactory.getLogger(this.getClass))
//  implicit val language = Language.load("/Users/martijn/Projects/metaborg-pascal/org.metaborg.lang.pascal", "org.metaborg:org.metaborg.lang.pascal:0.1.0-SNAPSHOT", "Pascal")
  implicit val language = Language.load("/Users/martijn/Projects/scopes-frames/L3", "org.metaborg:L3:0.1.0-SNAPSHOT", "L3")
//  implicit val language = Language.load("/Users/martijn/Projects/MiniJava", "org.metaborg:MiniJava:0.1.0-SNAPSHOT", "MiniJava")

  // Make the various language specifications implicitly available
  implicit val productions = language.productions
  implicit val signatures = language.signatures
  implicit val specification = language.specification
  implicit val printer = language.printer
  implicit val rules = specification.rules

  // Generation properties
  val maxSize = 30
  val growSteps = 2000
  val fuel = 200

  def main(args: Array[String]): Unit = {
    val startRules = language.startRules

    //logger.info("Start growing")
    //val base = repeat(grow, growSteps)(rules)
    //logger.info("Grown {} new rules; start building", base.length - rules.length)

    // Index rules by sort
    val rulesBySort = indexRules(rules)

    // Start from a start rule and build a complete program
    for (i <- 0 to 10000) {
      val result = build(startRules.random, rulesBySort, fuel)

      result match {
        case Left(rule) =>
          val states = Solver.solve(rule.state)

          if (states.nonEmpty) {
            val state = states.random

            // Use solution to create a concrete pattern
            val concretePattern = Concretor(language).concretize(rule, state)

            // Turn solved state (State) into a stratego term (IStrategoTerm)
            val concreteTerm = Converter.toTerm(concretePattern)

            // Turn a stratego term (IStrategoTerm) into concrete syntax (String)
            val syntax = printer(concreteTerm)

            println(syntax.stringValue())
            println("===")
          }
        case _ =>
          println(s"Attempt $i failed")
      }
    }
  }

  // Randomly merge rules in a rules into larger consistent rules
  def grow(rules: List[Rule])(implicit signatures: Signatures): List[Rule] = {
    val ruleA = rules.random
    val ruleB = rules.random

    if (ruleA.recurse.nonEmpty) {
      val recurse = ruleA.recurse.random

      ruleA
        .merge(recurse, ruleB, 1)
        .map(_ :: rules)
        .getOrElse(rules)
    } else {
      rules
    }
  }

  // Build a complete program by growing a partial program
  def build(partial: Rule, rules: Map[String, List[Rule]], fuel: Int)(implicit signatures: Signatures): Either[Rule, Int] = {
//    print(".")
//    println(partial)

    if (partial.recurse.isEmpty) {
      if (Solver.solve(partial.state).nonEmpty) {
        Left(partial)
      } else {
        println("Unsolvable")
        println(partial)
        Right(-1)
      }
    } else {
      // Randomly resolve the reference in a CResolve constraint to a random (but non-inconsistent) declaration
      lazy val resolveConstraints = partial.resolve

      val resolved = partial/*if (Random.nextInt(350) == 0 && resolveConstraints.nonEmpty) {
        val resolveConstraint = resolveConstraints.random
        val resolved = resolve(resolveConstraint, partial)

        if (resolved.nonEmpty) {
          resolved.get
        } else {
          partial
        }
      } else {
        partial
      }*/

      // Continue solving recurse constraints
      val recurse = resolved.recurse.random

      val remSize = maxSize - resolved.pattern.size
      val divSize = remSize / resolved.recurse.size

      val applicable = if (rules.contains(firstLevel(recurse.sort))) {
        rules(firstLevel(recurse.sort))
      } else {
        rules.values.flatten
      }

      // Randomize the applicable rules, because otherwise we get the same rule for some sort every time (e.g. Block(Block(Block(...))) for Statement)
      val randomApplicable = applicable.toList.shuffle

      // Make applicable lazy. We don't need to merge all upfront
      val lazyApplicable = randomApplicable//.view

      // Compute options (lazily)
      val mergedRules = for (rule <- lazyApplicable if rule.pattern.size <= divSize) yield {
        resolved.merge(recurse, rule, 2)
      }

      // Collect only valid rules
      val validMergedRules = mergedRules.flatten

      var remainingFuel = fuel

      for (mergedRule <- validMergedRules) {
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

      if (remainingFuel < 0) {
        println("Out of fuel")
        println(partial)
      }

      Right(remainingFuel)
    }
  }

  // Solve given CResolve constraint in given rule
  def resolve(resolve: CResolve, rule: Rule): Option[Rule] = {
    val declarations = Graph(rule.state.facts).res(rule.state.resolution)(resolve.n1)

    for (declaration <- declarations.shuffle) {
      val resolvedRule = Solver.resolve(rule, resolve, declaration)

      if (Consistency.check(resolvedRule)) {
        return Some(resolvedRule)
      }
    }

    None
  }

  // Map every sort to its applicable rules
  def indexRules(rules: List[Rule]): Map[String, List[Rule]] = {
    rules.groupBy(rule => firstLevel(rule.sort))
  }

  // Flatten a sort to its first level
  def firstLevel(sort: Sort): String = sort match {
    case SortAppl(name, _) => name
  }
}
