import org.metaborg.spg.core.Substitution
import org.metaborg.spg.core.terms.Var
import org.scalatest.FunSuite

class SubstitutionSuite extends FunSuite {
  test("substitute unifiers gives valid unifier") {
    // Arrange
    val σ = Substitution(Map(Var("x") -> Var("y")))
    val τ = Substitution(Map(Var("y") -> Var("z")))

    // Act
    val γ = τ ++ σ

    // Assert
    assert(γ == Substitution(Map(
      Var("x") -> Var("z"),
      Var("y") -> Var("z")
    )))
  }
}
