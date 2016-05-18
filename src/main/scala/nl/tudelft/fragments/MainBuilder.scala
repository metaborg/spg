package nl.tudelft.fragments

import nl.tudelft.fragments.examples.MiniJava
import nl.tudelft.fragments.memory.Node
import nl.tudelft.fragments.spoofax.{Converter, Printer}

import scala.collection.immutable.IndexedSeq

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

    // Generation phase
    val kb = repeat(generate, 1000)(rules)

    // Start variable
    println("Start")

    for (i <- 1 to 1000) {
      println(i)
      val result = Builder.build(kb, kb.random, 30, up, down)
      println(result)

      if (result.isDefined) {
        val substitution = Solver.solve(result.get.constraints)

        if (substitution.isDefined) {
          println("WERKT!")

          val concretePattern = Concretor.concretize(result.get, substitution.get)
          val strategoTerm = Converter.toTerm(concretePattern)
          val text = printer(strategoTerm).stringValue()

          println(text)
          println("-----")
        }
      }
    }
  }

  // Generate rules
  def generate(rules: List[Rule]): List[Rule] = {
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
