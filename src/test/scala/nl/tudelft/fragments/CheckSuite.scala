package nl.tudelft.fragments

import nl.tudelft.fragments.LabelImplicits._
import nl.tudelft.fragments.spoofax.models.SortAppl
import org.scalatest.FunSuite

class CheckSuite extends FunSuite {
  test("even Ps will succeed") {
    // Single rewrite
    val R = List(
      Rewrite(SortAppl("VarDecl"), ScopeAppl("s"), List(
        CGDirectEdge(ScopeAppl("s"), Label('P'), ScopeAppl("s2")),
        CGDecl(ScopeAppl("s2"), SymbolicName("Class", "x"))
      ))
    )

    // Any number of Ps: P, PP, PPP, ...
    val c = Label('P') *

    // Initial graph
    val g = List(
      CGenRecurse(TermVar("x"), List(ScopeAppl("s")), None, SortAppl("VarDecl"))
    )

    // Scope for which the continuation applies
    val s = ScopeAppl("s")

    assert(Check.declarationability(R, g, s, c))
  }

  test("uneven Ps will terminate and fail") {
    // Single rewrite
    val R = List(
      Rewrite(SortAppl("VarDecl"), ScopeAppl("s"), List(
        CGDirectEdge(ScopeAppl("s"), Label('P'), ScopeAppl("s2")),
        CGDirectEdge(ScopeAppl("s2"), Label('P'), ScopeAppl("s3")),
        CGDecl(ScopeAppl("s3"), SymbolicName("Class", "x"))
      ))
    )

    // Uneven number of Ps: P, PPP, PPPPP, ...
    val c = Label('P') ~ ((Label('P') ~ Label('P')) *)

    // Initial graph
    val g = List(
      CGenRecurse(TermVar("x"), List(ScopeAppl("s")), None, SortAppl("VarDecl"))
    )

    // Scope for which the continuation applies
    val s = ScopeAppl("s")

    assert(!Check.declarationability(R, g, s, c))
  }


  test("sorted rewrites succeeds") {
    // Single rewrite
    val R = List(
      // Class : Field -> ClassDecl
      Rewrite(SortAppl("ClassDecl"), ScopeAppl("s"), List(
        CGDirectEdge(ScopeAppl("s"), Label('P'), ScopeAppl("s2")),
        CGenRecurse(TermVar("x"), List(ScopeAppl("s2")), None, SortAppl("FieldDecl"))
      )),

      // Field : String -> FieldDecl
      Rewrite(SortAppl("FieldDecl"), ScopeAppl("s"), List(
        CGDecl(ScopeAppl("s"), SymbolicName("Ns", "x"))
      ))
    )

    // Any number of Ps: P, PP, PPP, ...
    val c = Label('P')*

    // Initial graph
    val g = List(
      CGenRecurse(TermVar("x"), List(ScopeAppl("s")), None, SortAppl("ClassDecl"))
    )

    // Scope for which the continuation applies
    val s = ScopeAppl("s")

    assert(Check.declarationability(R, g, s, c))
  }

  test("continuation from different sorts are different") {
    // Single rewrite
    val R = List(
      // Class : Field -> ClassDecl
      Rewrite(SortAppl("ClassDecl"), ScopeAppl("s"), List(
        CGDirectEdge(ScopeAppl("s"), Label('P'), ScopeAppl("s2")),
        CGenRecurse(TermVar("x"), List(ScopeAppl("s2")), None, SortAppl("FieldDecl"))
      )),

      // Field : String -> FieldDecl
      Rewrite(SortAppl("FieldDecl"), ScopeAppl("s"), List(
        CGDirectEdge(ScopeAppl("s"), Label('P'), ScopeAppl("s2")),
        CGenRecurse(TermVar("x"), List(ScopeAppl("s2")), None, SortAppl("Exp"))
      )),

      // Exp : String -> Exp
      Rewrite(SortAppl("Exp"), ScopeAppl("s"), List(
        CGDirectEdge(ScopeAppl("s"), Label('P'), ScopeAppl("s2")),
        CGDecl(ScopeAppl("s"), SymbolicName("Ns", "x"))
      ))
    )

    // Any number of Ps: P, PP, PPP, ...
    val c = Label('P')*

    // Initial graph
    val g = List(
      CGenRecurse(TermVar("x"), List(ScopeAppl("s")), None, SortAppl("ClassDecl"))
    )

    // Scope for which the continuation applies
    val s = ScopeAppl("s")

    assert(Check.declarationability(R, g, s, c))
  }
}
