package nl.tudelft.fragments

import org.scalatest.FunSuite

class CompletionSuite extends FunSuite {
  test("add missing class") {
    val rule = Rule(
      sort = SortAppl("Start"),
      typ = None,
      scopes = List(ScopeAppl("s1")),
      state = State(
        pattern = TermAppl("Program", List(
          TermAppl("Cons", List(TermVar("x1"), TermAppl("Nil"))),
          TermAppl("NewObject", List(TermVar("x2")))
        )),
        constraints = List(
          CGRef(SymbolicName("Class", "x2"), ScopeAppl("s1")),
          CGenRecurse(TermVar("x1"), List(ScopeAppl("s1")), None, SortAppl("Declaration", List()))
        )
      )
    )

    val fixes = Completion.fix(rule, CGRef(SymbolicName("Class", "x2"), ScopeAppl("s1")))

    assert(fixes.nonEmpty)
  }

  test("add missing field") {
    val rule = Rule(
      sort = SortAppl("Start"),
      typ = None,
      scopes = List(ScopeAppl("s1")),
      state = State(
        pattern = TermAppl("Program", List(
          TermAppl("Cons", List(
            TermAppl("Class", List(
              PatternNameAdapter(SymbolicName("Class", "n1")),
              TermAppl("None"),
              TermAppl("Cons", List(
                TermVar("x1"),
                TermAppl("None")
              ))
            )),
            TermAppl("Nil")
          )),
          TermAppl("QVar", List(
            TermAppl("NewObject", List(
              PatternNameAdapter(SymbolicName("Class", "n2"))
            )),
            PatternNameAdapter(SymbolicName("Field", "n3"))
          ))
        )),
        constraints = List(
          CGDecl(ScopeAppl("s1"), SymbolicName("Class", "n1")),
          CGDirectEdge(ScopeAppl("s2"), Label('P'), ScopeAppl("s1")),
          CGAssoc(SymbolicName("Class", "n1"), ScopeAppl("s2")),
          CTypeOf(SymbolicName("Class", "n1"), TermAppl("TClassDef", List(PatternNameAdapter(SymbolicName("Class", "n1"))))),
          CGRef(SymbolicName("Class", "n2"), ScopeAppl("s1")),
          CResolve(SymbolicName("Class", "n2"), NameVar("d1")),
          CEqual(TermAppl("TClass", List(PatternNameAdapter(NameVar("n2")))), TermAppl("TClass", List(PatternNameAdapter(NameVar("d1"))))),
          CAssoc(NameVar("n2"), ScopeVar("s4")),
          CGRef(SymbolicName("Var", "n3"), ScopeAppl("s3")),
          CGDirectEdge(ScopeAppl("s3"), Label('I'), ScopeVar("s4")),
          CResolve(SymbolicName("Var", "n3"), NameVar("d2")),
          CGenRecurse(TermVar("x1"), List(ScopeAppl("s2")), None, SortAppl("Field"))
        )
      )
    )

    val fixes = Completion.fix(rule, CGRef(SymbolicName("Var", "n3"), ScopeAppl("s3")))

    assert(fixes.nonEmpty)
  }

  test("add missing field with its list") {
    val rule = Rule(
      sort = SortAppl("Start"),
      typ = None,
      scopes = List(ScopeAppl("s1")),
      state = State(
        pattern = TermAppl("Program", List(
          TermAppl("Cons", List(
            TermAppl("Class", List(
              PatternNameAdapter(SymbolicName("Class", "n1")),
              TermAppl("None"),
              TermVar("x1")
            )),
            TermAppl("Nil")
          )),
          TermAppl("QVar", List(
            TermAppl("NewObject", List(
              PatternNameAdapter(SymbolicName("Class", "n2"))
            )),
            PatternNameAdapter(SymbolicName("Field", "n3"))
          ))
        )),
        constraints = List(
          CGDecl(ScopeAppl("s1"), SymbolicName("Class", "n1")),
          CGDirectEdge(ScopeAppl("s2"), Label('P'), ScopeAppl("s1")),
          CGAssoc(SymbolicName("Class", "n1"), ScopeAppl("s2")),
          CTypeOf(SymbolicName("Class", "n1"), TypeAppl("TClassDef", List(TypeNameAdapter(SymbolicName("Class", "n1"))))),
          CGRef(SymbolicName("Class", "n2"), ScopeAppl("s1")),
          CResolve(SymbolicName("Class", "n2"), NameVar("d1")),
          CEqual(TypeAppl("TClass", List(TypeNameAdapter(NameVar("n2")))), TypeAppl("TClass", List(TypeNameAdapter(NameVar("d1"))))),
          CAssoc(NameVar("n2"), ScopeVar("s4")),
          CGRef(SymbolicName("Var", "n3"), ScopeAppl("s3")),
          CGDirectEdge(ScopeAppl("s3"), Label('I'), ScopeVar("s4")),
          CResolve(SymbolicName("Var", "n3"), NameVar("d2")),
          CGenRecurse(TermVar("x1"), List(ScopeAppl("s2")), None, SortAppl("List", List(SortAppl("Field"))))
        )
      )
    )

    val fixes = Completion.fix(rule, CGRef(SymbolicName("Var", "n3"), ScopeAppl("s3")))

    assert(fixes.nonEmpty)
  }
}
