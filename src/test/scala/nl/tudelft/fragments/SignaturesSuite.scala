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
    val inj = Sort.injections(signatures, SortAppl("ResetExp"))

    assert(inj == Set(SortAppl("Exp")))
  }

  test("get injections closure") {
    val injections = Sort.injectionsClosure(signatures, SortAppl("ResetExp"))

    assert(injections == Set(
      SortAppl("ResetExp"),
      SortAppl("Exp"),
      SortAppl("Number")
    ))
  }

  test("get injections closure for parametric sort") {
    val injections = Sort.injectionsClosure(signatures, SortAppl("Iter", List(SortVar("x"))))

    assert(injections == Set(
      SortAppl("Iter", List(SortVar("x"))),
      SortAppl("Cons", List(SortVar("x"), SortAppl("List", List(SortVar("x")))))
    ))
  }

  test("get injections closure for complex sort") {
    val injections = Sort.injectionsClosure(signatures, SortAppl("Iter", List(SortAppl("Statement"))))

    assert(injections == Set(
      SortAppl("Iter", List(SortAppl("Statement"))),
      SortAppl("Cons", List(SortAppl("Statement"), SortAppl("List", List(SortAppl("Statement")))))
    ))
  }

  test("closure of ResultType in Pascal") {
    val language = Language.load(
      sdfPath =
        "zip:/Users/martijn/Projects/spoofax-releng/sdf/org.metaborg.meta.lang.template/target/org.metaborg.meta.lang.template-2.1.0.spoofax-language!/",
      nablPath =
        "zip:/Users/martijn/Projects/spoofax-releng/nabl/org.metaborg.meta.nabl2.lang/target/org.metaborg.meta.nabl2.lang-2.1.0.spoofax-language!/",
      projectPath =
        "/Users/martijn/Projects/metaborg-pascal/org.metaborg.lang.pascal",
      semanticsPath =
        "trans/static-semantics.nabl2"
    )

    val injections = Sort.injectionsClosure(language.signatures, SortAppl("ResultType"))

    assert(injections == Set(
      SortAppl("ResultType"),
      SortAppl("TypeIdentifier")
    ))
  }

  test("closure of List(MethodDecl) in MiniJava") {
    val language = Language.load(
      sdfPath =
        "zip:/Users/martijn/Projects/spoofax-releng/sdf/org.metaborg.meta.lang.template/target/org.metaborg.meta.lang.template-2.1.0.spoofax-language!/",
      nablPath =
        "zip:/Users/martijn/Projects/spoofax-releng/nabl/org.metaborg.meta.nabl2.lang/target/org.metaborg.meta.nabl2.lang-2.1.0.spoofax-language!/",
      projectPath =
        "/Users/martijn/Projects/MiniJava",
      semanticsPath =
        "trans/static-semantics.nabl2"
    )

    val injections = Sort.injectionsClosure(language.signatures, SortAppl("IterStar", List(SortAppl("MethodDecl", List()))))

    assert(injections == Set(
      SortAppl("List", List(SortAppl("MethodDecl", List()))),
      SortAppl("IterStar", List(SortAppl("MethodDecl", List())))
    ))
  }

  test("get sort for pattern") {
    val language = Language.load(
      sdfPath =
        "zip:/Users/martijn/Projects/spoofax-releng/sdf/org.metaborg.meta.lang.template/target/org.metaborg.meta.lang.template-2.1.0.spoofax-language!/",
      nablPath =
        "zip:/Users/martijn/Projects/spoofax-releng/nabl/org.metaborg.meta.nabl2.lang/target/org.metaborg.meta.nabl2.lang-2.1.0.spoofax-language!/",
      projectPath =
        "/Users/martijn/Projects/metaborg-tiger/org.metaborg.lang.tiger",
      semanticsPath =
        "trans/static-semantics.nabl2"
    )

    val sortOpt = language.signatures.sortForPattern(TermAppl("Mod", List(Var("e"))), Var("e"))

    assert(sortOpt.isDefined)
    assert(sortOpt.get unifiesWith SortAppl("Exp"))
  }

  test("get sort for pattern with alias") {
    val language = Language.load(
      sdfPath =
        "zip:/Users/martijn/Projects/spoofax-releng/sdf/org.metaborg.meta.lang.template/target/org.metaborg.meta.lang.template-2.1.0.spoofax-language!/",
      nablPath =
        "zip:/Users/martijn/Projects/spoofax-releng/nabl/org.metaborg.meta.nabl2.lang/target/org.metaborg.meta.nabl2.lang-2.1.0.spoofax-language!/",
      projectPath =
        "/Users/martijn/Projects/metaborg-tiger/org.metaborg.lang.tiger",
      semanticsPath =
        "trans/static-semantics.nabl2"
    )

    val sortOpt = language.signatures.sortForPattern(TermAppl("Cons", List(Var("e"), As(Var("es"),TermAppl("Cons", List(Var("x10"), Var("x11")))))), Var("es"))

    assert(sortOpt.isDefined)
    assert(sortOpt.get unifiesWith SortAppl("List", List(SortVar("a"))))
  }
}
