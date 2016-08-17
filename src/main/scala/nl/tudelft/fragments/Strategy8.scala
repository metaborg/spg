package nl.tudelft.fragments

import javax.inject.Singleton

import nl.tudelft.fragments.spoofax.{Printer, Signatures, Specification, Converter}
import org.metaborg.core.project.{IProjectService, SimpleProjectService}
import org.metaborg.spoofax.core.{Spoofax, SpoofaxModule}

import scala.annotation.tailrec

object Strategy8 {
  val spoofax = new Spoofax(new SpoofaxModule() {
    override def bindProject() {
      bind(classOf[SimpleProjectService]).in(classOf[Singleton])
      bind(classOf[IProjectService]).to(classOf[SimpleProjectService])
    }
  })

  implicit val signatures = Signatures.read(
    strategoPath = "zip:/Users/martijn/Projects/spoofax-releng/stratego/org.metaborg.meta.lang.stratego/target/org.metaborg.meta.lang.stratego-2.0.0-SNAPSHOT.spoofax-language!/",
    signaturePath = "/Users/martijn/Projects/scopes-frames/L3/src-gen/signatures/L3-sig.str"
  )

  implicit val specification = Specification.read(
    nablPath = "zip:/Users/martijn/Projects/nabl/org.metaborg.meta.nabl2.lang/target/org.metaborg.meta.nabl2.lang-2.1.0-SNAPSHOT.spoofax-language!/",
    specPath = "/Users/martijn/Projects/scopes-frames/L3/trans/analysis/l3.nabl2"
  )

  implicit val rules: List[Rule] = specification.rules

  def main(args: Array[String]): Unit = {
    val print = Printer.printer(
      languagePath = "/Users/martijn/Projects/scopes-frames/L3/"
    )

    val startRules = rules.filter(_.sort == SortAppl("Start"))

    // Randomly combine rules to build larger rules
    val base = repeat(grow, 200)(rules)

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
            val syntax = print(concreteTerm)

            println(syntax.stringValue())
            println("===")
          }
        case _ =>

      }
    }
  }

  // Randomly merge rules in a rules into larger consistent rules
  def grow(rules: List[Rule])(implicit signatures: List[Signatures.Decl]): List[Rule] = {
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
  def build(partial: Rule, rules: List[Rule], fuel: Int)(implicit signatures: List[Signatures.Decl]): Either[Rule, Int] = {
    print(".")
//    println(partial)

    if (partial.recurse.isEmpty) {
      //println("Complete program: " + partial)

      if (Solver.solve(partial.state).nonEmpty) {
        //println("Solved!")
      } else {
        //println("Unable to solve..")
      }

      Left(partial)
    } else {
      val recurse = partial.recurse.random

      val maxSize = 20
      val remSize = maxSize - partial.pattern.size
      val divSize = remSize / partial.recurse.size

      // Testing something..
      if (divSize > 2) {
        val mergedRules = for {rule <- rules.shuffle if rule.pattern.size <= divSize} yield {
          partial.merge(recurse, rule)
        }

        var remainingFuel = fuel

        for (mergedRule <- mergedRules.flatten) {
          remainingFuel = remainingFuel-1

          val complete = build(mergedRule, rules, remainingFuel)

          if (complete.isLeft) {
            return complete
          } else {
            remainingFuel = complete.asInstanceOf[Right[_, Int]].b

            if (remainingFuel < 0) {
              println("Out of fuel")

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
