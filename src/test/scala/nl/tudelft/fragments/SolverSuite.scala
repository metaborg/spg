package nl.tudelft.fragments

import nl.tudelft.fragments.spoofax.Language
import org.scalatest.FunSuite

class SolverSuite extends FunSuite {

//  test("single declaration") {
//    val constraints = List(
//      CGDirectEdge(Var("s1"),Label('P'),Var("s2")),
//      CGDecl(Var("s1"),SymbolicName("", "n3")),
//      CGRef(SymbolicName("", "n1"),Var("s1")),
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
//      CGDirectEdge(Var("s2"),Label('P'), Var("s1")),
//      CGDecl(Var("s1"), NameVar("n1")),
//      CGAssoc(NameVar("n1"), Var("s2")),
//      CGRef(NameVar("n2"), Var("s3")),
//      CGDirectEdge(Var("s3"), Label('I'), Var("s4")),
//      CEqual(TypeAppl("ClassType", List(TypeNameAdapter(NameVar("n4")))), TypeAppl("ClassType", List(TypeNameAdapter(NameVar("n1"))))),
//      CAssoc(NameVar("n4"), Var("s4")),
//      CResolve(NameVar("n2"), NameVar("n5"))
//    )
//
//    assert(Solver.solve(constraints).isEmpty)
//  }
//
//  test("incomplete but still consistent") {
//    val constraints = List(
//      CGDecl(Var("s1"),NameVar("n1")),
//      CGDirectEdge(Var("s2"),Label('P'),Var("s1")),
//      CGRef(NameVar("n2"),Var("s3")),
//      CResolve(NameVar("n2"),NameVar("n3")),
//      CGAssoc(NameVar("n1"),Var("s2")),
//      CGDirectEdge(Var("s3"),Label('I'),Var("s4")),
//      CAssoc(NameVar("n4"),Var("s4")),
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
//      CGRef(NameVar("n1"),Var("s1")),
//      CResolve(NameVar("n1"),NameVar("n4")),
//      CResolve(NameVar("n2"),NameVar("n4")),
//      CAssoc(NameVar("n4"),Var("s4")),
//      CGRef(NameVar("n2"),Var("s2")),
//      CGDirectEdge(Var("s2"),Label('I'),Var("s4")),
//      CGDecl(Var("s3"),NameVar("n3")),
//      CGDirectEdge(Var("s1"),Label('P'),Var("s3")),
//      CGAssoc(NameVar("n3"),Var("s1"))
//    )
//
//    assert(Solver.solve(constraints).isEmpty)
//  }
//
//  test("concrete names") {
//    val constraints = List(
//      CGRef(ConcreteName("Implicit", "this", 4), Var("s3")),
//      CGDirectEdge(Var("s3"),Label('P'), Var("s2")),
//      CGDirectEdge(Var("s2"),Label('P'), Var("s1")),
//      CGDecl(Var("s1"), SymbolicName("Class", "n1")),
//      CGDecl(Var("s2"), SymbolicName("Method", "n3")),
//      CGDecl(Var("s2"), ConcreteName("Implicit", "this", 2)),
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
//      CGDecl(Var("s2030"),SymbolicName("Variable", "n2009")),
//      CTypeOf(SymbolicName("Variable", "n2009"),TypeVar("t2012")),
//      CEqual(TypeVar("t1999"),TypeAppl("Int", List())),
//      CEqual(TypeVar("t1995"),TypeAppl("Nil", List())),
//      CGDecl(Var("s2030"),SymbolicName("Method", "n1961")),
//      CGDirectEdge(Var("s2028"),Label('P'),Var("s2030")), CGAssoc(SymbolicName("Method", "n1961"),Var("s2028")),
//      CTypeOf(SymbolicName("Method", "n1961"),TypeAppl("Pair", List(TypeVar("t1995"), TypeVar("t2013")))),
//      CGDecl(Var("s2032"),SymbolicName("Class", "n1951")),
//      CTypeOf(SymbolicName("Class", "n1951"),TypeAppl("ClassType", List(TypeNameAdapter(SymbolicName("Class", "n1951"))))),
//      CGDirectEdge(Var("s2030"),Label('P'),Var("s2032")),
//      CGDecl(Var("s2030"),ConcreteName("Implicit", "this", 1)),
//      CTypeOf(ConcreteName("Implicit", "this", 1),TypeAppl("ClassType", List(TypeNameAdapter(SymbolicName("Class", "n1951"))))),
//      CGAssoc(NameVar("n1951"),Var("s2030"))
//    )
//
//    assert(Solver.solve(constraints).nonEmpty)
//  }
//
//  test("matching concrete names resolve") {
//    val constraints = List(
//      CGDecl(Var("s"), ConcreteName("Implicit", "bar", 1)),
//      CGRef(ConcreteName("Implicit", "bar", 2), Var("s")),
//      CResolve(ConcreteName("Implicit", "bar", 2), NameVar("d659"))
//    )
//
//    assert(Solver.solve(constraints).nonEmpty)
//  }
//
//  test("distinct concrete names do not resolve") {
//    val constraints = List(
//      CGDecl(Var("s"), ConcreteName("Implicit", "foo", 1)),
//      CGRef(ConcreteName("Implicit", "bar", 2), Var("s")),
//      CResolve(ConcreteName("Implicit", "bar", 2), NameVar("d659"))
//    )
//
//    assert(Solver.solve(constraints).isEmpty)
//  }
//
//  test("does it work?") {
//    val constraints = List(
//      CGDirectEdge(Var("s714996"),Label('P'),Var("s714990")),
//      CGDirectEdge(Var("s714990"),Label('P'),Var("s715011")),
//      CGDirectEdge(Var("s715004"),Label('P'),Var("s715011")),
//
//      CGDecl(Var("s714990"),SymbolicName("Method", "n714947")),
//      CGDecl(Var("s715011"),SymbolicName("Class", "n714940")),
//      CGDecl(Var("s714990"),ConcreteName("Implicit", "this", 714962)),
//      CGDecl(Var("s715011"),SymbolicName("Class", "n714931")),
//      CGDecl(Var("s715004"),ConcreteName("Implicit", "this", 714963)),
//
//      CGRef(ConcreteName("Implicit", "this", 714974),Var("s714996")),
//      CGRef(SymbolicName("Class", "n714946"),Var("s714990")),
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
//      CGAssoc(SymbolicName("Method", "n714947"),Var("s714996")),
//      CGAssoc(NameVar("n714940"),Var("s714990")),
//      CGAssoc(NameVar("n714931"),Var("s715004"))
//    )
//
//    assert(Solver.solve(constraints).nonEmpty)
//  }
//
//  test("chained associated import") {
//    val constraints = List(
//      CGDirectEdge(Var("s1"),Label('P'), Var("s")),
//      CGDirectEdge(Var("s2"),Label('P'), Var("s")),
//      CGDirectEdge(Var("s3"),Label('P'), Var("s")),
//      CGDecl(Var("s"), SymbolicName("C", "n1")),
//      CGDecl(Var("s"), SymbolicName("C", "n2")),
//      CGDecl(Var("s"), SymbolicName("C", "n3")),
//      CGAssoc(SymbolicName("C", "n1"), Var("s1")),
//      CGAssoc(SymbolicName("C", "n2"), Var("s2")),
//      CGAssoc(SymbolicName("C", "n3"), Var("s3")),
//      CGRef(SymbolicName("C", "n4"), Var("s")),
//      CGRef(SymbolicName("C", "n5"), Var("s")),
//      CGNamedEdge(Var("s1"), Label('P'), SymbolicName("C", "n4")),
//      CGNamedEdge(Var("s3"), Label('P'), SymbolicName("C", "n5")),
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
//      CGDirectEdge(Var("s1"),Label('P'), Var("s")),
//      CGDirectEdge(Var("s2"),Label('P'), Var("s")),
//      CGDirectEdge(Var("s"),Label('P'), Var("s3")),
//      CGDecl(Var("s3"), SymbolicName("C", "n6")),
//      CGDecl(Var("s2"), SymbolicName("V", "n3")),
//      CGDecl(Var("s"), SymbolicName("C", "n1")),
//      CGDecl(Var("s"), SymbolicName("C", "n2")),
//      CGAssoc(SymbolicName("C", "n1"), Var("s1")),
//      CGAssoc(SymbolicName("C", "n2"), Var("s2")),
//      CGNamedEdge(Var("s1"), Label('P'), SymbolicName("C", "n4")),
//      CGRef(SymbolicName("C", "n4"), Var("s")),
//      CResolve(SymbolicName("C", "n4"), NameVar("d1")),
//      CGRef(SymbolicName("V", "n5"), Var("s1")),
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
//    val r = Rule(SortAppl("Declaration", List()), None, List(Var("s")), State(TermAppl("Class", List(TermVar("x"), TermAppl("Parent", List(TermVar("x_parent"))), TermAppl("Nil", List()))),List(CTypeOf(SymbolicName("Class", "x"),TypeAppl("TClassDef", List(TypeNameAdapter(SymbolicName("Class", "x"))))), CResolve(SymbolicName("Class", "x_parent"),NameVar("d_parent")), CAssoc(NameVar("d_parent"),Var("s''")), CTypeOf(NameVar("d_parent"),TypeAppl("TClassDef", List(TypeVar("d_parent")))), FSubtype(TypeAppl("TClass", List(TypeVar("x"))),TypeAppl("TClass", List(TypeVar("d_parent")))), CTrue()),List(CGDecl(Var("s"),SymbolicName("Class", "x")), CGAssoc(SymbolicName("Class", "x"),Var("s201")), CGRef(SymbolicName("Class", "x_parent"),Var("s")), CGDirectEdge(Var("s201"),Label('P'),Var("s''")), CGDirectEdge(Var("s201"),Label('P'),Var("s"))),TypeEnv(),Resolution(),SubtypeRelation(List()),List()))
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

