package nl.tudelft.fragments

import org.scalatest.FunSuite

class SolverSuite extends FunSuite {

  test("single declaration") {
    val constraints = List(
      Par(ScopeVar("s1"),ScopeVar("s2")),
      Dec(ScopeVar("s1"),SymbolicName("", "n3")),
      Ref(SymbolicName("", "n1"),ScopeVar("s1")),
      Res(SymbolicName("", "n1"),NameVar("n2")),
      TypeOf(SymbolicName("", "n1"),TypeVar("t1")),
      TypeOf(SymbolicName("", "n3"),TypeVar("t1")),
      TypeEquals(TypeVar("t2"),TypeAppl("Fun", List(TypeVar("t1"), TypeVar("t3"))))
    )

    assert(Solver.solve(constraints).nonEmpty)
  }

  test("impossible") {
    val constraints = List(
      Par(ScopeVar("s2"), ScopeVar("s1")),
      Dec(ScopeVar("s1"), NameVar("n1")),
      AssocFact(NameVar("n1"), ScopeVar("s2")),
      Ref(NameVar("n2"), ScopeVar("s3")),
      DirectImport(ScopeVar("s3"), ScopeVar("s4")),
      TypeEquals(TypeAppl("ClassType", List(TypeNameAdapter(NameVar("n4")))), TypeAppl("ClassType", List(TypeNameAdapter(NameVar("n1"))))),
      AssocConstraint(NameVar("n4"), ScopeVar("s4")),
      Res(NameVar("n2"), NameVar("n5"))
    )

    assert(Solver.solve(constraints).isEmpty)
  }

  test("incomplete but still consistent") {
    val constraints = List(
      Dec(ScopeVar("s1"),NameVar("n1")),
      Par(ScopeVar("s2"),ScopeVar("s1")),
      Ref(NameVar("n2"),ScopeVar("s3")),
      Res(NameVar("n2"),NameVar("n3")),
      AssocFact(NameVar("n1"),ScopeVar("s2")),
      DirectImport(ScopeVar("s3"),ScopeVar("s4")),
      AssocConstraint(NameVar("n4"),ScopeVar("s4")),
      TypeOf(NameVar("n3"),TypeAppl("Fun", List(TypeAppl("ClassType", List(TypeNameAdapter(NameVar("n4")))), TypeVar("t1")))),
      TypeOf(NameVar("n1"),TypeAppl("Fun", List(TypeVar("t1"), TypeVar("t2"))))
    )

    assert(Solver.solve(constraints).isEmpty)
  }

  test("this should not work") {
    val constraints = List(
      TypeOf(NameVar("n4"),TypeAppl("Fun", List(TypeAppl("ClassType", List(TypeNameAdapter(NameVar("n4")))), TypeAppl("Int", List())))),
      TypeOf(NameVar("n3"),TypeAppl("Fun", List(TypeAppl("Int", List()), TypeVar("t346")))),
      Ref(NameVar("n1"),ScopeVar("s1")),
      Res(NameVar("n1"),NameVar("n4")),
      Res(NameVar("n2"),NameVar("n4")),
      AssocConstraint(NameVar("n4"),ScopeVar("s4")),
      Ref(NameVar("n2"),ScopeVar("s2")),
      DirectImport(ScopeVar("s2"),ScopeVar("s4")),
      Dec(ScopeVar("s3"),NameVar("n3")),
      Par(ScopeVar("s1"),ScopeVar("s3")),
      AssocFact(NameVar("n3"),ScopeVar("s1"))
    )

    assert(Solver.solve(constraints).isEmpty)
  }

  test("concrete names") {
    val constraints = List(
      Ref(ConcreteName("Implicit", "this", 4), ScopeVar("s3")),
      Par(ScopeVar("s3"), ScopeVar("s2")),
      Par(ScopeVar("s2"), ScopeVar("s1")),
      Dec(ScopeVar("s1"), SymbolicName("Class", "n1")),
      Dec(ScopeVar("s2"), SymbolicName("Method", "n3")),
      Dec(ScopeVar("s2"), ConcreteName("Implicit", "this", 2)),
      Res(ConcreteName("Implicit", "this", 4), NameVar("d1"))
    )

    assert(Solver.solve(constraints).isDefined)
  }

  test("example with this") {
    val constraints = List(
      TypeEquals(TypeVar("t2033"),TypeAppl("Int", List())),
      TypeEquals(TypeVar("t2013"),TypeAppl("Bool", List())),
      TypeEquals(TypeVar("t2012"),TypeAppl("IntArray", List())),
      Dec(ScopeVar("s2030"),SymbolicName("Variable", "n2009")),
      TypeOf(SymbolicName("Variable", "n2009"),TypeVar("t2012")),
      TypeEquals(TypeVar("t1999"),TypeAppl("Int", List())),
      TypeEquals(TypeVar("t1995"),TypeAppl("Nil", List())),
      Dec(ScopeVar("s2030"),SymbolicName("Method", "n1961")),
      Par(ScopeVar("s2028"),ScopeVar("s2030")), AssocFact(SymbolicName("Method", "n1961"),ScopeVar("s2028")),
      TypeOf(SymbolicName("Method", "n1961"),TypeAppl("Pair", List(TypeVar("t1995"), TypeVar("t2013")))),
      Dec(ScopeVar("s2032"),SymbolicName("Class", "n1951")),
      TypeOf(SymbolicName("Class", "n1951"),TypeAppl("ClassType", List(TypeNameAdapter(SymbolicName("Class", "n1951"))))),
      Par(ScopeVar("s2030"),ScopeVar("s2032")),
      Dec(ScopeVar("s2030"),ConcreteName("Implicit", "this", 1)),
      TypeOf(ConcreteName("Implicit", "this", 1),TypeAppl("ClassType", List(TypeNameAdapter(SymbolicName("Class", "n1951"))))),
      AssocFact(NameVar("n1951"),ScopeVar("s2030"))
    )

    assert(Solver.solve(constraints).isDefined)
  }

