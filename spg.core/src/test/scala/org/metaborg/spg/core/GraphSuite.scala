package org.metaborg.spg.core

import org.metaborg.spg.core.regex._
import org.metaborg.spg.core.resolution.LabelImplicits._
import org.metaborg.spg.core.resolution.{Graph, Label, LabelOrdering}
import org.metaborg.spg.core.solver._
import org.metaborg.spg.core.spoofax.models.SortAppl
import org.metaborg.spg.core.spoofax.{Language, ResolutionParams, Specification}
import org.scalatest.FunSuite

class GraphSuite extends FunSuite {
  val resolutionParams = new ResolutionParams(
    labels = List(
      Label('P'),
      Label('I')
    ),
    order = LabelOrdering(
      (Label('D'), Label('P')),
      (Label('D'), Label('I')),
      (Label('I'), Label('P'))
    ),
    wf = (Label('P') *) ~ (Label('I') *)
  )

  // Define an implicit language with resolution params to do resolution on
  implicit val language: Language = new Language(Nil, null, Specification(resolutionParams, Nil), null, Set(), null)

  test("resolve reference in context of existing resolution") {
    val facts = List(
      CGRef(SymbolicName("Var", "x"), Var("s1")),
      CGDecl(Var("s1"), SymbolicName("Var", "y")),
      CGDecl(Var("s1"), SymbolicName("Var", "z"))
    )

    val resolution = Resolution(Map(
      SymbolicName("Var", "x") -> SymbolicName("Var", "y")
    ))

    assert(Graph(facts).res(resolution)(SymbolicName("Var", "x")) == List(
      SymbolicName("Var", "y")
    ))
  }

  test("resolve reference in context of existing resolution with parent edge") {
    val facts = List(
      CGRef(SymbolicName("Var", "x"), Var("s1")),
      CGDirectEdge(Var("s1"), Label('P'), Var("s2")),
      CGDecl(Var("s2"), SymbolicName("Var", "y")),
      CGDecl(Var("s2"), SymbolicName("Var", "z"))
    )

    val resolution = Resolution(Map(
      SymbolicName("Var", "x") -> SymbolicName("Var", "y")
    ))

    assert(Graph(facts).res(resolution)(SymbolicName("Var", "x")) == List(
      SymbolicName("Var", "y")
    ))
  }

  test("complex resolution") {
    val facts = List(
      CGDecl(Var("s"), SymbolicName("Class", "x")),
      CGAssoc(SymbolicName("Class", "x"), Var("s1705")),
      CGDirectEdge(Var("s1705"), Label('P'), Var("s")),
      CGDecl(Var("s1705"), SymbolicName("Var", "x835")),
      CGRef(SymbolicName("Class", "x836"), Var("s1705")),
      CGDecl(Var("s1705"), SymbolicName("Var", "x1706"))
    )

    val resolution = Resolution()

    assert(Graph(facts).res(resolution)(SymbolicName("Class", "x836")) == List(
      SymbolicName("Class", "x")
    ))
  }

  test("reachable scopes with parent edge") {
    val facts = List(
      CGDirectEdge(Var("s1"), Label('P'), Var("s2"))
    )

    assert(Graph(facts).reachableScopes((Label('P') *) ~ (Label('I') *), Nil, Nil, Resolution())(Var("s1")) == List(
      Var("s1"),
      Var("s2")
    ))
  }

  test("reachable scopes with multiple parent edges") {
    val facts = List(
      CGDirectEdge(Var("s1"), Label('P'), Var("s2")),
      CGDirectEdge(Var("s2"), Label('P'), Var("s3"))
    )

    assert(Graph(facts).reachableScopes((Label('P') *) ~ (Label('I') *), Nil, Nil, Resolution())(Var("s1")) == List(
      Var("s1"),
      Var("s2"),
      Var("s3")
    ))
  }

  test("reachable scopes with named edge without resolution") {
    val facts = List(
      CGRef(SymbolicName("Class", "parent"), Var("s1")),
      CGDecl(Var("s1"), SymbolicName("Class", "x")),
      CGAssoc(SymbolicName("Class", "x"), Var("s2")),
      CGNamedEdge(Var("s1"), Label('I'), SymbolicName("Class", "parent"))
    )

    assert(Graph(facts).reachableScopes((Label('P') *) ~ (Label('I') *), Nil, Nil, Resolution())(Var("s1")) == List(
      Var("s1")
    ))
  }

