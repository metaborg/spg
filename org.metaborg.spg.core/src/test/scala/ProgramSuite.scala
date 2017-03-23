import org.metaborg.spg.core.sdf.SortAppl
import org.metaborg.spg.core.{Binding, Program}
import org.metaborg.spg.core.solver.{CGRef, CGenRecurse, Resolution, Subtypes, TypeEnv}
import org.metaborg.spg.core.spoofax.Language
import org.metaborg.spg.core.stratego.{Injection, Signature}
import org.metaborg.spg.core.terms.{TermAppl, TermString, Var}
import org.scalatest.FunSuite

class ProgramSuite extends FunSuite {
  val language: Language = Language(null, Signature(List(Injection(SortAppl("Exp"), SortAppl("ResetExp")))), null, null, null, null)

  test("merge two programs") {
    // Arrange
    val program = Program(SortAppl("Exp", List()),TermAppl("App", List(TermAppl("Var", List(TermString("x105"))), Var("x122"))),List(TermAppl("s104", List())),Some(TermAppl("TInt", List())),List(CGenRecurse("Default", Var("x122"), List(TermAppl("s104", List())), Some(Var("x128")), SortAppl("ResetExp", List()), 0), CGRef(TermAppl("Occurrence", List(TermString("Var"), TermString("x105"), TermString("2"))),TermAppl("s104", List()))),TypeEnv(Map(Binding(TermAppl("Occurrence", List(TermString("Var"), TermString("x105"), TermString("4"))), TermAppl("TFun", List(Var("x128"), TermAppl("TInt", List())))))),Resolution(Map(Tuple2(TermAppl("Occurrence", List(TermString("Var"), TermString("x105"), TermString("2"))), TermAppl("Occurrence", List(TermString("Var"), TermString("x105"), TermString("4")))))),Subtypes(List()),List())
    val recurse = CGenRecurse("Default", Var("x122"), List(TermAppl("s104", List())), Some(Var("x128")), SortAppl("ResetExp", List()), 0)
    val subProgram = Program(SortAppl("Exp", List()),TermAppl("IntValue", List(Var("x133"))),List(TermAppl("s104", List())),Some(TermAppl("TInt", List())),List(),TypeEnv(Map()),Resolution(Map()),Subtypes(List()),List())

    // Act
    val merged = program.merge(recurse, subProgram)(language)

    // Assert
    val p3 = Program(SortAppl("Exp", List()),TermAppl("App", List(TermAppl("Var", List(TermString("x105"))), TermAppl("IntValue", List(Var("x101"))))),List(TermAppl("s104", List())),Some(TermAppl("TInt", List())),List(CGRef(TermAppl("Occurrence", List(TermString("Var"), TermString("x105"), TermString("2"))),TermAppl("s104", List()))),TypeEnv(Map()),Resolution(Map(Tuple2(TermAppl("Occurrence", List(TermString("Var"), TermString("x105"), TermString("2"))), TermAppl("Occurrence", List(TermString("Var"), TermString("x105"), TermString("4")))))),Subtypes(List()),List())

    assert(merged == Some(p3))
  }
}