  test("matching concrete names resolve") {
    val constraints = List(
      Dec(ScopeVar("s"), ConcreteName("Implicit", "bar", 1)),
      Ref(ConcreteName("Implicit", "bar", 2), ScopeVar("s")),
      Res(ConcreteName("Implicit", "bar", 2), NameVar("d659"))
    )

    assert(Solver.solve(constraints).nonEmpty)
  }

  test("distinct concrete names do not resolve") {
    val constraints = List(
      Dec(ScopeVar("s"), ConcreteName("Implicit", "foo", 1)),
      Ref(ConcreteName("Implicit", "bar", 2), ScopeVar("s")),
      Res(ConcreteName("Implicit", "bar", 2), NameVar("d659"))
    )

    assert(Solver.solve(constraints).isEmpty)
  }

  test("does it work?") {
    val constraints = List(
      Par(ScopeVar("s714996"),ScopeVar("s714990")),
      Par(ScopeVar("s714990"),ScopeVar("s715011")),
      Par(ScopeVar("s715004"),ScopeVar("s715011")),

      Dec(ScopeVar("s714990"),SymbolicName("Method", "n714947")),
      Dec(ScopeVar("s715011"),SymbolicName("Class", "n714940")),
      Dec(ScopeVar("s714990"),ConcreteName("Implicit", "this", 714962)),
      Dec(ScopeVar("s715011"),SymbolicName("Class", "n714931")),
      Dec(ScopeVar("s715004"),ConcreteName("Implicit", "this", 714963)),

      Ref(ConcreteName("Implicit", "this", 714974),ScopeVar("s714996")),
      Ref(SymbolicName("Class", "n714946"),ScopeVar("s714990")),

      Res(ConcreteName("Implicit", "this", 714974),NameVar("d714975")),
      Res(SymbolicName("Class", "n714946"),NameVar("d714961")),

      TypeOf(NameVar("d714975"),TypeVar("t714972")),
      TypeOf(NameVar("d714961"),TypeVar("t714972")),
      TypeOf(SymbolicName("Method", "n714947"),TypeAppl("Pair", List(TypeAppl("Nil", List()), TypeVar("t714972")))),
      TypeOf(SymbolicName("Class", "n714940"),TypeAppl("ClassType", List(TypeNameAdapter(SymbolicName("Class", "n714940"))))),
      TypeOf(ConcreteName("Implicit", "this", 714962),TypeAppl("ClassType", List(TypeNameAdapter(SymbolicName("Class", "n714940"))))),
      TypeOf(SymbolicName("Class", "n714931"),TypeAppl("ClassType", List(TypeNameAdapter(SymbolicName("Class", "n714931"))))),
      TypeOf(ConcreteName("Implicit", "this", 714963),TypeAppl("ClassType", List(TypeNameAdapter(SymbolicName("Class", "n714931"))))),

      AssocFact(SymbolicName("Method", "n714947"),ScopeVar("s714996")),
      AssocFact(NameVar("n714940"),ScopeVar("s714990")),
      AssocFact(NameVar("n714931"),ScopeVar("s715004"))
    )

    assert(Solver.solve(constraints).nonEmpty)
  }

  test("chained associated import") {
    val constraints = List(
      Par(ScopeVar("s1"), ScopeVar("s")),
      Par(ScopeVar("s2"), ScopeVar("s")),
      Par(ScopeVar("s3"), ScopeVar("s")),
      Dec(ScopeVar("s"), SymbolicName("C", "n1")),
      Dec(ScopeVar("s"), SymbolicName("C", "n2")),
      Dec(ScopeVar("s"), SymbolicName("C", "n3")),
      AssocFact(SymbolicName("C", "n1"), ScopeVar("s1")),
      AssocFact(SymbolicName("C", "n2"), ScopeVar("s2")),
      AssocFact(SymbolicName("C", "n3"), ScopeVar("s3")),
      Ref(SymbolicName("C", "n4"), ScopeVar("s")),
      Ref(SymbolicName("C", "n5"), ScopeVar("s")),
      AssociatedImport(ScopeVar("s1"), SymbolicName("C", "n4")),
      AssociatedImport(ScopeVar("s3"), SymbolicName("C", "n5")),
      Res(SymbolicName("C", "n4"), NameVar("d1")),
      Res(SymbolicName("C", "n5"), NameVar("d2"))
    )

    // NOTE: The solver gives a random solution, so this test will fail in the future.

    assert(Solver.solve(constraints) == Some((
      // Type binding
      Map(),
      // Name binding
      Map(NameVar("d2") -> SymbolicName("C", "n2"), NameVar("d1") -> SymbolicName("C", "n3")),
      // Conditions
      List(
        Eq(SymbolicName("C", "n5"),SymbolicName("C", "n2")),
        Eq(SymbolicName("C", "n4"),SymbolicName("C", "n3"))
      )
    )))
  }

}
