package nl.tudelft.fragments

import com.sun.xml.internal.bind.CycleRecoverable
import nl.tudelft.fragments
import nl.tudelft.fragments.spoofax.{Signatures, Specification}
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

  test("can we add a declaration for the reference in Program(_, Var('x'))") {
    implicit val signatures = Signatures.read(
      strategoPath = "zip:/Users/martijn/Projects/spoofax-releng/stratego/org.metaborg.meta.lang.stratego/target/org.metaborg.meta.lang.stratego-2.0.0-SNAPSHOT.spoofax-language!/",
      signaturePath = "/Users/martijn/Projects/scopes-frames/L3/src-gen/signatures/L3-sig.str"
    )

    implicit val rules = Specification.read(
      nablPath = "zip:/Users/martijn/Projects/nabl/org.metaborg.meta.nabl2.lang/target/org.metaborg.meta.nabl2.lang-2.1.0-SNAPSHOT.spoofax-language!/",
      specPath = "/Users/martijn/Projects/scopes-frames/L3/trans/analysis/l3.nabl2"
    )

    val rule = Rule(SortAppl("Start", List()), None, List(ScopeAppl("s1")), State(
      TermAppl("Program", List(
        TermVar("x2"),
        TermAppl("Var", List(TermVar("x1")))
      )),
      List(
        CGenRecurse(TermVar("x2"), List(ScopeAppl("s1")), None, SortAppl("List", List(SortAppl("Declaration")))),
        CResolve(SymbolicName("Var", "x1"),TermVar("d1")),
        CTypeOf(TermVar("d1"),TermVar("t1"))
      ),
      List(
        CGRef(SymbolicName("Var", "x1"),ScopeAppl("s1"))
      ),
      TypeEnv(),
      Resolution(),
      SubtypeRelation(List()),List()
    ))

    assert(!Consistency.canAddDeclaration(Nil, rule, ScopeAppl("s1"), "Var", rules))
  }

  test("can we add a declaration for the reference in Program(Cons(Class, Nil), QVar(New('n1'), 'n2'))") {
    implicit val signatures = Signatures.read(
      strategoPath = "zip:/Users/martijn/Projects/spoofax-releng/stratego/org.metaborg.meta.lang.stratego/target/org.metaborg.meta.lang.stratego-2.0.0-SNAPSHOT.spoofax-language!/",
      signaturePath = "/Users/martijn/Projects/scopes-frames/L3/src-gen/signatures/L3-sig.str"
    )

    implicit val rules = Specification.read(
      nablPath = "zip:/Users/martijn/Projects/nabl/org.metaborg.meta.nabl2.lang/target/org.metaborg.meta.nabl2.lang-2.1.0-SNAPSHOT.spoofax-language!/",
      specPath = "/Users/martijn/Projects/scopes-frames/L3/trans/analysis/l3.nabl2"
    )

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
      SubtypeRelation(List()),List()
    ))

    assert(!Consistency.canAddDeclaration(Nil, rule, ScopeAppl("s1"), "Class", rules))
  }

  test("can we add a declaration for the reference in Program(Cons(Class, _), QVar(New('n1'), 'n2'))") {
    implicit val signatures = Signatures.read(
      strategoPath = "zip:/Users/martijn/Projects/spoofax-releng/stratego/org.metaborg.meta.lang.stratego/target/org.metaborg.meta.lang.stratego-2.0.0-SNAPSHOT.spoofax-language!/",
      signaturePath = "/Users/martijn/Projects/scopes-frames/L3/src-gen/signatures/L3-sig.str"
    )

    implicit val rules = Specification.read(
      nablPath = "zip:/Users/martijn/Projects/nabl/org.metaborg.meta.nabl2.lang/target/org.metaborg.meta.nabl2.lang-2.1.0-SNAPSHOT.spoofax-language!/",
      specPath = "/Users/martijn/Projects/scopes-frames/L3/trans/analysis/l3.nabl2"
    )

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
      SubtypeRelation(List()),List()
    ))

    assert(Consistency.canAddDeclaration(Nil, rule, ScopeAppl("s1"), "Class", rules))
  }

}
