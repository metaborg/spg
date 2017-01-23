package org.metaborg.spg

import org.scalatest.FunSuite

class PatternSuite extends FunSuite {
  test("application contains variable") {
    val p1 = TermAppl("X", List(Var("x")))
    val p2 = Var("x")

    assert(p1.contains(p2))
  }

  test("unification terminates (needs occurs check)") {
    val p1 = TermAppl("X", List(Var("x")))
    val p2 = Var("x")

    assert(p1.unify(p2).isEmpty)
  }
}
