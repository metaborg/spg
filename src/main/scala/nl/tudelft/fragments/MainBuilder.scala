package nl.tudelft.fragments

import javax.inject.Singleton

import nl.tudelft.fragments.spoofax.{Converter, Printer, Specification, Signatures}
import org.metaborg.core.project.{IProjectService, SimpleProjectService}
import org.metaborg.spoofax.core.{Spoofax, SpoofaxModule}

import scala.util.Random
import scala.util.control.Breaks._

object MainBuilder {
  val spoofax = new Spoofax(new SpoofaxModule() {
    override def bindProject() {
      bind(classOf[SimpleProjectService]).in(classOf[Singleton])
      bind(classOf[IProjectService]).to(classOf[SimpleProjectService])
    }
  })

  def main(args: Array[String]): Unit = {
    implicit val signatures = Signatures.read(
      strategoPath = "zip:/Users/martijn/Projects/spoofax-releng/stratego/org.metaborg.meta.lang.stratego/target/org.metaborg.meta.lang.stratego-2.0.0-SNAPSHOT.spoofax-language!/",
      signaturePath = "/Users/martijn/Projects/scopes-frames/L1/src-gen/signatures/L1-sig.str"
    )

    val rules = Specification.read(
      nablPath = "zip:/Users/martijn/Projects/nabl/org.metaborg.meta.nabl2.lang/target/org.metaborg.meta.nabl2.lang-2.0.0-SNAPSHOT.spoofax-language!/",
      specPath = "/Users/martijn/Projects/scopes-frames/L1/trans/analysis/l1.nabl2"
    )

    val printer = Printer.printer(
      languagePath = "/Users/martijn/Projects/scopes-frames/L1/"
    )

    println(rules)

//    val rules = MiniJava.rules
//    val types = MiniJava.types
//    val printer = Printer.print("/Users/martijn/Documents/workspace/MiniJava")

    // Generation phase (TODO: the generated rules should not be "beyond repair", i.e. there must be reachable scopes to which we can add a declaration)
    //val kb = repeat(generateNaive, 10)(rules)
    //val (kb, _) = repeat(Function.tupled(generateIntelligent _), 10)((rules, Map.empty))
    val kb1 = repeat(generateOutwards, 2)(rules)

    // Start variable
    println("Start")

    // Resolve some refs
    val kb2 = repeat(Builder.buildToResolve, 100)(kb1)

    println(kb1.length)
    println(kb2.length)

    // Strategy 1: divide term size and backtrack
    for (i <- 0 to 1000) {
      val complete = generateComplete(kb2, kb2.random, 20)
      println(complete)

      if (complete.isDefined) {
        val concretePattern = Concretor.concretize(complete.get, complete.get.state.nameConstraints)
        val strategoTerm = Converter.toTerm(concretePattern)
        val text = printer(strategoTerm).stringValue()
        println(text)
        println("=====")
      }
    }
  }

  // Generate a complete rule within a size limit
  def generateComplete(rules: List[Rule], rule: Rule, size: Int): Option[Rule] = {
    // Check whether we can still close the fragment within the size limit using pre-computed bounds.
    if (/*up(rule.sort) + rule.pattern.vars.map(hole => down.getOrElse(hole.sort, 0)).sum +*/ rule.pattern.size + rule.pattern.vars.length > size) {
      return None
    }

    // Only consider choices that add a "balanced" amount
    /*
    for (hole <- rule.pattern.vars) {
      if (down(hole.sort) > (size - rule.pattern.size) / rule.pattern.vars.length) {
        return None
      }
    }
    */

    // Debugging
    println(rule)

    var result = rule

    // First, eagerly solve random references by merging
    if (rule.state.constraints.exists(_.isInstanceOf[Res])) {
      val choices = Builder.buildToResolve(rules, rule)

      breakable {
        for (choice <- Random.shuffle(choices).slice(0, 100)) {
          val nested = generateComplete(rules, choice._8, size)

          if (nested.isDefined) {
            result = nested.get
            break
          }
        }

        // Cannot solve resolution within size limit; exit.
        return None
      }
    }

    // Then, work directly towards the root
    /*
    if (result.sort != SortAppl("Program")) {
      val choices = Builder.buildToRoot(rules, result)

      for (choice <- Random.shuffle(choices)) {
        if (choice.sort == root(result.sort)) {
          val last = generateComplete(rules, choice, size)

          if (last.isDefined) {
            return last
          }
        }
      }
    }
    */

    // Then, eagerly close random holes
    if (result.state.pattern.vars.nonEmpty) {
      // For each hole
      for (hole <- result.state.pattern.vars) {
        val choices = Builder.buildToClose(rules, result, hole)

        // Shuffle the choices
        val limitedChoices = Random.shuffle(choices)

        for (choice <- limitedChoices) {
          // Only consider choices that add a "balanced" amount
          //if (choice.pattern.size - result.pattern.size < (size - result.pattern.size) / result.pattern.vars.length) {
            // Only consider closed fragments
            //if (choice.pattern.vars.length < rule.pattern.vars.length) {
              // Only consider fragments without resolution constraints
              //if (!choice.state.constraints.exists(_.isInstanceOf[Res])) {
                val last = generateComplete(rules, choice, size)

                if (last.isDefined) {
                  return last
                }
              //}
            //}
          //}
        }
      }

      None
    } else {
      Some(result)
    }
  }

  // Generate rules by combining each rule with every other rule
  def generateOutwards(rules: List[Rule]): List[Rule] = {
    val compute = for (rule <- rules; hole <- rule.pattern.vars; other <- rules) yield
      (rule, hole, other)

    val generated = compute.flatMap { case (rule, hole, other) =>
      rule.merge(hole, other)
    }

    generated ++ rules
  }

  // Generate rules by combining a random rule with another rule that matches the hole. OBSERVATION: misses crucial parts due to randomness..
  def generateIntelligent(rules: List[Rule], cache: Map[(Rule, TermVar, Rule), Option[Rule]]): (List[Rule], Map[(Rule, TermVar, Rule), Option[Rule]]) = {
    println("Generated now " + rules.length)

    // Compute what needs to be computed
    val compute = for (r1 <- rules; hole <- r1.pattern.vars; r2 <- rules; if !cache.contains((r1, hole, r2)))
      yield (r1, hole, r2)

    for ((r1, hole, r2) <- Random.shuffle(compute)) {
      if (r2.sort.unify(hole.sort).isDefined) {
        val merged = r1.merge(hole, r2)

        if (merged.isDefined) {
          return (merged.get :: rules, cache.updated((r1, hole, r2), merged))
        }
      }
    }

    (rules, cache)
  }

  // Generate rules naively
  def generateNaive(rules: List[Rule]): List[Rule] = {
    val r1 = rules.random
    val r2 = rules.random

    if (r1.pattern.vars.nonEmpty) {
      val hole = r1.pattern.vars.random
      val merged = r1.merge(hole, r2)

      if (merged.isDefined) {
        return merged.get :: rules
      }
    }

    rules
  }

  // Returns a function x => f(f(f(x))) with n times f
  def repeat[T](f: T => T, n: Int): T => T = n match {
    case 0 => (e: T) => e
    case _ => (e: T) => repeat(f, n - 1)(f(e))
  }
}
