package nl.tudelft.fragments

object Main {
  def main(args: Array[String]): Unit = {
    // TODO: Add sorts

    val ruleInt = Rule(
      TermAppl("Int"),
      TypeVar("t"),
      ScopeVar("s"),
      List(
        TypeEquals(TypeVar("t"), TypeAppl("Int"))
      )
    )

    val ruleAdd = Rule(
      TermAppl("Add", List(
        TermVar("e1", TypeVar("t1"), ScopeVar("s")),
        TermVar("e2", TypeVar("t2"), ScopeVar("s"))
      )),
      TypeVar("t"),
      ScopeVar("s"),
      List(
        TypeEquals(TypeVar("t"), TypeAppl("Int")),
        TypeEquals(TypeVar("t1"), TypeAppl("Int")),
        TypeEquals(TypeVar("t2"), TypeAppl("Int"))
      )
    )

    val ruleAbs = Rule(
      TermAppl("Abs", List(
        NameVar("n"),
        TermVar("e1", TypeVar("t1"), ScopeVar("s")),
        TermVar("e2", TypeVar("t1"), ScopeVar("s1"))
      )),
      TypeVar("t"),
      ScopeVar("s"),
      List(
        TypeEquals(TypeVar("t"), TypeAppl("Fun", List(TypeVar("t1"), TypeVar("t2")))),
        Par(ScopeVar("s1"), ScopeVar("s")),
        Dec(ScopeVar("s1"), NameVar("n")),
        TypeOf(NameVar("n"), TypeVar("t1"))
      )
    )

    val ruleVar = Rule(
      TermAppl("Var", List(
        NameVar("n")
      )),
      TypeVar("t"),
      ScopeVar("s"),
      List(
        Ref(NameVar("n"), ScopeVar("s")),
        Res(NameVar("n"), NameVar("d")),
        TypeOf(NameVar("n"), TypeVar("t"))
      )
    )

    // ---

    val rules = List(ruleAdd, ruleInt, ruleAbs, ruleVar)

    val types = List(
      TypeAppl("Int"),
      TypeAppl("Bool")
    )

    val cs = List(
      TypeOf(NameVar("x"), TypeVar("t1")),
      TypeEquals(TypeVar("t1"), TypeAppl("Bool")),
      TypeEquals(TypeVar("t1"), TypeAppl("Bool")),
      Subtype(TypeVar("t2"), TypeVar("t1"))
    )

    // Have the generator perform a single step
    val r1 = new Rule(TermAppl("Program", List(
      TermVar("x1", TypeVar("t"), ScopeVar("s")),
      TermVar("x2", TypeVar("t"), ScopeVar("s"))
    )), TypeVar("t"), ScopeVar("s"), List())

    val r2 = r1.merge(TermVar("x1", TypeVar("t"), ScopeVar("s")), r1)

    println(r2)

    // Make the generator repeat at most 10 times
    for (i <- 0 to 100) {
      val r1 = Generator.repeat(rules, new Rule(TermVar("e", TypeVar("t"), ScopeVar("s")), TypeVar("t"), ScopeVar("s"), List()), types, 10)
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
