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

}
