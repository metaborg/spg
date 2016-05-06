package nl.tudelft.fragments

import org.scalatest.FunSuite

class SolverSuite extends FunSuite {

  test("single declaration") {
    val constraints = List(
      Ref(NameVar("n190"),ScopeVar("s192")),
      Res(NameVar("n190"),NameVar("n193")),
      TypeOf(NameVar("n190"),TypeVar("t191")),
      TypeEquals(TypeVar("t176"),TypeAppl("Fun", List(TypeVar("t191"), TypeVar("t177")))),
      Par(ScopeVar("s192"),ScopeVar("s172")),
      Dec(ScopeVar("s192"),NameVar("n170")),
      TypeOf(NameVar("n170"),TypeVar("t191"))
    )

    assert(Solver.solve(constraints, List(TypeAppl("Int"))).nonEmpty)
  }

}
