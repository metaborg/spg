package nl.tudelft.fragments

import nl.tudelft.fragments.regex.Character
import org.scalatest.FunSuite

class CheckSuite extends FunSuite {
  test("even Ps will succeed") {
    // Single rewrite
    val R = List(
      Rewrite(List(
        CGDirectEdge(ScopeAppl("s"), Label('P'), ScopeAppl("s2")),
        CGDecl(ScopeAppl("s2"), SymbolicName("Class", "x"))
      ))
    )

    // Any number of Ps: P, PP, PPP, ...
    val c = Character('P') *

    assert(Check.declarationability(R, c))
  }

  test("uneven Ps will terminate and fail") {
    // Single rewrite
    val R = List(
      Rewrite(List(
        CGDirectEdge(ScopeAppl("s"), Label('P'), ScopeAppl("s2")),
        CGDirectEdge(ScopeAppl("s2"), Label('P'), ScopeAppl("s3")),
        CGDecl(ScopeAppl("s3"), SymbolicName("Class", "x"))
      ))
    )

    // Uneven number of Ps: P, PPP, PPPPP, ...
    val c = Character('P') ~ ((Character('P') ~ Character('P')) *)

    assert(!Check.declarationability(R, c))
  }
}
