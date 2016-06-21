package nl.tudelft.fragments

import nl.tudelft.fragments.spoofax.Signatures._
import org.scalatest.FunSuite

class SignaturesSuite extends FunSuite {
  implicit val signatures = List(
    OpDecl("Fun",FunType(List(ConstType(SortAppl("ID")), ConstType(SortAppl("Type")), ConstType(SortAppl("Exp"))),ConstType(SortAppl("Exp")))),
    OpDecl("App",FunType(List(ConstType(SortAppl("Exp")), ConstType(SortAppl("ResetExp"))),ConstType(SortAppl("Exp")))),
    OpDeclInj(FunType(List(ConstType(SortAppl("Exp"))),ConstType(SortAppl("ResetExp")))),
    OpDeclInj(FunType(List(ConstType(SortAppl("Foo"))),ConstType(SortAppl("Exp"))))
  )

  test("get injections") {
    val inj = injections(SortAppl("ResetExp"))

    assert(inj == Set(SortAppl("Exp")))
  }

  test("get injections closure") {
    val inj = injectionsClosure(Set(SortAppl("ResetExp")))

    assert(inj == Set(SortAppl("ResetExp"), SortAppl("Exp"), SortAppl("Foo")))
  }
}
