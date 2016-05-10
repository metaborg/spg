package nl.tudelft.fragments

import nl.tudelft.fragments.examples.{Lambda, MiniJava}
import nl.tudelft.fragments.spoofax.{Converter, Printer}

object Main {
  def main(args: Array[String]): Unit = {
//    val rules = Lambda.rules
//    val types = Lambda.types

    val rules = MiniJava.rules
    val types = MiniJava.types
    val printer = Printer.print("/Users/martijn/Documents/workspace/MiniJava")

    // Make the generator repeat at most 10 times
    for (i <- 1 to 1000) {
      val r1 = Generator.generate(rules, new Rule(
        TermVar("x", SortAppl("Program"), TypeVar("t"), ScopeVar("s")),
        SortAppl("Program"),
        TypeVar("t"),
        ScopeVar("s"),
        Nil
      ), 25, types)

      if (r1.isDefined) {
        val soln = Solver.solve(r1.get.constraints)

        if (soln.isDefined) {
//          println(r1)

//          println(soln)

          val concrete = Concretor.concretize(r1.get, soln.get)
//          println(concrete)

          val sterm = Converter.toTerm(concrete)
//          println(sterm)

          val s = printer(sterm)
          println(s.stringValue())

          println("-----")
        }
      }
    }
  }
}
