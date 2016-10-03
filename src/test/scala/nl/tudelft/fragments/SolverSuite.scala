package nl.tudelft.fragments

import nl.tudelft.fragments.spoofax.Language
import org.scalatest.FunSuite

class SolverSuite extends FunSuite {

//  test("single declaration") {
//    val constraints = List(
//      CGDirectEdge(ScopeVar("s1"),Label('P'),ScopeVar("s2")),
//      CGDecl(ScopeVar("s1"),SymbolicName("", "n3")),
//      CGRef(SymbolicName("", "n1"),ScopeVar("s1")),
//      CResolve(SymbolicName("", "n1"),NameVar("n2")),
//      CTypeOf(SymbolicName("", "n1"),TypeVar("t1")),
//      CTypeOf(SymbolicName("", "n3"),TypeVar("t1")),
//      CEqual(TypeVar("t2"),TypeAppl("Fun", List(TypeVar("t1"), TypeVar("t3"))))
//    )
//
//    assert(Solver.solve(constraints).nonEmpty)
//  }
//
//  test("impossible") {
//    val constraints = List(
//      CGDirectEdge(ScopeVar("s2"),Label('P'), ScopeVar("s1")),
//      CGDecl(ScopeVar("s1"), NameVar("n1")),
//      CGAssoc(NameVar("n1"), ScopeVar("s2")),
//      CGRef(NameVar("n2"), ScopeVar("s3")),
//      CGDirectEdge(ScopeVar("s3"), Label('I'), ScopeVar("s4")),
//      CEqual(TypeAppl("ClassType", List(TypeNameAdapter(NameVar("n4")))), TypeAppl("ClassType", List(TypeNameAdapter(NameVar("n1"))))),
//      CAssoc(NameVar("n4"), ScopeVar("s4")),
//      CResolve(NameVar("n2"), NameVar("n5"))
//    )
//
//    assert(Solver.solve(constraints).isEmpty)
//  }
//
//  test("incomplete but still consistent") {
//    val constraints = List(
//      CGDecl(ScopeVar("s1"),NameVar("n1")),
//      CGDirectEdge(ScopeVar("s2"),Label('P'),ScopeVar("s1")),
//      CGRef(NameVar("n2"),ScopeVar("s3")),
//      CResolve(NameVar("n2"),NameVar("n3")),
//      CGAssoc(NameVar("n1"),ScopeVar("s2")),
//      CGDirectEdge(ScopeVar("s3"),Label('I'),ScopeVar("s4")),
//      CAssoc(NameVar("n4"),ScopeVar("s4")),
//      CTypeOf(NameVar("n3"),TypeAppl("Fun", List(TypeAppl("ClassType", List(TypeNameAdapter(NameVar("n4")))), TypeVar("t1")))),
//      CTypeOf(NameVar("n1"),TypeAppl("Fun", List(TypeVar("t1"), TypeVar("t2"))))
//    )
//
//    assert(Solver.solve(constraints).isEmpty)
//  }
//
//  test("this should not work") {
//    val constraints = List(
//      CTypeOf(NameVar("n4"),TypeAppl("Fun", List(TypeAppl("ClassType", List(TypeNameAdapter(NameVar("n4")))), TypeAppl("Int", List())))),
//      CTypeOf(NameVar("n3"),TypeAppl("Fun", List(TypeAppl("Int", List()), TypeVar("t346")))),
//      CGRef(NameVar("n1"),ScopeVar("s1")),
//      CResolve(NameVar("n1"),NameVar("n4")),
//      CResolve(NameVar("n2"),NameVar("n4")),
//      CAssoc(NameVar("n4"),ScopeVar("s4")),
//      CGRef(NameVar("n2"),ScopeVar("s2")),
//      CGDirectEdge(ScopeVar("s2"),Label('I'),ScopeVar("s4")),
//      CGDecl(ScopeVar("s3"),NameVar("n3")),
//      CGDirectEdge(ScopeVar("s1"),Label('P'),ScopeVar("s3")),
//      CGAssoc(NameVar("n3"),ScopeVar("s1"))
//    )
//
//    assert(Solver.solve(constraints).isEmpty)
//  }
//
//  test("concrete names") {
//    val constraints = List(
//      CGRef(ConcreteName("Implicit", "this", 4), ScopeVar("s3")),
//      CGDirectEdge(ScopeVar("s3"),Label('P'), ScopeVar("s2")),
//      CGDirectEdge(ScopeVar("s2"),Label('P'), ScopeVar("s1")),
//      CGDecl(ScopeVar("s1"), SymbolicName("Class", "n1")),
//      CGDecl(ScopeVar("s2"), SymbolicName("Method", "n3")),
//      CGDecl(ScopeVar("s2"), ConcreteName("Implicit", "this", 2)),
//      CResolve(ConcreteName("Implicit", "this", 4), NameVar("d1"))
//    )
//
//    assert(Solver.solve(constraints).nonEmpty)
//  }
//
//  test("example with this") {
//    val constraints = List(
//      CEqual(TypeVar("t2033"),TypeAppl("Int", List())),
//      CEqual(TypeVar("t2013"),TypeAppl("Bool", List())),
//      CEqual(TypeVar("t2012"),TypeAppl("IntArray", List())),
//      CGDecl(ScopeVar("s2030"),SymbolicName("Variable", "n2009")),
//      CTypeOf(SymbolicName("Variable", "n2009"),TypeVar("t2012")),
//      CEqual(TypeVar("t1999"),TypeAppl("Int", List())),
//      CEqual(TypeVar("t1995"),TypeAppl("Nil", List())),
//      CGDecl(ScopeVar("s2030"),SymbolicName("Method", "n1961")),
//      CGDirectEdge(ScopeVar("s2028"),Label('P'),ScopeVar("s2030")), CGAssoc(SymbolicName("Method", "n1961"),ScopeVar("s2028")),
//      CTypeOf(SymbolicName("Method", "n1961"),TypeAppl("Pair", List(TypeVar("t1995"), TypeVar("t2013")))),
//      CGDecl(ScopeVar("s2032"),SymbolicName("Class", "n1951")),
//      CTypeOf(SymbolicName("Class", "n1951"),TypeAppl("ClassType", List(TypeNameAdapter(SymbolicName("Class", "n1951"))))),
//      CGDirectEdge(ScopeVar("s2030"),Label('P'),ScopeVar("s2032")),
//      CGDecl(ScopeVar("s2030"),ConcreteName("Implicit", "this", 1)),
//      CTypeOf(ConcreteName("Implicit", "this", 1),TypeAppl("ClassType", List(TypeNameAdapter(SymbolicName("Class", "n1951"))))),
//      CGAssoc(NameVar("n1951"),ScopeVar("s2030"))
//    )
//
//    assert(Solver.solve(constraints).nonEmpty)
//  }
//
//  test("matching concrete names resolve") {
//    val constraints = List(
//      CGDecl(ScopeVar("s"), ConcreteName("Implicit", "bar", 1)),
//      CGRef(ConcreteName("Implicit", "bar", 2), ScopeVar("s")),
//      CResolve(ConcreteName("Implicit", "bar", 2), NameVar("d659"))
//    )
//
//    assert(Solver.solve(constraints).nonEmpty)
//  }
//
//  test("distinct concrete names do not resolve") {
//    val constraints = List(
//      CGDecl(ScopeVar("s"), ConcreteName("Implicit", "foo", 1)),
//      CGRef(ConcreteName("Implicit", "bar", 2), ScopeVar("s")),
//      CResolve(ConcreteName("Implicit", "bar", 2), NameVar("d659"))
//    )
//
//    assert(Solver.solve(constraints).isEmpty)
//  }
//
//  test("does it work?") {
//    val constraints = List(
//      CGDirectEdge(ScopeVar("s714996"),Label('P'),ScopeVar("s714990")),
//      CGDirectEdge(ScopeVar("s714990"),Label('P'),ScopeVar("s715011")),
//      CGDirectEdge(ScopeVar("s715004"),Label('P'),ScopeVar("s715011")),
//
//      CGDecl(ScopeVar("s714990"),SymbolicName("Method", "n714947")),
//      CGDecl(ScopeVar("s715011"),SymbolicName("Class", "n714940")),
//      CGDecl(ScopeVar("s714990"),ConcreteName("Implicit", "this", 714962)),
//      CGDecl(ScopeVar("s715011"),SymbolicName("Class", "n714931")),
//      CGDecl(ScopeVar("s715004"),ConcreteName("Implicit", "this", 714963)),
//
//      CGRef(ConcreteName("Implicit", "this", 714974),ScopeVar("s714996")),
//      CGRef(SymbolicName("Class", "n714946"),ScopeVar("s714990")),
//
//      CResolve(ConcreteName("Implicit", "this", 714974),NameVar("d714975")),
//      CResolve(SymbolicName("Class", "n714946"),NameVar("d714961")),
//
//      CTypeOf(NameVar("d714975"),TypeVar("t714972")),
//      CTypeOf(NameVar("d714961"),TypeVar("t714972")),
//      CTypeOf(SymbolicName("Method", "n714947"),TypeAppl("Pair", List(TypeAppl("Nil", List()), TypeVar("t714972")))),
//      CTypeOf(SymbolicName("Class", "n714940"),TypeAppl("ClassType", List(TypeNameAdapter(SymbolicName("Class", "n714940"))))),
//      CTypeOf(ConcreteName("Implicit", "this", 714962),TypeAppl("ClassType", List(TypeNameAdapter(SymbolicName("Class", "n714940"))))),
//      CTypeOf(SymbolicName("Class", "n714931"),TypeAppl("ClassType", List(TypeNameAdapter(SymbolicName("Class", "n714931"))))),
//      CTypeOf(ConcreteName("Implicit", "this", 714963),TypeAppl("ClassType", List(TypeNameAdapter(SymbolicName("Class", "n714931"))))),
//
//      CGAssoc(SymbolicName("Method", "n714947"),ScopeVar("s714996")),
//      CGAssoc(NameVar("n714940"),ScopeVar("s714990")),
//      CGAssoc(NameVar("n714931"),ScopeVar("s715004"))
//    )
//
//    assert(Solver.solve(constraints).nonEmpty)
//  }
//
//  test("chained associated import") {
//    val constraints = List(
//      CGDirectEdge(ScopeVar("s1"),Label('P'), ScopeVar("s")),
//      CGDirectEdge(ScopeVar("s2"),Label('P'), ScopeVar("s")),
//      CGDirectEdge(ScopeVar("s3"),Label('P'), ScopeVar("s")),
//      CGDecl(ScopeVar("s"), SymbolicName("C", "n1")),
//      CGDecl(ScopeVar("s"), SymbolicName("C", "n2")),
//      CGDecl(ScopeVar("s"), SymbolicName("C", "n3")),
//      CGAssoc(SymbolicName("C", "n1"), ScopeVar("s1")),
//      CGAssoc(SymbolicName("C", "n2"), ScopeVar("s2")),
//      CGAssoc(SymbolicName("C", "n3"), ScopeVar("s3")),
//      CGRef(SymbolicName("C", "n4"), ScopeVar("s")),
//      CGRef(SymbolicName("C", "n5"), ScopeVar("s")),
//      CGNamedEdge(ScopeVar("s1"), Label('P'), SymbolicName("C", "n4")),
//      CGNamedEdge(ScopeVar("s3"), Label('P'), SymbolicName("C", "n5")),
//      CResolve(SymbolicName("C", "n4"), NameVar("d1")),
//      CResolve(SymbolicName("C", "n5"), NameVar("d2"))
//    )
//
//    // NOTE: The solver gives a random solution, so this test will fail in the future.
//
//    assert(Solver.solve(constraints) == Some((
//      // Type binding
//      Map(),
//      // Name binding
//      Map(NameVar("d2") -> SymbolicName("C", "n2"), NameVar("d1") -> SymbolicName("C", "n3")),
//      // Conditions
//      List(
//        Eq(SymbolicName("C", "n5"),SymbolicName("C", "n2")),
//        Eq(SymbolicName("C", "n4"),SymbolicName("C", "n3"))
//      )
//    )))
//  }
//
//  test("associated import with symbolic names") {
//    val constraints = List(
//      CGDirectEdge(ScopeVar("s1"),Label('P'), ScopeVar("s")),
//      CGDirectEdge(ScopeVar("s2"),Label('P'), ScopeVar("s")),
//      CGDirectEdge(ScopeVar("s"),Label('P'), ScopeVar("s3")),
//      CGDecl(ScopeVar("s3"), SymbolicName("C", "n6")),
//      CGDecl(ScopeVar("s2"), SymbolicName("V", "n3")),
//      CGDecl(ScopeVar("s"), SymbolicName("C", "n1")),
//      CGDecl(ScopeVar("s"), SymbolicName("C", "n2")),
//      CGAssoc(SymbolicName("C", "n1"), ScopeVar("s1")),
//      CGAssoc(SymbolicName("C", "n2"), ScopeVar("s2")),
//      CGNamedEdge(ScopeVar("s1"), Label('P'), SymbolicName("C", "n4")),
//      CGRef(SymbolicName("C", "n4"), ScopeVar("s")),
//      CResolve(SymbolicName("C", "n4"), NameVar("d1")),
//      CGRef(SymbolicName("V", "n5"), ScopeVar("s1")),
//      CResolve(SymbolicName("V", "n5"), NameVar("d2"))
//    )
//
//    for (i <- 0 to 20) {
//      println(Solver.solve(constraints))
//    }
//  }
//
//  test("solve trivial TypeEquals constraint") {
//    val constraints = List(
//      CEqual(TypeAppl("Int"), TypeAppl("Bool"))
//    )
//
//    assert(Solver.solve(constraints).isEmpty)
//  }
//
//  test("solve trivial subtyping constraint") {
//    val constraints = List(
//      CSubtype(TypeAppl("TInt"), TypeAppl("TInt"))
//    )
//
//    assert(Solver.solve(constraints).nonEmpty)
//  }
//
//  test("rule that leads to a direct edge to the same scope (after resolving the reference to the only declaration) and hence a stackoverflow exception") {
//    val r = Rule(SortAppl("Declaration", List()), None, List(ScopeVar("s")), State(TermAppl("Class", List(TermVar("x"), TermAppl("Parent", List(TermVar("x_parent"))), TermAppl("Nil", List()))),List(CTypeOf(SymbolicName("Class", "x"),TypeAppl("TClassDef", List(TypeNameAdapter(SymbolicName("Class", "x"))))), CResolve(SymbolicName("Class", "x_parent"),NameVar("d_parent")), CAssoc(NameVar("d_parent"),ScopeVar("s''")), CTypeOf(NameVar("d_parent"),TypeAppl("TClassDef", List(TypeVar("d_parent")))), FSubtype(TypeAppl("TClass", List(TypeVar("x"))),TypeAppl("TClass", List(TypeVar("d_parent")))), CTrue()),List(CGDecl(ScopeVar("s"),SymbolicName("Class", "x")), CGAssoc(SymbolicName("Class", "x"),ScopeVar("s201")), CGRef(SymbolicName("Class", "x_parent"),ScopeVar("s")), CGDirectEdge(ScopeVar("s201"),Label('P'),ScopeVar("s''")), CGDirectEdge(ScopeVar("s201"),Label('P'),ScopeVar("s"))),TypeEnv(),Resolution(),SubtypeRelation(List()),List()))
//
//    assert(Solver.solveAny(r.state).nonEmpty)
//  }

