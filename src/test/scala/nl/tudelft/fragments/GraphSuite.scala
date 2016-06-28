package nl.tudelft.fragments

import org.scalatest.FunSuite

class GraphSuite extends FunSuite {

  test("scope") {
    val constraints = List(
      CGRef(SymbolicName("", "n190"), ScopeVar("s192")),
      CResolve(SymbolicName("", "n190"), NameVar("n193")),
      CTypeOf(SymbolicName("", "n190"), TypeVar("t191")),
      CEqual(TypeVar("t176"), TypeAppl("Fun", List(TypeVar("t191"), TypeVar("t177")))),
      CGDirectEdge(ScopeVar("s192"), ScopeVar("s172")),
      CGDecl(ScopeVar("s192"), SymbolicName("", "n170")),
      CTypeOf(SymbolicName("", "n170"), TypeVar("t191"))
    )

    assert(Graph(constraints).scope(SymbolicName("", "n190")) == List(ScopeVar("s192")))
  }

  test("associated import") {
    val constraints = List(
      CGDirectEdge(ScopeVar("s1"), ScopeVar("s")),
      CGDirectEdge(ScopeVar("s2"), ScopeVar("s")),
      CGDecl(ScopeVar("s"), SymbolicName("C", "n1")),
      CGDecl(ScopeVar("s"), SymbolicName("C", "n2")),
      CGAssoc(SymbolicName("C", "n1"), ScopeVar("s1")),
      CGAssoc(SymbolicName("C", "n2"), ScopeVar("s2")),
      CGRef(SymbolicName("C", "n3"), ScopeVar("s2")),
      CGNamedEdge(ScopeVar("s2"), SymbolicName("C", "n3"))
    )

    assert(Graph(constraints).resolves(Nil, SymbolicName("C", "n3"), Nil) == List((
      List(SymbolicName("C", "n3")),
      List(Parent()),
      SymbolicName("C", "n1"),
      List(Eq(SymbolicName("C", "n3"), SymbolicName("C", "n1")))
      ), (
      List(SymbolicName("C", "n3")),
      List(Parent()),
      SymbolicName("C", "n2"),
      List(Eq(SymbolicName("C", "n3"), SymbolicName("C", "n2")))
      )))
  }

  test("chained associated import") {
    val constraints = List(
      CGDirectEdge(ScopeVar("s1"), ScopeVar("s")),
      CGDirectEdge(ScopeVar("s2"), ScopeVar("s")),
      CGDirectEdge(ScopeVar("s3"), ScopeVar("s")),
      CGDecl(ScopeVar("s"), SymbolicName("C", "n1")),
      CGDecl(ScopeVar("s"), SymbolicName("C", "n2")),
      CGDecl(ScopeVar("s"), SymbolicName("C", "n3")),
      CGAssoc(SymbolicName("C", "n1"), ScopeVar("s1")),
      CGAssoc(SymbolicName("C", "n2"), ScopeVar("s2")),
      CGAssoc(SymbolicName("C", "n3"), ScopeVar("s3")),
      CGRef(SymbolicName("C", "n4"), ScopeVar("s")),
      CGRef(SymbolicName("C", "n5"), ScopeVar("s")),
      CGNamedEdge(ScopeVar("s1"), SymbolicName("C", "n4")),
      CGNamedEdge(ScopeVar("s3"), SymbolicName("C", "n5"))
    )

    assert(Graph(constraints).resolves(Nil, SymbolicName("C", "n4"), Nil) == List((
      List(SymbolicName("C", "n4")),
      List(),
      SymbolicName("C", "n1"),
      List(Eq(SymbolicName("C", "n4"), SymbolicName("C", "n1")))
      ), (
      List(SymbolicName("C", "n4")),
      List(),
      SymbolicName("C", "n2"),
      List(Eq(SymbolicName("C", "n4"), SymbolicName("C", "n2")))
      ), (
      List(SymbolicName("C", "n4")),
      List(),
      SymbolicName("C", "n3"),
      List(Eq(SymbolicName("C", "n4"), SymbolicName("C", "n3")))
      )))
  }

  test("dependent resolution") {
    val constraints = List(
      CGRef(SymbolicName("C", "n0"), ScopeVar("s")),
      CGDecl(ScopeVar("s"), SymbolicName("C", "n1")),
      CGDecl(ScopeVar("s"), SymbolicName("C", "n2")),
      CGAssoc(SymbolicName("C", "n1"), ScopeVar("s2")),
      CGAssoc(SymbolicName("C", "n2"), ScopeVar("s3")),
      CGDecl(ScopeVar("s2"), SymbolicName("V", "n3")),
      CGDecl(ScopeVar("s3"), SymbolicName("V", "n4")),
      CGDirectEdge(ScopeVar("s4"), ScopeVar("s")),
      CGRef(SymbolicName("V", "n5"), ScopeVar("s4")),
      CGNamedEdge(ScopeVar("s4"), SymbolicName("C", "n0"))
    )

    assert(Graph(constraints).resolves(Nil, SymbolicName("V", "n5"), Nil).length == 2)
  }

  test("dependent resolution with naming conditions") {
    val constraints = List(
      CGRef(SymbolicName("C", "n0"), ScopeVar("s")),
      CGDecl(ScopeVar("s"), SymbolicName("C", "n1")),
      CGDecl(ScopeVar("s"), SymbolicName("C", "n2")),
      CGAssoc(SymbolicName("C", "n1"), ScopeVar("s2")),
      CGAssoc(SymbolicName("C", "n2"), ScopeVar("s3")),
      CGDecl(ScopeVar("s2"), SymbolicName("V", "n3")),
      CGDecl(ScopeVar("s3"), SymbolicName("V", "n4")),
      CGDirectEdge(ScopeVar("s4"), ScopeVar("s")),
      CGRef(SymbolicName("V", "n5"), ScopeVar("s4")),
      CGNamedEdge(ScopeVar("s4"), SymbolicName("C", "n0"))
    )

    val conditions = List(
      Eq(SymbolicName("C", "n0"), SymbolicName("C", "n1")),
      Diseq(SymbolicName("C", "n0"), SymbolicName("C", "n2"))
    )

    assert(Graph(constraints).resolves(Nil, SymbolicName("V", "n5"), conditions).length == 1)
  }

}
