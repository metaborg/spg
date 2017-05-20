import org.metaborg.spg.core.Unifier
import org.metaborg.spg.core.terms.Var
import org.scalatest.FunSuite

class UnifierSuite extends FunSuite {
  test("unify two lists of patterns") {
    // Arrange
    val l1 = List(Var("x"), Var("x"))
    val l2 = List(Var("y"), Var("z"))

    // Act
    val unifier = Unifier.unify(l1, l2)

    // Assert
    assert(unifier.isDefined)
    assert(l1.substitute(unifier.get.delegate) == l2.substitute(unifier.get.delegate))
  }
}
