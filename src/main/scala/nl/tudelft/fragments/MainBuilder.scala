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

    // Generation phase (TODO: the generated rules should not be "beyond repair", i.e. there must be reachable scopes to which we can add a declaration)
    //val kb = repeat(generateNaive, 10)(rules)
    //val (kb, _) = repeat(Function.tupled(generateIntelligent _), 10)((rules, Map.empty))
    val kb1 = repeat(generateOutwards, 2)(rules)

    // Start variable
    println("Start")

    val rr = Rule(TermAppl("If", List(TermAppl("And", List(TermAppl("Lt", List(TermAppl("Call", List(TermAppl("NewObject", List(PatternNameAdapter(SymbolicName("Class", "n372933")))), PatternNameAdapter(SymbolicName("Method", "n372934")), TermAppl("Cons", List(TermAppl("NewObject", List(PatternNameAdapter(SymbolicName("Class", "n372935")))), TermVar("x372938", SortAppl("List", List(SortAppl("Exp", List()))), TypeVar("t372936"), List(ScopeVar("s372937"))))))), TermAppl("Call", List(TermVar("x67512", SortAppl("Exp", List()), TypeVar("t67511"), List(ScopeVar("s372937"))), PatternNameAdapter(SymbolicName("Method", "n67513")), TermVar("x67515", SortAppl("List", List(SortAppl("Exp", List()))), TypeVar("t67514"), List(ScopeVar("s372937"))))))), TermVar("x433", SortAppl("Exp", List()), TypeVar("t432"), List(ScopeVar("s372937"))))), TermVar("x2", SortAppl("Statement", List()), TypeVar("t2"), List(ScopeVar("s1"))), TermVar("x3", SortAppl("Statement", List()), TypeVar("t3"), List(ScopeVar("s2"))))), SortAppl("Statement", List()), TypeVar("t"), List(ScopeVar("s372937")), List(Ref(SymbolicName("Class", "n372935"),ScopeVar("s372937")), Res(SymbolicName("Class", "n372935"),NameVar("d372940")), TypeEquals(TypeVar("t372941"),TypeAppl("ClassType", List(TypeNameAdapter(NameVar("d372940"))))), TypeEquals(TypeVar("t372942"),TypeAppl("Cons", List(TypeVar("t372941"), TypeVar("t372936")))), Ref(SymbolicName("Class", "n372933"),ScopeVar("s372937")), Res(SymbolicName("Class", "n372933"),NameVar("d372943")), TypeEquals(TypeVar("t372944"),TypeAppl("ClassType", List(TypeNameAdapter(NameVar("d372943"))))), DirectImport(ScopeVar("s372945"),ScopeVar("s372946")), Ref(SymbolicName("Method", "n372934"),ScopeVar("s372945")), AssocConstraint(NameVar("d372947"),ScopeVar("s372946")), Res(SymbolicName("Method", "n372934"),NameVar("d372948")), TypeOf(NameVar("d372948"),TypeAppl("Pair", List(TypeVar("t372942"), TypeVar("t372939")))), TypeEquals(TypeVar("t372944"),TypeAppl("ClassType", List(TypeNameAdapter(NameVar("d372947"))))), DirectImport(ScopeVar("s67517"),ScopeVar("s67518")), Ref(SymbolicName("Method", "n67513"),ScopeVar("s67517")), AssocConstraint(NameVar("d67519"),ScopeVar("s67518")), Res(SymbolicName("Method", "n67513"),NameVar("d67520")), TypeOf(NameVar("d67520"),TypeAppl("Pair", List(TypeVar("t67514"), TypeVar("t67521")))), TypeEquals(TypeVar("t67511"),TypeAppl("ClassType", List(TypeNameAdapter(NameVar("d67519"))))), TypeEquals(TypeVar("t67516"),TypeAppl("Bool", List())), TypeEquals(TypeVar("t372939"),TypeAppl("Int", List())), TypeEquals(TypeVar("t67521"),TypeAppl("Int", List())), TypeEquals(TypeVar("t434"),TypeAppl("Bool", List())), TypeEquals(TypeVar("t67516"),TypeAppl("Bool", List())), TypeEquals(TypeVar("t432"),TypeAppl("Bool", List())), TypeEquals(TypeVar("t434"),TypeAppl("Bool", List())), Par(ScopeVar("s1"),ScopeVar("s372937")), Par(ScopeVar("s2"),ScopeVar("s372937"))))
    println(Builder.buildToResolve(kb1, rr))

    // Resolve some refs
    val kb2 = repeat(Builder.buildToResolve, 100)(kb1)

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
