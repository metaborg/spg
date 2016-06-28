package nl.tudelft.fragments

import org.scalatest.FunSuite

class ConsistencySuite extends FunSuite {

  test("type of") {
    val constraints = List(
      CTypeOf(NameVar("n1"), TypeVar("t1")),
      CTypeOf(NameVar("n1"), TypeAppl("Bool"))
    )

    assert(Consistency.checkTypeOf(constraints))
  }

  test("complicated type of") {
    val constraints = List(
      CTypeOf(SymbolicName("Variable", "n2201"),TypeVar("t2721")),
      CTypeOf(SymbolicName("Variable", "n2201"),TypeVar("t2721")),
      CTypeOf(SymbolicName("Variable", "n2201"),TypeVar("t2198")),
      CTypeOf(SymbolicName("Variable", "n2201"),TypeVar("t176")),
      CTypeOf(SymbolicName("Method", "n1"),TypeAppl("Pair", List(TypeVar("t1"), TypeVar("t176"))))
    )

    assert(Consistency.checkTypeOf(constraints))
  }

  test("type equals") {
    val constraints = List(
      CEqual(TypeVar("t1"), TypeAppl("Int"))
    )

    assert(Consistency.checkTypeEquals(constraints).nonEmpty)
  }

  test("type of and type equals") {
    val constraints = List(
      CTypeOf(NameVar("n1"), TypeVar("t1")),
      CTypeOf(NameVar("n1"), TypeAppl("Bool")),
      CEqual(TypeVar("t1"), TypeAppl("Int"))
    )

    assert(!Consistency.check(State(constraints = constraints)))
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

  test("test 1") {
    val conditions = List(
      Eq(SymbolicName("Class", "n234824"),SymbolicName("Class", "n")),
      Eq(SymbolicName("Variable", "n232952"),SymbolicName("Variable", "n8326")),
      Diseq(SymbolicName("Variable", "n8326"),SymbolicName("Variable", "n234825")),
      Diseq(SymbolicName("Class", "n"),SymbolicName("Variable", "n234825")),
      Diseq(SymbolicName("Class", "n"),ConcreteName("Implicit", "this", 1)),
      Diseq(SymbolicName("Class", "n"),SymbolicName("Variable", "n8326")),
      Diseq(SymbolicName("Class", "n"),SymbolicName("Method", "n232944"))
    )

    assert(Consistency.checkNamingConditions(conditions))
  }

  test("cyclic subtyping in constraints + subtyping relation") {
    val constraints = List(
      FSubtype(TypeAppl("Numeric"), TypeAppl("Int"))
    )

    val subtypingRelation = SubtypeRelation(List(
      (TypeAppl("Int"), TypeAppl("Numeric"))
    ))

    assert(!Consistency.checkSubtyping(State(constraints, Nil, TypeEnv(), Resolution(), subtypingRelation, Nil)))
  }

  test("cyclic subtyping in constraints") {
    val constraints = List(
      FSubtype(TypeAppl("Numeric"), TypeAppl("Int")),
      FSubtype(TypeAppl("Int"), TypeAppl("Numeric"))
    )

    val subtypingRelation = SubtypeRelation()

    assert(!Consistency.checkSubtyping(State(constraints, Nil, TypeEnv(), Resolution(), subtypingRelation, Nil)))
  }

}
