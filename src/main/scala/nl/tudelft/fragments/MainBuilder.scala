package nl.tudelft.fragments

import nl.tudelft.fragments.examples.MiniJava
import nl.tudelft.fragments.spoofax.{Converter, Printer}

import scala.util.Random
import scala.util.control.Breaks._

object MainBuilder {
  // Minimal size needed to root from given sort
  val up = Map[Sort, Int](
    SortAppl("Program") -> 0,
    SortAppl("MainClass") -> 2,
    SortAppl("List", List(SortAppl("ClassDecl"))) -> 6,
    SortAppl("ClassDecl") -> 8,
    SortAppl("ParentDecl") -> 12,
    SortAppl("List", List(SortAppl("FieldDecl"))) -> 12,
    SortAppl("FieldDecl") -> 14,
    SortAppl("List", List(SortAppl("MethodDecl"))) -> 12,
    SortAppl("MethodDecl") -> 14,
    SortAppl("List", List(SortAppl("VarDecl"))) -> 20,
    SortAppl("VarDecl") -> 22,
    SortAppl("List", List(SortAppl("ParamDecl"))) -> 20,
    SortAppl("ParamDecl") -> 22,
    SortAppl("Type") -> 16,
    SortAppl("List", List(SortAppl("Statement"))) -> 20,
    SortAppl("Statement") -> 22,
    SortAppl("List", List(SortAppl("Exp", List()))) -> 20,
    SortAppl("Exp") -> 20
  )

  // Minimal size needed to bottom from given sort
  val down = Map[Sort, Int](
    SortAppl("Program") -> 7,
    SortAppl("MainClass") -> 5,
    SortAppl("List", List(SortAppl("ClassDecl"))) -> 1,
    SortAppl("ClassDecl") -> 5,
    SortAppl("ParentDecl") -> 1,
    SortAppl("List", List(SortAppl("FieldDecl"))) -> 1,
    SortAppl("FieldDecl") -> 3,
    SortAppl("List", List(SortAppl("MethodDecl"))) -> 1,
    SortAppl("MethodDecl") -> 7,
    SortAppl("List", List(SortAppl("VarDecl"))) -> 1,
    SortAppl("VarDecl") -> 3,
    SortAppl("List", List(SortAppl("ParamDecl"))) -> 1,
    SortAppl("ParamDecl") -> 3,
    SortAppl("Type") -> 1,
    SortAppl("List", List(SortAppl("Statement"))) -> 1,
    SortAppl("Statement") -> 2,
    SortAppl("List", List(SortAppl("Exp", List()))) -> 1,
    SortAppl("Exp") -> 1
  )

  // Sort needed to go to root quickest
  val root = Map[Sort, Sort](
    SortAppl("MainClass") -> SortAppl("Program"),
    SortAppl("List", List(SortAppl("ClassDecl"))) -> SortAppl("Program"),
    SortAppl("ClassDecl") -> SortAppl("List", List(SortAppl("ClassDecl"))),
    SortAppl("List", List(SortAppl("MethodDecl"))) -> SortAppl("ClassDecl"),
    SortAppl("MethodDecl") -> SortAppl("List", List(SortAppl("MethodDecl"))),
    SortAppl("Statement") -> SortAppl("MethodDecl"),
    SortAppl("List", List(SortAppl("Exp", List()))) -> SortAppl("Exp"),
    SortAppl("Exp") -> SortAppl("Statement")
  )

  def main(args: Array[String]): Unit = {
    val rules = MiniJava.rules
    val types = MiniJava.types
    val printer = Printer.print("/Users/martijn/Documents/workspace/MiniJava")

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
      val complete = generateComplete(kb2, kb2.random, 60)
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
    if (up(rule.sort) + rule.pattern.vars.map(hole => down.getOrElse(hole.sort, 0)).sum + rule.pattern.size + rule.pattern.vars.length > size) {
      return None
    }

    // Only consider choices that add a "balanced" amount
    for (hole <- rule.pattern.vars) {
      if (down(hole.sort) > (size - rule.pattern.size) / rule.pattern.vars.length) {
        return None
      }
    }

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

    // Then, eagerly close random holes
    if (result.state.pattern.vars.nonEmpty) {
      val choices = Builder.buildToClose(rules, result)

      // Shuffle the choices and limit to 100
      val limitedChoices = Random.shuffle(choices)

      for (choice <- limitedChoices) {
        // Only consider choices that add a "balanced" amount
        if (choice.pattern.size - result.pattern.size < (size - result.pattern.size) / result.pattern.vars.length) {
          // Only consider closed fragments
          if (choice.pattern.vars.length < rule.pattern.vars.length) {
            // Only consider fragments without resolution constraints
            if (!choice.state.constraints.exists(_.isInstanceOf[Res])) {
              val last = generateComplete(rules, choice, size)

              if (last.isDefined) {
                return last
              }
            }
          }
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

    val filtered = compute
      .filter { case (rule, hole, other) => other.sort.unify(hole.sort).isDefined }

    val generated = filtered.flatMap { case (rule, hole, other) =>
      val merged = rule
        .merge(hole, other)
        .substituteSort(hole.sort.unify(other.sort).get)

      if (Consistency.check(merged.constraints)) {
        Some(merged)
      } else {
        None
      }
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
        val merged = r1
          .merge(hole, r2)
          .substituteSort(hole.sort.unify(r2.sort).get)

        if (Consistency.check(merged.constraints)) {
          return (merged :: rules, cache.updated((r1, hole, r2), Some(merged)))
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
      val unifier = hole.sort.unify(r2.sort)

      if (unifier.isDefined) {
        val merged = r1.merge(hole, r2)
          .substituteSort(unifier.get)

        if (Consistency.check(merged.constraints)) {
          return merged :: rules
        }
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