  test("xxx") {
    // TODO: Can we use varargs here to omit the list?
    // i.e. val t1 = TermAppl("TInt", TermAppl("TInt", TermAppl("TInt", Nil)))

    val t1 = TermAppl("TInt", List(TermAppl("TInt", List(TermAppl("TInt", Nil)))))
    val t2 = TermAppl("TInt", Nil)

    val c = CSubtype(t1, t2)
    val s = State(Nil)

    assert(Solver.rewrite(c, s)(null).isEmpty)
  }

  test("xxy") {
    // TODO: Mock the parts of language that are necessary for the test? See ResolutionSuite for alternative solution.
    val language = Language.load("/Users/martijn/Projects/MiniJava", "org.metaborg:MiniJava:0.1.0-SNAPSHOT", "MiniJava")

    val s = State(
      pattern = Var("x"),
      constraints = List(
        CTypeOf(ConcreteName("M", "m", 3), TermAppl("TMethod", List(TermAppl("TInt"), TermAppl("TInt")))),
        CResolve(ConcreteName("C", "Foo", 1), Var("d1")),
        CAssoc(Var("d1"), ScopeVar("sigma")),
        CResolve(ConcreteName("M", "m", 4), Var("d2")),
        CTypeOf(Var("d2"), TermAppl("TMethod", List(Var("rty"), Var("tf")))),
        CSubtype(TermAppl("TInt", List(TermAppl("TInt", List(TermAppl("TInt", Nil))))), Var("tf"))
      ),
      facts = List(
        CGRef(ConcreteName("C", "Foo", 1), ScopeAppl("s")),
        CGDecl(ScopeAppl("s"), ConcreteName("C", "Foo", 2)),
        CGDirectEdge(ScopeAppl("cs"), Label('P'), ScopeAppl("s")),
        CGAssoc(ConcreteName("C", "Foo", 2), ScopeAppl("cs")),
        CGDecl(ScopeAppl("cs"), ConcreteName("M", "m", 3)),
        CGDirectEdge(ScopeAppl("ms"), Label('P'), ScopeAppl("cs")),
        CGRef(ConcreteName("M", "m", 4), ScopeAppl("s'")),
        CGDirectEdge(ScopeAppl("s'"), Label('I'), ScopeVar("sigma"))
      ),
      typeEnv = TypeEnv(),
      resolution = Resolution(),
      subtypeRelation = SubtypeRelation()
    )

    assert(Solver.solve(s)(language).isEmpty)
  }
}
