package nl.tudelft.fragments

import nl.tudelft.fragments.spoofax.Language
import nl.tudelft.fragments.spoofax.models.SortAppl
import org.scalatest.FunSuite

class ConsistencySuite extends FunSuite {

  //  test("type of") {
  //    val constraints = List(
  //      CTypeOf(TermVar("n1"), TermVar("t1")),
  //      CTypeOf(TermVar("n1"), TermAppl("Bool"))
  //    )
  //
  //    assert(Consistency.checkTypeOf(constraints))
  //  }
  //
  //  test("complicated type of") {
  //    val constraints = List(
  //      CTypeOf(SymbolicName("Variable", "n2201"),TermVar("t2721")),
  //      CTypeOf(SymbolicName("Variable", "n2201"),TermVar("t2721")),
  //      CTypeOf(SymbolicName("Variable", "n2201"),TermVar("t2198")),
  //      CTypeOf(SymbolicName("Variable", "n2201"),TermVar("t176")),
  //      CTypeOf(SymbolicName("Method", "n1"),TermAppl("Pair", List(TermVar("t1"), TermVar("t176"))))
  //    )
  //
  //    assert(Consistency.checkTypeOf(constraints))
  //  }
  //
  //  test("type equals") {
  //    val constraints = List(
  //      CEqual(TermVar("t1"), TermAppl("Int"))
  //    )
  //
  //    assert(Consistency.checkTypeEquals(constraints).nonEmpty)
  //  }
  //
  //  test("type of and type equals") {
  //    val constraints = List(
  //      CTypeOf(TermVar("n1"), TermVar("t1")),
  //      CTypeOf(TermVar("n1"), TermAppl("Bool")),
  //      CEqual(TermVar("t1"), TermAppl("Int"))
  //    )
  //
  //    assert(!Consistency.checkTypeOf(constraints) || Consistency.checkTypeEquals(constraints).isEmpty)
  //  }
  //
  //  test("consistency of naming conditions") {
  //    val conditions = List(
  //      Diseq(SymbolicName("C", "n1"), SymbolicName("C", "n2")),
  //      Eq(SymbolicName("C", "n3"), SymbolicName("C", "n1")),
  //      Eq(SymbolicName("C", "n3"), SymbolicName("C", "n2"))
  //    )
  //
  //    assert(!Consistency.checkNamingConditions(conditions))
  //  }
  //
  //  test("consistency with same name multiple equalities") {
  //    val conditions = List(
  //      Eq(SymbolicName("Class", "n2156"),SymbolicName("Class", "n")),
  //      Diseq(SymbolicName("Class", "n"),SymbolicName("Method", "n2157")),
  //      Diseq(SymbolicName("Class", "n"),ConcreteName("Implicit", "this", 1)),
  //      Eq(SymbolicName("Class", "n2271"),SymbolicName("Class", "n")),
  //      Diseq(SymbolicName("Class", "n"),SymbolicName("Method", "n2157")),
  //      Diseq(SymbolicName("Class", "n"),ConcreteName("Implicit", "this", 1))
  //    )
  //
  //    assert(Consistency.checkNamingConditions(conditions))
  //  }
  //
  //  test("test 1") {
  //    val conditions = List(
  //      Eq(SymbolicName("Class", "n234824"),SymbolicName("Class", "n")),
  //      Eq(SymbolicName("Variable", "n232952"),SymbolicName("Variable", "n8326")),
  //      Diseq(SymbolicName("Variable", "n8326"),SymbolicName("Variable", "n234825")),
  //      Diseq(SymbolicName("Class", "n"),SymbolicName("Variable", "n234825")),
  //      Diseq(SymbolicName("Class", "n"),ConcreteName("Implicit", "this", 1)),
  //      Diseq(SymbolicName("Class", "n"),SymbolicName("Variable", "n8326")),
  //      Diseq(SymbolicName("Class", "n"),SymbolicName("Method", "n232944"))
  //    )
  //
  //    assert(Consistency.checkNamingConditions(conditions))
  //  }
  //
  //  test("cyclic subtyping in constraints + subtyping relation") {
  //    val constraints = List(
  //      FSubtype(TermAppl("Numeric"), TermAppl("Int"))
  //    )
  //
  //    val subtypingRelation = SubtypeRelation(List(
  //      (TermAppl("Int"), TermAppl("Numeric"))
  //    ))
  //
  //    assert(!Consistency.checkSubtyping(State(constraints, Nil, TypeEnv(), Resolution(), subtypingRelation, Nil)))
  //  }
  //
  //  test("cyclic subtyping in constraints") {
  //    val constraints = List(
  //      FSubtype(TermAppl("Numeric"), TermAppl("Int")),
  //      FSubtype(TermAppl("Int"), TermAppl("Numeric"))
  //    )
  //
  //    val subtypingRelation = SubtypeRelation()
  //
  //    assert(!Consistency.checkSubtyping(State(constraints, Nil, TypeEnv(), Resolution(), subtypingRelation, Nil)))
  //  }

