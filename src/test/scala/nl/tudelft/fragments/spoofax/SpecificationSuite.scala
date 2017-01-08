package nl.tudelft.fragments.spoofax

import nl.tudelft.fragments.TermAppl
import org.scalatest.FunSuite
import org.spoofax.interpreter.terms.{IStrategoAppl, IStrategoList, IStrategoTerm}
import org.spoofax.terms._

class SpecificationSuite extends FunSuite {
  test("read TList") {
    val list = buildAppl("TList", Array(
      buildList(
        buildAppl("Op", Array(
          buildString("STRING"),
          buildList()
        )),
        buildAppl("Op", Array(
          buildString("INT"),
          buildList()
        )),
        buildAppl("Op", Array(
          buildString("INT"),
          buildList()
        ))
      )
    ))

    val result = Specification.toType(0, list)

    assert(result == TermAppl("Cons", List(
      TermAppl("STRING", List()),
      TermAppl("Cons", List(
        TermAppl("INT", List()),
        TermAppl("Cons", List(
          TermAppl("INT", List()),
          TermAppl("Nil", List())
        ))
      ))
    )))
  }

  def buildAppl(cons: String, kids: Array[IStrategoTerm]): IStrategoAppl =
    new StrategoAppl(new StrategoConstructor(cons, kids.length), kids, null, 0)

  def buildList(terms: IStrategoTerm*): IStrategoList = terms toList match {
    case x :: xs =>
      new StrategoList(x, buildList(xs: _*), null, 0)
    case Nil =>
      new StrategoList(null, null, null, 0)
  }

  def buildString(s: String) =
    new StrategoString(s, null, 0)
}
