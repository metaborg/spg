import org.metaborg.spg.core.sdf.SortAppl
import org.metaborg.spg.core.spoofax.{Language, LanguageService}
import org.metaborg.spg.core.stratego.{Injection, Operation, Signature}
import org.metaborg.spg.core.terms.{TermAppl, TermString}
import org.scalatest.FunSuite

class SignatureSuite extends FunSuite {
  val language: Language = Language(
    grammar =
      null,
    signature =
      Signature(LanguageService.defaultSignatures ++ List(
        // Unit : List(Cons) -> Unit
        Operation("Unit", List(SortAppl("List", List(SortAppl("Expr")))), SortAppl("Unit")),

        // Subtract : Expr * Expr -> Expr
        Operation("Subtract", List(SortAppl("Expr"), SortAppl("Expr")), SortAppl("Expr")),

        // Const : INT -> Expr
        Operation("Const", List(SortAppl("INT")), SortAppl("Expr")),

        // : String -> Int
        Injection(SortAppl("String"), SortAppl("INT"))
      )),
    specification =
      null,
    printer =
      null,
    startSymbols =
      null,
    implementation =
      null
  )

  test("infer sort for string") {
    // Arrange
    val program = TermAppl("Subtract", List(
      TermAppl("Const", List(
        TermString("-20")
      )),
      TermAppl("Const", List(
        TermString("-20")
      ))
    ))

    val pattern = TermString("-20")

    // Act
    val sort = language.signature.getSort(program, pattern)

    // Assert
    assert(sort == Some(SortAppl("INT")))
  }

  test("infer sort for string in parametric list") {
    // Arrange
    val program = TermAppl("Unit", List(
      TermAppl("Cons", List(
        TermAppl("Const", List(
          TermString("-20")
        )),
        TermAppl("Nil")
      ))
    ))

    val pattern = TermString("-20")

    // Act
    val sort = language.signature.getSort(program, pattern)

    // Assert
    assert(sort == Some(SortAppl("INT")))
  }
}