  val language = Language.load("/Users/martijn/Projects/scopes-frames/L3", "org.metaborg:L3:0.1.0-SNAPSHOT", "L3")

  // Make the various language specifications implicitly available
  implicit val productions = language.productions
  implicit val signatures = language.signatures
  implicit val specification = language.specification
  implicit val printer = language.printer
  implicit val rules = specification.rules

  test("can we add a declaration for the reference in Program(_, Var('x'))") {
    val rule = Rule(SortAppl("Start", List()), None, List(ScopeAppl("s1")), State(
      TermAppl("Program", List(
        TermVar("x2"),
        TermAppl("Var", List(TermVar("x1")))
      )),
      List(
        CGenRecurse(TermVar("x2"), List(ScopeAppl("s1")), None, SortAppl("List", List(SortAppl("Declaration")))),
        CResolve(SymbolicName("Var", "x1"), TermVar("d1")),
        CTypeOf(TermVar("d1"), TermVar("t1"))
      ),
      List(
        CGRef(SymbolicName("Var", "x1"), ScopeAppl("s1"))
      ),
      TypeEnv(),
      Resolution(),
      SubtypeRelation(List()), List()
    ))

  }

  test("can we add a declaration for the reference in complicated program") {
    val rule = Rule(SortAppl("Start", List()), None, List(ScopeAppl("s42393")), State(TermAppl("Program", List(TermAppl("Cons", List(TermAppl("Class", List(TermVar("x28129"), TermAppl("Parent", List(TermVar("x28130"))), TermAppl("Nil", List()))), TermAppl("Nil", List()))), TermAppl("Fun", List(TermVar("x11033"), TermAppl("FunType", List(TermAppl("FunType", List(TermAppl("FunType", List(TermVar("x41984"), TermAppl("FunType", List(TermVar("x42394"), TermVar("x42395"))))), TermAppl("FunType", List(TermAppl("ClassType", List(TermVar("x39241"))), TermAppl("IntType", List()))))), TermAppl("ClassDefType", List(TermVar("x29497"))))), TermAppl("App", List(TermAppl("Var", List(TermVar("x30393"))), TermAppl("Assign", List(TermAppl("Var", List(TermVar("x12960"))), TermAppl("Add", List(TermAppl("IntValue", List(TermVar("x11037"))), TermAppl("Seq", List(TermAppl("Fun", List(TermVar("x12540"), TermAppl("FunType", List(TermAppl("ClassDefType", List(TermVar("x29251"))), TermAppl("ClassType", List(TermVar("x29723"))))), TermAppl("IntValue", List(TermVar("x13343"))))), TermAppl("QVar", List(TermAppl("Var", List(TermVar("x22290"))), TermVar("x13030"))))))))))))))), List(CTypeOf(SymbolicName("Var", "x11033"), TermAppl("TFun", List(TermAppl("TFun", List(TermAppl("TFun", List(TermVar("x41987"), TermAppl("TFun", List(TermVar("x42397"), TermVar("x42396"))))), TermAppl("TFun", List(TermAppl("TClass", List(TermVar("x39242"))), TermAppl("TInt", List()))))), TermAppl("TClassDef", List(TermVar("x29498")))))), CSubtype(TermVar("x12962"), TermVar("x11041")), CSubtype(TermAppl("TInt", List()), TermVar("x12962")), CTrue(), CTypeOf(SymbolicName("Var", "x12540"), TermAppl("TFun", List(TermAppl("TClassDef", List(TermVar("x29252"))), TermAppl("TClass", List(TermVar("x29724")))))), CResolve(SymbolicName("Var", "x12960"), TermVar("x12961")), CTypeOf(TermVar("x12961"), TermVar("x12962")), CAssoc(TermVar("x13031"), ScopeVar("s13032")), CResolve(SymbolicName("Var", "x13030"), TermVar("x13033")), CTypeOf(TermVar("x13033"), TermAppl("TInt", List())), CTrue(), CResolve(SymbolicName("Var", "x22290"), TermVar("x22291")), CTypeOf(TermVar("x22291"), TermAppl("TClass", List(TermVar("x13031")))), CTrue(), CTypeOf(SymbolicName("Class", "x28129"), TermAppl("TClassDef", List(SymbolicName("Class", "x28129")))), CResolve(SymbolicName("Class", "x28130"), TermVar("x28133")), CAssoc(TermVar("x28133"), ScopeVar("s28134")), CTypeOf(TermVar("x28133"), TermAppl("TClassDef", List(TermVar("x28133")))), FSubtype(TermAppl("TClass", List(SymbolicName("Class", "x28129"))), TermAppl("TClass", List(TermVar("x28133")))), CTrue(), CResolve(SymbolicName("Class", "x29251"), TermVar("x29252")), CResolve(SymbolicName("Class", "x29497"), TermVar("x29498")), CResolve(SymbolicName("Class", "x29723"), TermVar("x29724")), CResolve(SymbolicName("Var", "x30393"), TermVar("x30394")), CTypeOf(TermVar("x30394"), TermAppl("TFun", List(TermVar("x11041"), TermVar("x11042")))), CTrue(), CResolve(SymbolicName("Class", "x39241"), TermVar("x39242")), CGenRecurse(TermVar("x41984"), List(ScopeAppl("s42393")), Some(TermVar("x41987")), SortAppl("Type", List())), CGenRecurse(TermVar("x42395"), List(ScopeAppl("s42393")), Some(TermVar("x42396")), SortAppl("Type", List())), CGenRecurse(TermVar("x42394"), List(ScopeAppl("s42393")), Some(TermVar("x42397")), SortAppl("Type", List()))), List(CGDecl(ScopeAppl("s30392"), SymbolicName("Var", "x11033")), CGDirectEdge(ScopeAppl("s30392"), Label('P'), ScopeAppl("s42393")), CGDecl(ScopeAppl("s13342"), SymbolicName("Var", "x12540")), CGDirectEdge(ScopeAppl("s13342"), Label('P'), ScopeAppl("s30392")), CGRef(SymbolicName("Var", "x12960"), ScopeAppl("s30392")), CGDirectEdge(ScopeAppl("s13035"), Label('I'), ScopeVar("s13032")), CGRef(SymbolicName("Var", "x13030"), ScopeAppl("s13035")), CGRef(SymbolicName("Var", "x22290"), ScopeAppl("s30392")), CGDecl(ScopeAppl("s42393"), SymbolicName("Class", "x28129")), CGAssoc(SymbolicName("Class", "x28129"), ScopeAppl("s28762")), CGRef(SymbolicName("Class", "x28130"), ScopeAppl("s42393")), CGDirectEdge(ScopeAppl("s28762"), Label('I'), ScopeVar("s28134")), CGDirectEdge(ScopeAppl("s28762"), Label('P'), ScopeAppl("s42393")), CGRef(SymbolicName("Class", "x29251"), ScopeAppl("s30392")), CGRef(SymbolicName("Class", "x29497"), ScopeAppl("s42393")), CGRef(SymbolicName("Class", "x29723"), ScopeAppl("s30392")), CGRef(SymbolicName("Var", "x30393"), ScopeAppl("s30392")), CGRef(SymbolicName("Class", "x39241"), ScopeAppl("s42393"))), TypeEnv(), Resolution(), SubtypeRelation(List()), List()))

    assert(!Consistency.canAddDeclaration(Nil, rule, ScopeAppl("s30392"), "Var", rules))
  }