  test("reachable scopes with named edge with resolution") {
    val facts = List(
      CGRef(SymbolicName("Class", "parent"), Var("s1")),
      CGDecl(Var("s1"), SymbolicName("Class", "x")),
      CGAssoc(SymbolicName("Class", "x"), Var("s2")),
      CGNamedEdge(Var("s1"), Label('I'), SymbolicName("Class", "parent"))
    )

    val resolution = Resolution(
      Map(SymbolicName("Class", "parent") -> SymbolicName("Class", "x"))
    )

    assert(Graph(facts).reachableScopes((Label('P') *) ~ (Label('I') *), Nil, Nil, resolution)(Var("s1")) == List(
      Var("s1"),
      Var("s2")
    ))
  }

  test("single declaration when multiple max labels") {
    val resolutionParams = new ResolutionParams(
      labels = List(
        Label('D'),
        Label('P'),
        Label('I'),
        Label('S')
      ),
      order = LabelOrdering(
        (Label('D'), Label('P')),
        (Label('D'), Label('I')),
        (Label('I'), Label('P')),
        (Label('S'), Label('I')),
        (Label('S'), Label('P')),
        (Label('D'), Label('S'))
      ),
      wf = (Label('P') *) ~ (Epsilon() || (Label('S') ~ Label('I'))) ~ (Label('I') *)
    )

    val language = new Language(Nil, null, new Specification(resolutionParams, Nil), null, Set(), null)

    val facts = List(
      CGRef(ConcreteName("C", "Foo", 1), TermAppl("s")),
      CGDecl(TermAppl("s"), ConcreteName("C", "Foo", 2)),
      CGDirectEdge(TermAppl("cs"), Label('P'), TermAppl("s")),
      CGAssoc(ConcreteName("C", "Foo", 2), TermAppl("cs")),
      CGDecl(TermAppl("cs"), ConcreteName("M", "m", 3)),
      CGDirectEdge(TermAppl("ms"), Label('P'), TermAppl("cs")),
      CGRef(ConcreteName("M", "m", 4), TermAppl("s'")),
      CGDirectEdge(TermAppl("s'"), Label('I'), Var("sigma"))
    )

    assert(Graph(facts)(language).res(Resolution())(ConcreteName("C", "Foo", 1)).size == 1)
  }

  test("foo cannot resolve to bar") {
    val facts = List(
      CGRef(ConcreteName("Class", "foo", 1), TermAppl("s")),
      CGDecl(TermAppl("s"), ConcreteName("Class", "bar", 2))
    )

    val result = Graph(facts).res(Resolution())(ConcreteName("Class", "foo", 1))

    assert(result.isEmpty)
  }

  test("existing resolution `a |-> this` excludes `this |-> b` as possibility") {
    val facts = List(
      CGRef(SymbolicName("Class", "a"), TermAppl("s")),
      CGRef(ConcreteName("Class", "this", 4), TermAppl("s")),
      CGDirectEdge(TermAppl("s"), Label('P'), TermAppl("s'")),
      CGDecl(TermAppl("s'"), SymbolicName("Class", "b")),
      CGDirectEdge(TermAppl("s'"), Label('P'), TermAppl("s''")),
      CGDecl(TermAppl("s''"), ConcreteName("Class", "this", 5))
    )

    val resolution = Resolution(Map(
      SymbolicName("Class", "a") -> ConcreteName("Class", "this", 5)
    ))

    val result = Graph(facts).res(resolution)(ConcreteName("Class", "this", 4))

    assert(result.size == 1)
    assert(result.head._2 == ConcreteName("Class", "this", 5))
  }

