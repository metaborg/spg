package nl.tudelft.fragments

import nl.tudelft.fragments.LabelImplicits._
import org.scalatest.FunSuite

class ResolutionSuite extends FunSuite {
  test("resolution") {
    val facts = List(
      CGRef(SymbolicName("Var", "x"), ScopeVar("s1")),
      CGDecl(ScopeVar("s1"), SymbolicName("Var", "y")),
      CGDecl(ScopeVar("s1"), SymbolicName("Var", "z"))
    )

    val resolution = Resolution(Map(
      SymbolicName("Var", "x") -> SymbolicName("Var", "y")
    ))

    assert(Graph(facts).res(resolution)(SymbolicName("Var", "x")) == List(
      (SymbolicName("Var", "y"), Nil)
    ))
  }

  test("resolution with a direct edge") {
    val facts = List(
      CGRef(SymbolicName("Var", "x"), ScopeVar("s1")),
      CGDirectEdge(ScopeVar("s1"), Label('P'), ScopeVar("s2")),
      CGDecl(ScopeVar("s2"), SymbolicName("Var", "y")),
      CGDecl(ScopeVar("s2"), SymbolicName("Var", "z"))
    )

    val resolution = Resolution(Map(
      SymbolicName("Var", "x") -> SymbolicName("Var", "y")
    ))

    assert(Graph(facts).res(resolution)(SymbolicName("Var", "x")) == List(
      (SymbolicName("Var", "y"), Nil)
    ))
  }

  test("complex resolution") {
    val facts = List(
      CGDecl(ScopeVar("s"), SymbolicName("Class", "x")),
      CGAssoc(SymbolicName("Class", "x"), ScopeVar("s1705")),
      CGDirectEdge(ScopeVar("s1705"), Label('P'), ScopeVar("s")),
      CGDecl(ScopeVar("s1705"), SymbolicName("Var", "x835")),
      CGRef(SymbolicName("Class", "x836"), ScopeVar("s1705")),
      CGDecl(ScopeVar("s1705"), SymbolicName("Var", "x1706"))
    )

    val resolution = Resolution()

    assert(Graph(facts).res(resolution)(SymbolicName("Class", "x836")) == List(
      (SymbolicName("Class", "x"), List(Eq(SymbolicName("Class", "x836"), SymbolicName("Class", "x"))))
    ))
  }

  test("reachable scopes with parent edge") {
    val facts = List(
      CGDirectEdge(ScopeVar("s1"), Label('P'), ScopeVar("s2"))
    )

    assert(Graph(facts).reachableScopes((Label('P') *) ~ (Label('I') *), Nil, Nil, Resolution())(ScopeVar("s1")) == List(
      ScopeVar("s1"),
      ScopeVar("s2")
    ))
  }

  test("reachable scopes with multiple parent edges") {
    val facts = List(
      CGDirectEdge(ScopeVar("s1"), Label('P'), ScopeVar("s2")),
      CGDirectEdge(ScopeVar("s2"), Label('P'), ScopeVar("s3"))
    )

    assert(Graph(facts).reachableScopes((Label('P') *) ~ (Label('I') *), Nil, Nil, Resolution())(ScopeVar("s1")) == List(
      ScopeVar("s1"),
      ScopeVar("s2"),
      ScopeVar("s3")
    ))
  }

  test("reachable scopes with named edge without resolution") {
    val facts = List(
      CGRef(SymbolicName("Class", "parent"), ScopeVar("s1")),
      CGDecl(ScopeVar("s1"), SymbolicName("Class", "x")),
      CGAssoc(SymbolicName("Class", "x"), ScopeVar("s2")),
      CGNamedEdge(ScopeVar("s1"), Label('I'), SymbolicName("Class", "parent"))
    )

    assert(Graph(facts).reachableScopes((Label('P') *) ~ (Label('I') *), Nil, Nil, Resolution())(ScopeVar("s1")) == List(
      ScopeVar("s1")
    ))
  }

  test("reachable scopes with named edge with resolution") {
    val facts = List(
      CGRef(SymbolicName("Class", "parent"), ScopeVar("s1")),
      CGDecl(ScopeVar("s1"), SymbolicName("Class", "x")),
      CGAssoc(SymbolicName("Class", "x"), ScopeVar("s2")),
      CGNamedEdge(ScopeVar("s1"), Label('I'), SymbolicName("Class", "parent"))
    )

    val resolution = Resolution(
      Map(SymbolicName("Class", "parent") -> SymbolicName("Class", "x"))
    )

    assert(Graph(facts).reachableScopes((Label('P') *) ~ (Label('I') *), Nil, Nil, resolution)(ScopeVar("s1")) == List(
      ScopeVar("s1"),
      ScopeVar("s2")
    ))
  }
}