  test("can we add a declaration for the reference in Program(Cons(Class, Nil), QVar(New('n1'), 'n2'))") {
    val rule = Rule(SortAppl("Start", List()), None, List(ScopeAppl("s1")), State(
      TermAppl("Program", List(
        TermAppl("Cons", List(
          TermAppl("Class", List(
            TermVar("x1"),
            TermAppl("None"),
            TermAppl("Nil")
          )),
          TermAppl("Nil")
        )),
        TermAppl("QVar", List(
          TermAppl("NewObject", List(
            TermVar("x2")
          )),
          TermVar("x3")
        ))
      )),
      List(
        CResolve(SymbolicName("Class", "x2"), TermVar("d1")),
        CTypeOf(TermVar("d1"), TermAppl("ClassType", List(TermVar("d1")))),
        CAssoc(TermVar("d1"), ScopeVar("s4")),
        CResolve(SymbolicName("Class", "x3"), TermVar("d2")),
        CGDirectEdge(ScopeAppl("s3"), Label('I'), ScopeVar("s4"))
      ),
      List(
        CGDecl(ScopeAppl("s1"), SymbolicName("Class", "x1")),
        CGDirectEdge(ScopeAppl("s2"), Label('P'), ScopeAppl("s1")),
        CGAssoc(SymbolicName("Class", "x1"), ScopeAppl("s2")),
        CGRef(SymbolicName("Class", "x2"), ScopeAppl("s1")),
        CGRef(SymbolicName("Class", "x3"), ScopeAppl("s3"))
      ),
      TypeEnv(),
      Resolution(),
      SubtypeRelation(List()), List()
    ))

    assert(!Consistency.canAddDeclaration(Nil, rule, ScopeAppl("s1"), "Class", rules))
  }

