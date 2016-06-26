package nl.tudelft.fragments

import org.scalatest.FunSuite

class ResolutionSuite extends FunSuite {
  test("resolution in the presence of existing naming constraints") {
    val facts = List(
      Ref(SymbolicName("Var", "x"), ScopeVar("s1")),
      Dec(ScopeVar("s1"), SymbolicName("Var", "y")),
      Dec(ScopeVar("s1"), SymbolicName("Var", "z"))
    )

    val nc = List(
      Eq(SymbolicName("Var", "x"), SymbolicName("Var", "y"))
    )

    assert(Graph(facts).res(nc, SymbolicName("Var", "x")).length == 1)
    assert(Graph(facts).res(nc, SymbolicName("Var", "x")).head == (SymbolicName("Var", "y"), List(
      Eq(SymbolicName("Var", "x"), SymbolicName("Var", "y")),
      Diseq(SymbolicName("Var", "x"), SymbolicName("Var", "z"))
    )))
  }

  test("resolution with a direct edge") {
    val facts = List(
      Ref(SymbolicName("Var", "x"), ScopeVar("s1")),
      DirectEdge(ScopeVar("s1"), ScopeVar("s2")),
      Dec(ScopeVar("s2"), SymbolicName("Var", "y")),
      Dec(ScopeVar("s2"), SymbolicName("Var", "z"))
    )

    val nc = List(
      Eq(SymbolicName("Var", "x"), SymbolicName("Var", "y"))
    )

    assert(Graph(facts).res(nc, SymbolicName("Var", "x")).length == 1)
    assert(Graph(facts).res(nc, SymbolicName("Var", "x")).head == (SymbolicName("Var", "y"), List(
      Eq(SymbolicName("Var", "x"), SymbolicName("Var", "y")),
      Diseq(SymbolicName("Var", "x"), SymbolicName("Var", "z"))
    )))
  }
}