  test("example failed resolution") {
    val program = Program(TermAppl("Mod", List(TermAppl("Let", List(TermAppl("Cons", List(TermAppl("VarDec", List(Var("x125"), TermAppl("Tid", List(Var("x252"))), Var("x127"))), TermAppl("Nil", List()))), Var("x117"))))),List(CGDecl(TermAppl("s3090", List()),ConcreteName("Type", "int", -1)), CGDecl(TermAppl("s3090", List()),ConcreteName("Type", "string", -1)), CGDecl(TermAppl("s3090", List()),ConcreteName("Var", "print", -1)), CGDecl(TermAppl("s3090", List()),ConcreteName("Var", "flush", -1)), CGDecl(TermAppl("s3090", List()),ConcreteName("Var", "getchar", -1)), CGDecl(TermAppl("s3090", List()),ConcreteName("Var", "ord", -1)), CGDecl(TermAppl("s3090", List()),ConcreteName("Var", "chr", -1)), CGDecl(TermAppl("s3090", List()),ConcreteName("Var", "size", -1)), CGDecl(TermAppl("s3090", List()),ConcreteName("Var", "substring", -1)), CGDecl(TermAppl("s3090", List()),ConcreteName("Var", "concat", -1)), CGDecl(TermAppl("s3090", List()),ConcreteName("Var", "not", -1)), CGDecl(TermAppl("s3090", List()),ConcreteName("Var", "exit", -1)), CGenRecurse("Seq", Var("x117"), List(TermAppl("s115", List())), Some(Var("x119")), SortAppl("IterStar", List(SortAppl("Exp", List())))), CDistinct(Declarations(TermAppl("s115", List()), "All")), CGDirectEdge(TermAppl("s115", List()),Label('P'),TermAppl("s3090", List())), CGenRecurse("Default", Var("x127"), List(TermAppl("s3090", List())), Some(Var("x130")), SortAppl("Exp", List())), CSubtype(Var("x130"),Var("x254")), CGDecl(TermAppl("s115", List()),SymbolicName("Var", "x125")), CTypeOf(SymbolicName("Var", "x125"),Var("x254")), CGRef(SymbolicName("Type", "x252"),TermAppl("s3090", List())), CResolve(SymbolicName("Type", "x252"),Var("x255")), CTypeOf(Var("x255"),Var("x254"))),TypeEnv(Map(Binding(ConcreteName("Var", "concat", -1), TermAppl("FUN", List(TermAppl("Cons", List(TermAppl("STRING", List()), TermAppl("Cons", List(TermAppl("STRING", List()), TermAppl("Nil", List()))))), TermAppl("STRING", List())))), Binding(ConcreteName("Var", "size", -1), TermAppl("FUN", List(TermAppl("Cons", List(TermAppl("STRING", List()), TermAppl("Nil", List()))), TermAppl("INT", List())))), Binding(ConcreteName("Var", "print", -1), TermAppl("FUN", List(TermAppl("Cons", List(TermAppl("STRING", List()), TermAppl("Nil", List()))), TermAppl("UNIT", List())))), Binding(ConcreteName("Var", "ord", -1), TermAppl("FUN", List(TermAppl("Cons", List(TermAppl("STRING", List()), TermAppl("Nil", List()))), TermAppl("INT", List())))), Binding(ConcreteName("Var", "chr", -1), TermAppl("FUN", List(TermAppl("Cons", List(TermAppl("INT", List()), TermAppl("Nil", List()))), TermAppl("STRING", List())))), Binding(ConcreteName("Type", "string", -1), TermAppl("STRING", List())), Binding(ConcreteName("Var", "substring", -1), TermAppl("FUN", List(TermAppl("Cons", List(TermAppl("STRING", List()), TermAppl("Cons", List(TermAppl("INT", List()), TermAppl("Cons", List(TermAppl("INT", List()), TermAppl("Nil", List()))))))), TermAppl("STRING", List())))), Binding(ConcreteName("Var", "not", -1), TermAppl("FUN", List(TermAppl("Cons", List(TermAppl("INT", List()), TermAppl("Nil", List()))), TermAppl("INT", List())))), Binding(ConcreteName("Var", "exit", -1), TermAppl("FUN", List(TermAppl("Cons", List(TermAppl("INT", List()), TermAppl("Nil", List()))), TermAppl("UNIT", List())))), Binding(ConcreteName("Var", "getchar", -1), TermAppl("FUN", List(TermAppl("Nil", List()), TermAppl("STRING", List())))), Binding(ConcreteName("Var", "flush", -1), TermAppl("FUN", List(TermAppl("Nil", List()), TermAppl("UNIT", List())))), Binding(ConcreteName("Type", "int", -1), TermAppl("INT", List())))),Resolution(Map()),Subtypes(List()),List())

    assert(Graph(program.constraints).res(program.resolution)(program.properConstraints(5).asInstanceOf[CResolve].n1).nonEmpty)
  }
}
