package nl.tudelft.fragments.regex

import org.scalatest.FunSuite

class RegexSuite extends FunSuite {
  val regex: Regex = (Character('P')*) ~ (Character('I')*)

  test("well-formedness derivative wrt P") {
    assert(regex.derive('P') == regex)
  }

  test("well-formedness derivative wrt I") {
    assert(regex.derive('I') == (Character('I')*))
  }

  test("well-formedness wrt invalid character") {
    assert(regex.derive('D') == EmptySet)
  }
}
