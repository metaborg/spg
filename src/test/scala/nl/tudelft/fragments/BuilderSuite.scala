package nl.tudelft.fragments

import org.scalatest.FunSuite

class BuilderSuite extends FunSuite {
  test("resolve a reference") {
    val rule = Rule(TermVar("x", SortAppl("x"), TypeVar("x"), Nil), SortAppl("x"), TypeVar("x"), Nil, List(
      Dec(ScopeVar("s"), SymbolicName("Class", "n2")),
      Ref(SymbolicName("Class", "n1"), ScopeVar("s")),
      Res(SymbolicName("Class", "n1"), NameVar("n3"))
    ))

    val res = Res(SymbolicName("Class", "n1"), NameVar("n3"))

    assert(Builder.resolve(rule, res, rule.constraints).contains(
      Rule(
        TermVar("x", SortAppl("x", List()), TypeVar("x"), List()),
        SortAppl("x", List()),
        TypeVar("x"),
        List(),
        List(
          Dec(ScopeVar("s"),SymbolicName("Class", "n2")),
          Ref(SymbolicName("Class", "n1"),ScopeVar("s")),

          // The builder made n1 equal to n2 and removed the resolution constraint
          Eq(SymbolicName("Class", "n1"),SymbolicName("Class", "n2"))
        )
      )
    ))
  }
}
