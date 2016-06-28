package nl.tudelft.fragments

import org.scalatest.FunSuite

class ResolutionSuite extends FunSuite {
  test("resolution in the presence of existing naming constraints") {
    val facts = List(
      CGRef(SymbolicName("Var", "x"), ScopeVar("s1")),
      CGDecl(ScopeVar("s1"), SymbolicName("Var", "y")),
      CGDecl(ScopeVar("s1"), SymbolicName("Var", "z"))
    )

    val resolution = Resolution(Map(
      SymbolicName("Var", "x") -> SymbolicName("Var", "y")
    ))

    assert(Graph(facts).res(resolution)(SymbolicName("Var", "x")).length == 1)
    assert(Graph(facts).res(resolution)(SymbolicName("Var", "x")).head == (SymbolicName("Var", "y"), List(
      Eq(SymbolicName("Var", "x"), SymbolicName("Var", "y")),
      Diseq(SymbolicName("Var", "x"), SymbolicName("Var", "z"))
    )))
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

    assert(Graph(facts).res(resolution)(SymbolicName("Var", "x")).length == 1)
    assert(Graph(facts).res(resolution)(SymbolicName("Var", "x")).head == (SymbolicName("Var", "y"), List(
      Eq(SymbolicName("Var", "x"), SymbolicName("Var", "y")),
      Diseq(SymbolicName("Var", "x"), SymbolicName("Var", "z"))
    )))
  }

  test("new resolution") {
    val state = State(
      TermAppl("Class", List(TermVar("x"), TermAppl("None", List()), TermAppl("Cons", List(TermAppl("Field", List(TermVar("x1706"), TermVar("x1707"), TermVar("x1708"))), TermAppl("Cons", List(TermAppl("Field", List(TermVar("x835"), TermAppl("ClassDefType", List(TermVar("x836"))), TermVar("x837"))), TermVar("x461"))))))),
      List(CGenRecurse(TermVar("x461"),List(ScopeVar("s1705")),None,SortAppl("List", List(SortAppl("Field", List())))), CGenRecurse(TermVar("x837"),List(ScopeVar("s1705")),Some(TypeVar("t838")),SortAppl("Exp", List())), CSubtype(TypeVar("t838"),TypeAppl("TClassDef", List(TypeVar("t839")))), CGenRecurse(TermVar("x1708"),List(ScopeVar("s1705")),Some(TypeVar("t1709")),SortAppl("Exp", List())), CGenRecurse(TermVar("x1707"),List(ScopeVar("s1705")),Some(TypeVar("t1710")),SortAppl("Type", List())), CSubtype(TypeVar("t1709"),TypeVar("t1710"))),
      List(CGDecl(ScopeVar("s"),SymbolicName("Class", "x")), CGAssoc(SymbolicName("Class", "x"),ScopeVar("s1705")), CGDirectEdge(ScopeVar("s1705"),Label('P'),ScopeVar("s")), CGDecl(ScopeVar("s1705"),SymbolicName("Var", "x835")), CGRef(SymbolicName("Class", "x836"),ScopeVar("s1705")), CGDecl(ScopeVar("s1705"),SymbolicName("Var", "x1706"))),
      TypeEnv(Map(SymbolicName("Class", "x") -> TypeAppl("TClassDef", List(TypeNameAdapter(SymbolicName("Class", "x")))), SymbolicName("Var", "x835") -> TypeAppl("TClassDef", List(TypeVar("t839"))), SymbolicName("Var", "x1706") -> TypeVar("t1710"))),
      Resolution(),
      SubtypeRelation(),
      Nil
    )

    assert(Graph(state.facts).res(state.resolution)(SymbolicName("Class", "x836")) == List(
      (SymbolicName("Class", "x"), List(Eq(SymbolicName("Class", "x836"), SymbolicName("Class", "x"))))
    ))
  }
}
