package nl.tudelft.fragments.examples

import nl.tudelft.fragments._

object Simple {
  private val ruleNumber = Rule(
    TermAppl("Number"),
    SortAppl("Exp"),
    TypeVar("t"),
    List(ScopeVar("s")),
    Nil
  )

  private val ruleCons = Rule(
    TermAppl("Cons", List(
      TermVar("e1", SortAppl("Exp"), TypeVar("t1"), List(ScopeVar("s"))),
      TermVar("e2", SortAppl("List"), TypeVar("t2"), List(ScopeVar("s")))
    )),
    SortAppl("List"),
    TypeVar("t"),
    List(ScopeVar("s")),
    Nil
  )

  private val ruleNil = Rule(
    TermAppl("Nil"),
    SortAppl("List"),
    TypeVar("t"),
    List(ScopeVar("s")),
    Nil
  )

  val rules = List(
    ruleNumber,
    ruleCons,
    ruleNil
  )

  val types = List(
    TypeAppl("Int"),
    TypeAppl("Bool")
  )
}
