package nl.tudelft.fragments

import nl.tudelft.fragments.LabelImplicits._
import nl.tudelft.fragments.spoofax.{Language, ResolutionParams, Specification}
import nl.tudelft.fragments.spoofax.models.{Signatures, SortAppl}
import org.scalatest.FunSuite

class CheckSuite extends FunSuite {
  implicit val langauge = new Language(Nil, Signatures(Nil), new Specification(new ResolutionParams(Nil, null, null), Nil), null, Nil)

  test("even Ps will succeed") {
    // Single rewrite
    val R = List(
      Rule(SortAppl("VarDecl"), None, List(ScopeAppl("s")), State(TermAppl("Var", List()), List(
        CGDirectEdge(ScopeAppl("s"), Label('P'), ScopeAppl("s2")),
        CGDecl(ScopeAppl("s2"), SymbolicName("Class", "x"))
      )))
    )

    // Any number of Ps: P, PP, PPP, ...
    val c = Label('P') *

    // Initial graph
    val r = Rule(SortAppl("Program"), None, List(ScopeAppl("s")), State(TermAppl("Program"), List(
      CGenRecurse(TermVar("x"), List(ScopeAppl("s")), None, SortAppl("VarDecl"))
    )))

    // Scope for which the continuation applies
    val s = ScopeAppl("s")

    assert(Check.declarationability(R, r, s, "Class", c))
  }

  test("uneven Ps will terminate and fail") {
    // Single rewrite
    val R = List(
      Rule(SortAppl("VarDecl"), None, List(ScopeAppl("s")), State(TermAppl("Var", List()), List(
        CGDirectEdge(ScopeAppl("s"), Label('P'), ScopeAppl("s2")),
        CGDirectEdge(ScopeAppl("s2"), Label('P'), ScopeAppl("s3")),
        CGDecl(ScopeAppl("s3"), SymbolicName("Class", "x"))
      )))
    )

    // Uneven number of Ps: P, PPP, PPPPP, ...
    val c = Label('P') ~ ((Label('P') ~ Label('P')) *)

    // Initial graph
    val r = Rule(SortAppl("Program"), None, Nil, State(TermAppl("Program"), List(
      CGenRecurse(TermVar("x"), List(ScopeAppl("s")), None, SortAppl("VarDecl"))
    )))

    // Scope for which the continuation applies
    val s = ScopeAppl("s")

    assert(!Check.declarationability(R, r, s, "Class", c))
  }

  test("sorted rewrites succeeds") {
    // Single rewrite
    val R = List(
      // Class : Field -> ClassDecl
      Rule(SortAppl("ClassDecl"), None, List(ScopeAppl("s")), State(TermAppl("Class", List()), List(
        CGDirectEdge(ScopeAppl("s"), Label('P'), ScopeAppl("s2")),
        CGenRecurse(TermVar("x"), List(ScopeAppl("s2")), None, SortAppl("FieldDecl"))
      ))),

      // Field : String -> FieldDecl
      Rule(SortAppl("FieldDecl"), None, List(ScopeAppl("s")), State(TermAppl("Field", List()), List(
        CGDecl(ScopeAppl("s"), SymbolicName("Class", "x"))
      )))
    )

    // Any number of Ps: P, PP, PPP, ...
    val c = Label('P')*

    // Initial graph
    val r = Rule(SortAppl("Program"), None, Nil, State(TermAppl("Program"), List(
      CGenRecurse(TermVar("x"), List(ScopeAppl("s")), None, SortAppl("ClassDecl"))
    )))

    // Scope for which the continuation applies
    val s = ScopeAppl("s")

    assert(Check.declarationability(R, r, s, "Class", c))
  }

  test("continuation from different sorts are different") {
    // Single rewrite
    val R = List(
      // Class : Field -> ClassDecl
      Rule(SortAppl("ClassDecl"), None, List(ScopeAppl("s")), State(TermAppl("Class", List()), List(
        CGDirectEdge(ScopeAppl("s"), Label('P'), ScopeAppl("s2")),
        CGenRecurse(TermVar("x"), List(ScopeAppl("s2")), None, SortAppl("FieldDecl"))
      ))),

      // Field : String -> FieldDecl
      Rule(SortAppl("FieldDecl"), None, List(ScopeAppl("s")), State(TermAppl("Field", List()), List(
        CGDirectEdge(ScopeAppl("s"), Label('P'), ScopeAppl("s2")),
        CGenRecurse(TermVar("x"), List(ScopeAppl("s2")), None, SortAppl("Exp"))
      ))),

      // Add : Exp
      Rule(SortAppl("Exp"), None, List(ScopeAppl("s")), State(TermAppl("Add", List()), List(
        CGDirectEdge(ScopeAppl("s"), Label('P'), ScopeAppl("s2")),
        CGDecl(ScopeAppl("s"), SymbolicName("Class", "x"))
      )))
    )

    // Any number of Ps: P, PP, PPP, ...
    val c = Label('P')*

    // Initial graph
    val r = Rule(SortAppl("Program"), None, Nil, State(TermAppl("Program"), List(
      CGenRecurse(TermVar("x"), List(ScopeAppl("s")), None, SortAppl("ClassDecl"))
    )))

    // Scope for which the continuation applies
    val s = ScopeAppl("s")

    assert(Check.declarationability(R, r, s, "Class", c))
  }
}
