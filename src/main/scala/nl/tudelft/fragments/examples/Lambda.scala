package nl.tudelft.fragments.examples

import nl.tudelft.fragments._

object Lambda {
  private val ruleNumber = Rule(
    TermAppl("Number"),
    SortAppl("Exp"),
    TypeVar("t"),
    List(ScopeVar("s")),
    State(List(
      CEqual(TypeVar("t"), TypeAppl("Int"))
    ))
  )

  private val ruleInt = Rule(
    TermAppl("Int"),
    SortAppl("Type"),
    TypeVar("t"),
    List(ScopeVar("s")),
    State(List(
      CEqual(TypeVar("t"), TypeAppl("Int"))
    ))
  )

  private val ruleAdd = Rule(
    TermAppl("Add", List(
      Var("e1", SortAppl("Exp"), TypeVar("t1"), List(ScopeVar("s"))),
      Var("e2", SortAppl("Exp"), TypeVar("t2"), List(ScopeVar("s")))
    )),
    SortAppl("Exp"),
    TypeVar("t"),
    List(ScopeVar("s")),
    State(List(
      CEqual(TypeVar("t"), TypeAppl("Int")),
      CEqual(TypeVar("t1"), TypeAppl("Int")),
      CEqual(TypeVar("t2"), TypeAppl("Int"))
    ))
  )

  private val ruleAbs = Rule(
    TermAppl("Abs", List(
      PatternNameAdapter(SymbolicName("Variable", "n")),
      Var("e1", SortAppl("Type"), TypeVar("t1"), List(ScopeVar("s"))),
      Var("e2", SortAppl("Exp"), TypeVar("t2"), List(ScopeVar("s1")))
    )),
    SortAppl("Exp"),
    TypeVar("t"),
    List(ScopeVar("s")),
    State(List(
      CEqual(TypeVar("t"), TypeAppl("Fun", List(TypeVar("t1"), TypeVar("t2")))),
      CGDirectEdge(ScopeVar("s1"), ScopeVar("s")),
      CGDecl(ScopeVar("s1"), SymbolicName("Variable", "n")),
      CTypeOf(SymbolicName("Variable", "n"), TypeVar("t1"))
    ))
  )

  private val ruleApp = Rule(
    TermAppl("App", List(
      Var("e1", SortAppl("Exp"), TypeVar("t1"), List(ScopeVar("s"))),
      Var("e2", SortAppl("Exp"), TypeVar("t2"), List(ScopeVar("s")))
    )),
    SortAppl("Exp"),
    TypeVar("t"),
    List(ScopeVar("s")),
    State(List(
      CEqual(TypeVar("t1"), TypeAppl("Fun", List(TypeVar("t2"), TypeVar("t"))))
    ))
  )

  private val ruleVar = Rule(
    TermAppl("Var", List(
      PatternNameAdapter(SymbolicName("Variable", "n"))
    )),
    SortAppl("Exp"),
    TypeVar("t"),
    List(ScopeVar("s")),
    State(List(
      CGRef(SymbolicName("Variable", "n"), ScopeVar("s")),
      CResolve(SymbolicName("Variable", "n"), NameVar("d")),
      CTypeOf(NameVar("d"), TypeVar("t"))
    ))
  )

  val rules = List(
    ruleNumber,
    ruleAdd,
    ruleInt,
    ruleAbs,
    ruleApp,
    ruleVar
  )

  val types = List(
    TypeAppl("Int"),
    TypeAppl("Bool")
  )
}
