package nl.tudelft.fragments

import org.scalatest.FunSuite

class SolverSuite extends FunSuite {

  test("single declaration") {
    val constraints = List(
      Par(ScopeVar("s1"),ScopeVar("s2")),
      Dec(ScopeVar("s1"),SymbolicName("n3")),
      Ref(SymbolicName("n1"),ScopeVar("s1")),
      Res(SymbolicName("n1"),NameVar("n2")),
      TypeOf(SymbolicName("n1"),TypeVar("t1")),
      TypeOf(SymbolicName("n3"),TypeVar("t1")),
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

  test("does it work?") {
    val constraints = List(
      Ref(SymbolicName("n1"),ScopeVar("s1")),
      Ref(SymbolicName("n2"),ScopeVar("s3")),
      Dec(ScopeVar("s2"),SymbolicName("n3")),
      Par(ScopeVar("s1"),ScopeVar("s2")),
      AssocFact(SymbolicName("n3"),ScopeVar("s1")),
      DirectImport(ScopeVar("s3"),ScopeVar("s4")),

      Res(SymbolicName("n1"),NameVar("d1")),
      Res(SymbolicName("n2"),NameVar("d2")),
      AssocConstraint(NameVar("d1"),ScopeVar("s4")),
      TypeOf(NameVar("d2"),TypeAppl("Fun", List(TypeVar("t1"), TypeAppl("Int", List())))),
      TypeOf(SymbolicName("n3"),TypeAppl("Fun", List(TypeAppl("Int", List()), TypeVar("t2"))))
    )

    assert(Solver.solve(constraints).nonEmpty)
  }

  test("should have diseq constraints") {
    val constraints = List(
      Ref(SymbolicName("n453"),ScopeVar("s455")),
      Ref(SymbolicName("n36"),ScopeVar("s455")),
      Dec(ScopeVar("s455"),SymbolicName("n442")),
      Dec(ScopeVar("s455"),SymbolicName("n18")),
      Dec(ScopeVar("s46"),SymbolicName("n34")),
      Par(ScopeVar("s452"),ScopeVar("s455")),
      Par(ScopeVar("s46"),ScopeVar("s455")),
      AssocFact(NameVar("n442"),ScopeVar("s452")),
      AssocFact(NameVar("n18"),ScopeVar("s46")),
      TypeOf(SymbolicName("n34"),TypeAppl("Int", List()))
    )

    assert(Solver.solve(constraints).get._3.nonEmpty)
  }

}
