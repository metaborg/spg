package nl.tudelft.fragments

import org.scalatest.FunSuite
import Graph._

class GraphSuite extends FunSuite {

  test("scope") {
    val constraints = List(
      Ref(SymbolicName("", "n190"),ScopeVar("s192")),
      Res(SymbolicName("", "n190"),NameVar("n193")),
      TypeOf(SymbolicName("", "n190"),TypeVar("t191")),
      TypeEquals(TypeVar("t176"),TypeAppl("Fun", List(TypeVar("t191"), TypeVar("t177")))),
      Par(ScopeVar("s192"),ScopeVar("s172")),
      Dec(ScopeVar("s192"),SymbolicName("", "n170")),
      TypeOf(SymbolicName("", "n170"),TypeVar("t191"))
    )

    assert(scope(SymbolicName("", "n190"), constraints) == List(ScopeVar("s192")))
  }

  test("visible") {
    val constraints = List(
      Ref(NameVar("n190"),ScopeVar("s192")),
      Res(NameVar("n190"),NameVar("n193")),
      TypeOf(NameVar("n190"),TypeVar("t191")),
      TypeEquals(TypeVar("t176"),TypeAppl("Fun", List(TypeVar("t191"), TypeVar("t177")))),
      Par(ScopeVar("s192"),ScopeVar("s172")),
      Dec(ScopeVar("s192"),NameVar("n170")),
      TypeOf(NameVar("n170"),TypeVar("t191"))
    )

    assert(visible(ScopeVar("s192"), constraints) == List(
      (List(), NameVar("n170"), List())
    ))
  }



}
