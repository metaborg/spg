package nl.tudelft.fragments

import nl.tudelft.fragments.examples.MiniJava
import nl.tudelft.fragments.memory.Node
import nl.tudelft.fragments.spoofax.{Converter, Printer}

import scala.collection.immutable.IndexedSeq
import scala.util.Random

object MainBuilder {
  def main(args: Array[String]): Unit = {
    val rules = MiniJava.rules
    val types = MiniJava.types
    val printer = Printer.print("/Users/martijn/Documents/workspace/MiniJava")

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
      SortAppl("Exp") -> 1
    )

    // Generation phase (TODO: the generated rules should not be "beyond repair", i.e. there must be reachable scopes to which we can add a declaration)
    //val kb = repeat(generateNaive, 10)(rules)
    //val (kb, _) = repeat(Function.tupled(generateIntelligent _), 10)((rules, Map.empty))
    val kb1 = repeat(generateOutwards, 2)(rules)

    // Start variable
    println("Start")

    // Resolve some refs
    val kb2 = repeat(Builder.buildToResolve, 1)(kb1)

    println(kb1.length)
    println(kb2.length)

    // Pick a fragment without references, close a hole
    var r = kb2.random
    println(r)

    for (i <- 0 to 10) {
      println(i)

      val resolve = Builder.buildToResolve(kb2, r)
      if (resolve.isDefined) {
        r = resolve.get
        println(r)
      }

      val resolve2 = Builder.buildToClose(kb2, r)
      if (resolve2.isDefined) {
        r = resolve2.get
        println(r)
      }
    }

//    for (i <- 1 to 10000) {
////      println(i)
//      val result = Builder.build(kb, kb.random, 40, up, down)
////      println(result)
//
//      if (result.isDefined) {
//        val substitution = Solver.solve(result.get.constraints)
//
//        if (substitution.isDefined) {
////          println("WERKT!")
//
//          val concretePattern = Concretor.concretize(result.get, substitution.get)
//          val strategoTerm = Converter.toTerm(concretePattern)
//          val text = printer(strategoTerm).stringValue()
//
//          println(text)
//          println("-----")
//        }
//      }
//    }
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

    /*
    // Old, inefficient code:

    val rulesWithHole = rules.flatMap(rule =>
      rule.pattern.vars.map(hole =>
        (rule, hole)
      )
    )

    val rulesWithHoleWithOther: List[(Rule, TermVar, Rule, Rule)] = rulesWithHole.flatMap { case (rule, hole) =>
      rules.flatMap(otherRule =>
        if (otherRule.sort.unify(hole.sort).isDefined) {
          val merged = rule
            .merge(hole, otherRule)
            .substituteSort(hole.sort.unify(otherRule.sort).get)

          if (Consistency.check(merged.constraints)) {
            Some(rule, hole, otherRule, merged)
          } else {
            None
          }
        } else {
          None
        }
      )
    }

    if (rulesWithHoleWithOther.nonEmpty) {
      val (rule, hole, other, merged) = rulesWithHoleWithOther.random

      merged :: rules
    } else {
      rules
    }
    */
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
    case _ => (e: T) => repeat(f, n-1)(f(e))
  }
}