  test("can we add a declaration for the reference in Program(Cons(Class, _), QVar(New('n1'), 'n2'))") {
    val rule = Rule(SortAppl("Start", List()), None, List(ScopeAppl("s1")), State(
      TermAppl("Program", List(
        TermAppl("Cons", List(
          TermAppl("Class", List(
            TermVar("x1"),
            TermAppl("None"),
            TermAppl("Nil")
          )),
          TermVar("x4")
        )),
        TermAppl("QVar", List(
          TermAppl("NewObject", List(
            TermVar("x2")
          )),
          TermVar("x3")
        ))
      )),
      List(
        CResolve(SymbolicName("Class", "x2"), TermVar("d1")),
        CTypeOf(TermVar("d1"), TermAppl("ClassType", List(TermVar("d1")))),
        CAssoc(TermVar("d1"), ScopeVar("s4")),
        CResolve(SymbolicName("Class", "x3"), TermVar("d2")),
        CGenRecurse(TermVar("x4"), List(ScopeAppl("s1")), None, SortAppl("List", List(SortAppl("Declaration"))))
      ),
      List(
        CGDirectEdge(ScopeAppl("s3"), Label('I'), ScopeVar("s4")),
        CGDecl(ScopeAppl("s1"), SymbolicName("Class", "x1")),
        CGDirectEdge(ScopeAppl("s2"), Label('P'), ScopeAppl("s1")),
        CGAssoc(SymbolicName("Class", "x1"), ScopeAppl("s2")),
        CGRef(SymbolicName("Class", "x2"), ScopeAppl("s1")),
        CGRef(SymbolicName("Class", "x3"), ScopeAppl("s3"))
      ),
      TypeEnv(),
      Resolution(),
      SubtypeRelation(List()), List()
    ))

    assert(Consistency.canAddDeclaration(Nil, rule, ScopeAppl("s1"), "Class", rules))
  }

  test("can we satisfy the type in a recurse in Program(Nil, QVar(x, 'n1'))") {
    val rule = Rule(SortAppl("Start", List()), None, List(ScopeAppl("s1")), State(
      TermAppl("Program", List(
        TermAppl("Nil"),
        TermAppl("QVar", List(
          TermVar("x1"),
          TermVar("x2")
        ))
      )),
      List(
        CGenRecurse(TermVar("x1"), List(ScopeAppl("s1")), Some(TermAppl("t1")), SortAppl("Exp")),
        CEqual(TermAppl("t1"), TermAppl("ClassType", List(TermVar("d1")))),
        CResolve(SymbolicName("Var", "x2"), TermVar("d2")),
        CAssoc(TermVar("d1"), ScopeVar("s3"))
      ),
      List(
        CGRef(SymbolicName("Var", "x2"), ScopeAppl("s2")),
        CGDirectEdge(ScopeAppl("s2"), Label('I'), ScopeVar("s3"))
      ),
      TypeEnv(),
      Resolution(),
      SubtypeRelation(List()), List()
    ))

    // TODO
    //assert(Consistency.canSatisfyType(Nil, rule, ScopeAppl("s1"), "Class", rules))
  }

  test("resolution consistent") {
    val rule = Rule(SortAppl("Start", List()), None, List(ScopeAppl("s183889")), State(TermAppl("Program", List(TermAppl("Cons", List(TermAppl("Class", List(TermVar("x182328"), TermAppl("None", List()), TermAppl("Nil", List()))), TermAppl("Nil", List()))), TermAppl("NewObject", List(TermVar("x183890"))))), List(CTrue(), CTypeOf(SymbolicName("Class", "x182328"), TermAppl("TClassDef", List(SymbolicName("Class", "x182328")))), CTrue(), CResolve(SymbolicName("Class", "x183890"), TermVar("x183891"))), List(CGDecl(ScopeAppl("s183889"), SymbolicName("Class", "x182328")), CGAssoc(SymbolicName("Class", "x182328"), ScopeAppl("s183069")), CGDirectEdge(ScopeAppl("s183069"), Label('P'), ScopeAppl("s183889")), CGRef(SymbolicName("Class", "x183890"), ScopeAppl("s183889"))), TypeEnv(), Resolution(), SubtypeRelation(List()), List()))

    assert(Consistency.decidedDeclarationsConsistency(rule))
  }

}
