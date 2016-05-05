package nl.tudelft.fragments

object Main {
  def main(args: Array[String]): Unit = {
    val ruleInt = Rule(
      TermAppl("Int"),
      TypeVar("t3"),
      List(
        TypeEquals(TypeVar("t3"), TypeAppl("Int"))
      )
    )

    val ruleAdd = Rule(
      TermAppl("Add", List(
        TermVar("e1", TypeVar("t1")),
        TermVar("e2", TypeVar("t2"))
      )),
      TypeVar("t"),
      List(
        TypeEquals(TypeVar("t"), TypeAppl("Int")),
        TypeEquals(TypeVar("t1"), TypeAppl("Int")),
        TypeEquals(TypeVar("t2"), TypeAppl("Int"))
      )
    )

    // ---

    val rules = List(ruleAdd, ruleInt)

    val types = List(
      TypeAppl("Int"),
      TypeAppl("Bool")
    )

    val cs = List(
      TypeOf("x", TypeVar("t1")),
      TypeEquals(TypeVar("t1"), TypeAppl("Bool")),
      TypeEquals(TypeVar("t1"), TypeAppl("Bool")),
      Subtype(TypeVar("t2"), TypeVar("t1"))
    )

    // Have the generator perform a single step
    //val r1 = Generator.generate(rules, new Rule(TermVar("e", TypeVar("t")), TypeVar("t"), List()), types)
    //println(r1)

    // Make the generator repeat at most 10 times
    for (i <- 0 to 100) {
      val r1 = Generator.repeat(rules, new Rule(TermVar("e", TypeVar("t")), TypeVar("t"), List()), types, 10)
      println(r1)
    }

    //val r = Solver.solve(cs.head, cs.tail, types)
    //println(r)
  }
}
