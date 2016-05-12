package nl.tudelft.fragments.examples

import nl.tudelft.fragments._

object Lambda {
  private val ruleNumber = Rule(
    TermAppl("Number"),
    SortAppl("Exp"),
    TypeVar("t"),
    ScopeVar("s"),
    List(
      TypeEquals(TypeVar("t"), TypeAppl("Int"))
    )
  )

  private val ruleInt = Rule(
    TermAppl("Int"),
    SortAppl("Type"),
    TypeVar("t"),
    ScopeVar("s"),
    List(
      TypeEquals(TypeVar("t"), TypeAppl("Int"))
    )
  )

  private val ruleAdd = Rule(
    TermAppl("Add", List(
      TermVar("e1", SortAppl("Exp"), TypeVar("t1"), ScopeVar("s")),
      TermVar("e2", SortAppl("Exp"), TypeVar("t2"), ScopeVar("s"))
    )),
    SortAppl("Exp"),
    TypeVar("t"),
    ScopeVar("s"),
    List(
      TypeEquals(TypeVar("t"), TypeAppl("Int")),
      TypeEquals(TypeVar("t1"), TypeAppl("Int")),
      TypeEquals(TypeVar("t2"), TypeAppl("Int"))
    )
  )

  private val ruleAbs = Rule(
    TermAppl("Abs", List(
      PatternNameAdapter(SymbolicName("n")),
      TermVar("e1", SortAppl("Type"), TypeVar("t1"), ScopeVar("s")),
      TermVar("e2", SortAppl("Exp"), TypeVar("t2"), ScopeVar("s1"))
    )),
    SortAppl("Exp"),
    TypeVar("t"),
    ScopeVar("s"),
    List(
      TypeEquals(TypeVar("t"), TypeAppl("Fun", List(TypeVar("t1"), TypeVar("t2")))),
      Par(ScopeVar("s1"), ScopeVar("s")),
      Dec(ScopeVar("s1"), SymbolicName("n")),
      TypeOf(SymbolicName("n"), TypeVar("t1"))
    )
  )

  private val ruleApp = Rule(
    TermAppl("App", List(
      TermVar("e1", SortAppl("Exp"), TypeVar("t1"), ScopeVar("s")),
      TermVar("e2", SortAppl("Exp"), TypeVar("t2"), ScopeVar("s"))
    )),
    SortAppl("Exp"),
    TypeVar("t"),
    ScopeVar("s"),
    List(
      TypeEquals(TypeVar("t1"), TypeAppl("Fun", List(TypeVar("t2"), TypeVar("t"))))
    )
  )

  private val ruleVar = Rule(
    TermAppl("Var", List(
      PatternNameAdapter(SymbolicName("n"))
    )),
    SortAppl("Exp"),
    TypeVar("t"),
    ScopeVar("s"),
    List(
      Ref(SymbolicName("n"), ScopeVar("s")),
      Res(SymbolicName("n"), NameVar("d")),
      TypeOf(NameVar("d"), TypeVar("t"))
    )
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
