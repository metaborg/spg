package org.metaborg.spg

import org.metaborg.spg.spoofax.models.Strategy
import org.scalatest.FunSuite

class StrategySuite extends FunSuite {
  test("topdown") {
    val conssToCons = new Strategy {
      override def apply(p: Pattern): Option[Pattern] = p match {
        case TermAppl("Conss", children) =>
          Some(TermAppl("Cons", children))
        case _ =>
          None
      }
    }

    val term = TermAppl("Blibla", List(
      TermAppl("TypeDecs", List(
        TermAppl("Fodibo"),
        TermAppl("Conss", List(
          TermAppl("TypeDec", List(
            TermString("n1"),
            TermAppl("ArrayTy", List(
              TermAppl("Tid", List(
                TermAppl("string")
              )),
              TermAppl("Cons", List(
                TermAppl("TypeDec", List(
                  TermAppl("C"),
                  TermAppl("Tid", List(
                    TermAppl("n1")
                  ))
                )),
                TermAppl("Nil")
              ))
            ))
          ))
        ))
      ))
    ))

    val result = Strategy.topdown(Strategy.`try`(conssToCons))(term).get

    println(result)

    TermAppl("TypeDecs", List(
      TermAppl("Cons", List(
        TermAppl("TypeDec", List(
          TermString("n1"),
          TermAppl("ArrayTy", List(
            TermAppl("Tid", List(
              TermAppl("string", List())
            )),
            TermAppl("Cons", List(
              TermAppl("TypeDec", List(
                TermAppl("C", List()),
                TermAppl("Tid", List(
                  TermAppl("n1", List())
                ))
              )),
              TermAppl("Nil", List())
            ))
          ))
        ))
      ))
    ))
  }
}
