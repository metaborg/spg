package nl.tudelft.fragments

import org.scalatest.FunSuite

class BuilderSuite extends FunSuite {
  test("resolve a reference") {
    val rule = Rule(TermVar("x", SortAppl("x"), TypeVar("x"), Nil), SortAppl("x"), TypeVar("x"), Nil, List(
      Dec(ScopeVar("s"), SymbolicName("Class", "n2")),
      Ref(SymbolicName("Class", "n1"), ScopeVar("s")),
      Res(SymbolicName("Class", "n1"), NameVar("n3"))
    ))

    val res = Res(SymbolicName("Class", "n1"), NameVar("n3"))

    assert(Builder.resolve(rule, res, rule.constraints).contains(
      Rule(
        TermVar("x", SortAppl("x", List()), TypeVar("x"), List()),
        SortAppl("x", List()),
        TypeVar("x"),
        List(),
        List(
          Dec(ScopeVar("s"),SymbolicName("Class", "n2")),
          Ref(SymbolicName("Class", "n1"),ScopeVar("s")),

          // The builder made n1 equal to n2 and removed the resolution constraint
          Eq(SymbolicName("Class", "n1"),SymbolicName("Class", "n2"))
        )
      )
    ))
  }

  test("x") {
    val rule = Rule(TermAppl("Method", List(TermVar("x1", SortAppl("Type", List()), TypeVar("t2212"), List(ScopeVar("s"))), PatternNameAdapter(SymbolicName("Method", "n1")), TermAppl("Cons", List(TermAppl("Param", List(TermVar("x3748", SortAppl("Type", List()), TypeVar("t3746"), List(ScopeVar("s3747"))), PatternNameAdapter(SymbolicName("Variable", "n3749")))), TermAppl("Nil", List()))), TermVar("x3", SortAppl("List", List(SortAppl("VarDecl", List()))), TypeVar("t4"), List(ScopeVar("s3747"))), TermAppl("Cons", List(TermAppl("If", List(TermVar("x745", SortAppl("Exp", List()), TypeVar("t743"), List(ScopeVar("s3747"))), TermVar("x748", SortAppl("Statement", List()), TypeVar("t746"), List(ScopeVar("s747"))), TermAppl("ArrayAssign", List(PatternNameAdapter(SymbolicName("Variable", "n749")), TermAppl("Add", List(TermAppl("VarRef", List(PatternNameAdapter(SymbolicName("Variable", "n750")))), TermAppl("Sub", List(TermVar("x753", SortAppl("Exp", List()), TypeVar("t751"), List(ScopeVar("s752"))), TermVar("x755", SortAppl("Exp", List()), TypeVar("t754"), List(ScopeVar("s752"))))))), TermVar("x757", SortAppl("Exp", List()), TypeVar("t756"), List(ScopeVar("s752"))))))), TermVar("x759", SortAppl("List", List(SortAppl("Statement", List()))), TypeVar("t758"), List(ScopeVar("s3747"))))), TermAppl("Lt", List(TermVar("x2209", SortAppl("Exp", List()), TypeVar("t2207"), List(ScopeVar("s3747"))), TermAppl("Length", List(TermVar("x2211", SortAppl("Exp", List()), TypeVar("t2210"), List(ScopeVar("s3747"))))))))), SortAppl("MethodDecl", List()), TypeVar("t"), List(ScopeVar("s")), List(Dec(ScopeVar("s3747"),SymbolicName("Variable", "n3749")), TypeOf(SymbolicName("Variable", "n3749"),TypeVar("t3746")), TypeEquals(TypeVar("t3751"),TypeAppl("Nil", List())), TypeEquals(TypeVar("t3750"),TypeAppl("Cons", List(TypeVar("t3752"), TypeVar("t3751")))), TypeEquals(TypeVar("t2210"),TypeAppl("IntArray", List())), TypeEquals(TypeVar("t2213"),TypeAppl("Int", List())), TypeEquals(TypeVar("t2212"),TypeAppl("Bool", List())), TypeEquals(TypeVar("t2207"),TypeAppl("Int", List())), TypeEquals(TypeVar("t2213"),TypeAppl("Int", List())), TypeEquals(TypeVar("t760"),TypeAppl("Int", List())), TypeEquals(TypeVar("t751"),TypeAppl("Int", List())), TypeEquals(TypeVar("t754"),TypeAppl("Int", List())), Ref(SymbolicName("Variable", "n750"),ScopeVar("s752")), Res(SymbolicName("Variable", "n750"),NameVar("d761")), TypeOf(NameVar("d761"),TypeVar("t762")), TypeEquals(TypeVar("t763"),TypeAppl("Int", List())), TypeEquals(TypeVar("t762"),TypeAppl("Int", List())), TypeEquals(TypeVar("t760"),TypeAppl("Int", List())), Ref(SymbolicName("Variable", "n749"),ScopeVar("s752")), Res(SymbolicName("Variable", "n749"),NameVar("d764")), TypeOf(NameVar("d764"),TypeAppl("IntArrayType", List())), TypeEquals(TypeVar("t763"),TypeAppl("Int", List())), TypeEquals(TypeVar("t756"),TypeAppl("Int", List())), TypeEquals(TypeVar("t743"),TypeAppl("Bool", List())), Par(ScopeVar("s747"),ScopeVar("s3747")), Par(ScopeVar("s752"),ScopeVar("s3747")), Dec(ScopeVar("s"),SymbolicName("Method", "n1")), Par(ScopeVar("s3747"),ScopeVar("s")), AssocFact(SymbolicName("Method", "n1"),ScopeVar("s3747")), TypeOf(SymbolicName("Method", "n1"),TypeAppl("Pair", List(TypeVar("t3750"), TypeVar("t2212"))))))

    val res = Res(SymbolicName("Variable", "n750"),NameVar("d761"))

    println(Builder.resolve(rule, res, rule.constraints))
  }
}
