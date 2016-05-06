package nl.tudelft.fragments.examples

import nl.tudelft.fragments.{Dec, NameVar, Par, Rule, ScopeVar, TermAppl, TermVar, TypeAppl, TypeEquals, TypeVar}

object MiniJava {
  private val ruleProgram = Rule(
    TermAppl("Program", List(
      TermVar("x", "Class", TypeVar("t"), ScopeVar("s"))
    )),
    "Program",
    TypeVar("t"),
    ScopeVar("s"),
    Nil
  )

  private val ruleClass = Rule(
    TermAppl("Class", List(
      NameVar("n"),
      TermVar("x", "Method", TypeVar("t"), ScopeVar("s"))
    )),
    "Class",
    TypeVar("t"),
    ScopeVar("s"),
    Nil
  )

  private val ruleMethod = Rule(
    TermAppl("Method", List(
      TermVar("x1", "Type", TypeVar("t2"), ScopeVar("s")),
      NameVar("n1"),
      NameVar("n2"),
      TermVar("x2", "Type", TypeVar("t1"), ScopeVar("s")),
      TermVar("x3", "Exp", TypeVar("t2"), ScopeVar("s1"))
    )),
    "Method",
    TypeVar("t"),
    ScopeVar("s"),
    List(
      TypeEquals(TypeVar("t"), TypeAppl("Fun", List(TypeVar("t1"), TypeVar("t2")))),
      Par(ScopeVar("s1"), ScopeVar("s")),
      Dec(ScopeVar("s1"), NameVar("n2"))
    )
  )

  private val ruleIntType = Rule(
    TermAppl("Int"),
    "Type",
    TypeVar("t"),
    ScopeVar("s"),
    List(
      TypeEquals(TypeVar("t"), TypeAppl("Int"))
    )
  )

  private val ruleBoolType = Rule(
    TermAppl("Bool"),
    "Type",
    TypeVar("t"),
    ScopeVar("s"),
    List(
      TypeEquals(TypeVar("t"), TypeAppl("Bool"))
    )
  )

  private val ruleNumber = Rule(
    TermAppl("Number"),
    "Exp",
    TypeVar("t"),
    ScopeVar("s"),
    List(
      TypeEquals(TypeVar("t"), TypeAppl("Int"))
    )
  )

  private val ruleTrue = Rule(
    TermAppl("True"),
    "Exp",
    TypeVar("t"),
    ScopeVar("s"),
    List(
      TypeEquals(TypeVar("t"), TypeAppl("Bool"))
    )
  )

  private val ruleFalse = Rule(
    TermAppl("False"),
    "Exp",
    TypeVar("t"),
    ScopeVar("s"),
    List(
      TypeEquals(TypeVar("t"), TypeAppl("Bool"))
    )
  )

  val rules = List(
    ruleProgram,
    ruleClass,
    ruleMethod,
    ruleIntType,
    ruleBoolType,
    ruleNumber,
    ruleTrue,
    ruleFalse
  )

  val types = List(
    TypeAppl("Int"),
    TypeAppl("Bool")
  )
}
