package org.metaborg.spg

import org.metaborg.spg.LabelImplicits._
import org.metaborg.spg.regex._
import org.metaborg.spg.spoofax.{Language, ResolutionParams, Specification}
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
  implicit val language: Language = new Language(Nil, null, new Specification(resolutionParams, null, Nil), null, Set(), null)

  test("resolution") {
    val facts = List(
      CGRef(SymbolicName("Var", "x"), Var("s1")),
      CGDecl(Var("s1"), SymbolicName("Var", "y")),
      CGDecl(Var("s1"), SymbolicName("Var", "z"))
    )

    val resolution = Resolution(Map(
      SymbolicName("Var", "x") -> SymbolicName("Var", "y")
    ))

    assert(Graph(facts).res(resolution)(SymbolicName("Var", "x")) == List(
      (SymbolicName("Var", "y"), Nil)
    ))
  }

  test("resolution with a direct edge") {
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
      (SymbolicName("Var", "y"), Nil)
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
      (SymbolicName("Class", "x"), List(Eq(SymbolicName("Class", "x836"), SymbolicName("Class", "x"))))
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

    val language = new Language(Nil, null, new Specification(resolutionParams, null, Nil), null, Set(), null)

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

    assert(Graph(facts)(language).res(Resolution())(ConcreteName("C", "Foo", 1)).length == 1)
  }
}
