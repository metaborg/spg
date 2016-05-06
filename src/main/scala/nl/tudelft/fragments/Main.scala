package nl.tudelft.fragments

import nl.tudelft.fragments.examples.{Lambda, MiniJava}

object Main {
  def main(args: Array[String]): Unit = {
//    val rules = Lambda.rules
//    val types = Lambda.types

    val rules = MiniJava.rules
    val types = MiniJava.types

    // Make the generator repeat at most 10 times
    for (i <- 1 to 1000) {
      val r1 = Generator.generate(rules, new Rule(TermVar("x", "Program", TypeVar("t"), ScopeVar("s")), "_", TypeVar("t"), ScopeVar("s"), List()), 10, types)
      println(r1)

      if (r1.isDefined) {
        val soln = Solver.solve(r1.get.constraints, types)

        if (soln.isDefined) {
          println(soln)
        }
      }
    }
  }
}
