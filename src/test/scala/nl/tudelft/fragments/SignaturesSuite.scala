package nl.tudelft.fragments

import nl.tudelft.fragments.spoofax.Language
import nl.tudelft.fragments.spoofax.models._
import org.scalatest.FunSuite

class SignaturesSuite extends FunSuite {
  val signatures = Signatures(List(
    // Fun : ID * Type * Exp -> Exp
    OpDecl("Fun",
      FunType(
        List(
          ConstType(SortAppl("ID")),
          ConstType(SortAppl("Type")),
          ConstType(SortAppl("Exp"))
        ),
        ConstType(SortAppl("Exp"))
      )
    ),

    // App : Exp * ResetExp -> Exp
    OpDecl("App",
      FunType(
        List(
          ConstType(SortAppl("Exp")),
          ConstType(SortAppl("ResetExp"))
        ),
        ConstType(SortAppl("Exp"))
      )
    ),

    // : Exp -> ResetExp
    OpDeclInj(
      FunType(
        List(
          ConstType(SortAppl("Exp"))
        ),
        ConstType(SortAppl("ResetExp"))
      )
    ),

    // : Number -> Exp
    OpDeclInj(
      FunType(
        List(
          ConstType(SortAppl("Number"))
        ),
        ConstType(SortAppl("Exp"))
      )
    ),

    // : Cons(a, List(a)) -> Iter(a)
    OpDeclInj(
      FunType(
        List(
          ConstType(SortAppl("Cons", List(SortVar("a"), SortAppl("List", List(SortVar("a"))))))
        ),
        ConstType(SortAppl("Iter", List(SortVar("a"))))
      )
    )
  ))

  test("get direct injections for simple sort") {
    val inj = Sort.injections(signatures)(SortAppl("ResetExp"))

    assert(inj == Set(SortAppl("Exp")))
  }

  test("get injections closure") {
    val injections = Sort.injectionsClosure(signatures)(Set(SortAppl("ResetExp")))

    assert(injections == Set(
      SortAppl("ResetExp"),
      SortAppl("Exp"),
      SortAppl("Number")
    ))
  }

  test("get injections closure for parametric sort") {
    val injections = Sort.injectionsClosure(signatures)(Set(SortAppl("Iter", List(SortVar("x")))))

    assert(injections == Set(
      SortAppl("Iter", List(SortVar("x"))),
      SortAppl("Cons", List(SortVar("x"), SortAppl("List", List(SortVar("x")))))
    ))
  }

  test("get injections closure for complex sort") {
    val injections = Sort.injectionsClosure(signatures)(Set(SortAppl("Iter", List(SortAppl("Statement")))))

    assert(injections == Set(
      SortAppl("Iter", List(SortAppl("Statement"))),
      SortAppl("Cons", List(SortAppl("Statement"), SortAppl("List", List(SortAppl("Statement")))))
    ))
  }

  test("closure of ResultType in Pascal") {
    val language = Language.load("/Users/martijn/Projects/metaborg-pascal/org.metaborg.lang.pascal", "org.metaborg:org.metaborg.lang.pascal:0.1.0-SNAPSHOT", "Pascal")
    val injections = Sort.injectionsClosure(language.signatures)(Set(SortAppl("ResultType")))

    assert(injections == Set(
      SortAppl("ResultType"),
      SortAppl("TypeIdentifier")
    ))
  }

  test("closure of List(MethodDecl) in MiniJava") {
    val language = Language.load("/Users/martijn/Projects/MiniJava", "org.metaborg:MiniJava:0.1.0-SNAPSHOT", "MiniJava")
    val injections = Sort.injectionsClosure(language.signatures)(Set(SortAppl("IterStar", List(SortAppl("MethodDecl", List())))))

    assert(injections == Set(
      SortAppl("List", List(SortAppl("MethodDecl", List()))),
      SortAppl("IterStar", List(SortAppl("MethodDecl", List())))
    ))
  }
}
