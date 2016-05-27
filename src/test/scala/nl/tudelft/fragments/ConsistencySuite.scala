package nl.tudelft.fragments

import org.scalatest.FunSuite

class ConsistencySuite extends FunSuite {

  test("type of") {
    val constraints = List(
      TypeOf(NameVar("n1"), TypeVar("t1")),
      TypeOf(NameVar("n1"), TypeAppl("Bool"))
    )

    assert(Consistency.checkTypeOf(constraints))
  }

  test("complicated type of") {
    val constraints = List(
      TypeOf(SymbolicName("Variable", "n2201"),TypeVar("t2721")),
      TypeOf(SymbolicName("Variable", "n2201"),TypeVar("t2721")),
      TypeOf(SymbolicName("Variable", "n2201"),TypeVar("t2198")),
      TypeOf(SymbolicName("Variable", "n2201"),TypeVar("t176")),
      TypeOf(SymbolicName("Method", "n1"),TypeAppl("Pair", List(TypeVar("t1"), TypeVar("t176"))))
    )

    assert(Consistency.checkTypeOf(constraints))
  }

  test("type equals") {
    val constraints = List(
      TypeEquals(TypeVar("t1"), TypeAppl("Int"))
    )

    assert(Consistency.checkTypeEquals(constraints).nonEmpty)
  }

  test("type of and type equals") {
    val constraints = List(
      TypeOf(NameVar("n1"), TypeVar("t1")),
      TypeOf(NameVar("n1"), TypeAppl("Bool")),
      TypeEquals(TypeVar("t1"), TypeAppl("Int"))
    )

    assert(!Consistency.check(constraints))
  }

  test("no cycle in disequalities") {
    val disEqs = List(
      Diseq(SymbolicName("C", "n1"), SymbolicName("C", "n2"))
    )

    assert(!Consistency.detectCycle(disEqs))
  }

  test("cycle in disequalities") {
    val disEqs = List(
      Diseq(SymbolicName("C", "n1"), SymbolicName("C", "n2")),
      Diseq(SymbolicName("C", "n2"), SymbolicName("C", "n3")),
      Diseq(SymbolicName("C", "n3"), SymbolicName("C", "n1"))
    )

    assert(Consistency.detectCycle(disEqs))
  }

  test("consistency of naming conditions") {
    val conditions = List(
      Diseq(SymbolicName("C", "n1"), SymbolicName("C", "n2")),
      Eq(SymbolicName("C", "n3"), SymbolicName("C", "n1")),
      Eq(SymbolicName("C", "n3"), SymbolicName("C", "n2"))
    )

    assert(!Consistency.checkNamingConditions(conditions))
  }

  test("consistency with same name multiple equalities") {
    val conditions = List(
      Eq(SymbolicName("Class", "n2156"),SymbolicName("Class", "n")),
      Diseq(SymbolicName("Class", "n"),SymbolicName("Method", "n2157")),
      Diseq(SymbolicName("Class", "n"),ConcreteName("Implicit", "this", 1)),
      Eq(SymbolicName("Class", "n2271"),SymbolicName("Class", "n")),
      Diseq(SymbolicName("Class", "n"),SymbolicName("Method", "n2157")),
      Diseq(SymbolicName("Class", "n"),ConcreteName("Implicit", "this", 1))
    )

    assert(Consistency.checkNamingConditions(conditions))
  }

}