    assert(Solver.rewrite(c, s, true)(null).isEmpty)
  }

  test("xxy") {
    // TODO: Mock the parts of language that are necessary for the test? See ResolutionSuite for alternative solution.
    val language = Language.load("/Users/martijn/Projects/MiniJava", "trans/static-semantics.nabl2")

    val s = State(
      pattern = Var("x"),
      constraints = List(
        // Proper constraints
        CTypeOf(ConcreteName("M", "m", 3), TermAppl("TMethod", List(TermAppl("TInt"), TermAppl("TInt")))),
        CResolve(ConcreteName("C", "Foo", 1), Var("d1")),
        CAssoc(Var("d1"), Var("sigma")),
        CResolve(ConcreteName("M", "m", 4), Var("d2")),
        CTypeOf(Var("d2"), TermAppl("TMethod", List(Var("rty"), Var("tf")))),
        CSubtype(TermAppl("TInt", List(TermAppl("TInt", List(TermAppl("TInt", Nil))))), Var("tf")),

        // Facts
        CGRef(ConcreteName("C", "Foo", 1), TermAppl("s")),
        CGDecl(TermAppl("s"), ConcreteName("C", "Foo", 2)),
        CGDirectEdge(TermAppl("cs"), Label('P'), TermAppl("s")),
        CGAssoc(ConcreteName("C", "Foo", 2), TermAppl("cs")),
        CGDecl(TermAppl("cs"), ConcreteName("M", "m", 3)),
        CGDirectEdge(TermAppl("ms"), Label('P'), TermAppl("cs")),
        CGRef(ConcreteName("M", "m", 4), TermAppl("s'")),
        CGDirectEdge(TermAppl("s'"), Label('I'), Var("sigma"))
      ),
      typeEnv = TypeEnv(),
      resolution = Resolution(),
      subtypeRelation = SubtypeRelation(),
      inequalities = Nil
    )

    assert(Solver.solve(s)(language).isEmpty)
  }
}
