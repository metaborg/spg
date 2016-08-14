package nl.tudelft.fragments

import org.scalatest.FunSuite

class BuilderSuite extends FunSuite {
  test("resolve a reference") {
    val rule = Rule(TermVar("x", SortAppl("x"), TypeVar("x"), Nil), SortAppl("x"), TypeVar("x"), Nil, State(List(
      CGDecl(ScopeVar("s"), SymbolicName("Class", "n2")),
      CGRef(SymbolicName("Class", "n1"), ScopeVar("s")),
      CResolve(SymbolicName("Class", "n1"), NameVar("n3"))
    )))

    val res = CResolve(SymbolicName("Class", "n1"), NameVar("n3"))

    assert(Builder.resolve(rule, res, SymbolicName("Class", "n2")) == Rule(
      TermVar("x", SortAppl("x", List()), TypeVar("x"), List()),
      SortAppl("x", List()),
      TypeVar("x"),
      List(),
      // The builder made n1 equal to n2 and removed the resolution constraint
      State(
        Nil,
        List(
          CGDecl(ScopeVar("s"),SymbolicName("Class", "n2")),
          CGRef(SymbolicName("Class", "n1"),ScopeVar("s"))
        ),
        TypeEnv(),
        Resolution(),
        List(
          Eq(SymbolicName("Class", "n1"),SymbolicName("Class", "n2"))
        )
      )
    ))
  }
}
