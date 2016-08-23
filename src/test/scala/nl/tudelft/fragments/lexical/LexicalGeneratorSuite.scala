package nl.tudelft.fragments.lexical

import nl.tudelft.fragments.spoofax.models._
import org.scalatest.FunSuite

class LexicalGeneratorSuite extends FunSuite {
  test("generate ") {
    val productions = List(
      // Identifier = [a-zA-Z][a-zA-Z0-9\_]*
      Production(Sort("Identifier"), List(
        Simple(Range(Short('a'), Short('z')), Range(Short('A'), Short('Z'))),
        IterStar(
          Simple(Range(Short('a'), Short('z')), Range(Short('A'), Short('Z')), Range(Short('0'), Short('9')), Short('_'))
        )
      )),

      // Integernumber = Digitsequence
      Production(Sort("Integernumber"), List(Sort("Digitsequence"))),

      // Realnumber = Digitsequence "." Digitsequence? Scalefactor?
      Production(Sort("Realnumber"), List[Symbol](
        Sort("Digitsequence"),
        Lit("."),
        Opt(Sort("Digitsequence")),
        Opt(Sort("Scalefactor"))
      )),

      // Realnumber = Digitsequence Scalefactor
      Production(Sort("Realnumber"), List[Symbol](
        Sort("Digitsequence"),
        Opt(Sort("Scalefactor"))
      )),

      // Scalefactor = [Ee] [\+\-]? Digitsequence
      Production(Sort("Scalefactor"), List[Symbol](
        Simple(Short('E'), Short('e')),
        Opt(
          Simple(Short('+'), Short('-'))
        ),
        Sort("Digitsequence")
      )),

      // Unsigneddigitsequence = [0-9]+
      Production(Sort("Unsigneddigitsequence"), List[Symbol](
        Iter(Simple(Range(Short('0'), Short('9'))))
      )),

      // Digitsequence = [\+\-]? Unsigneddigitsequence
      Production(Sort("Digitsequence"), List[Symbol](
        Opt(
          Simple(Short('+'), Short('-'))
        ),
        Sort("Unsigneddigitsequence")
      )),

      // String = "'" Stringcharacter+  "'"
      Production(Sort("String"), List[Symbol](
        Lit("'"),
        Iter(
          Sort("Stringcharacter")
        ),
        Lit("'")
      )),

      // Stringcharacter = ~[\']
      Production(Sort("Stringcharacter"), List[Symbol](
        Comp(Simple(Short('\\')))
      )),

      // Stringcharacter = "''"
      Production(Sort("Stringcharacter"), List[Symbol](
        Lit("''")
      ))
    )

    for (i <- 0 to 10) {
      println(new LexicalGenerator(productions).generate(Sort("Identifier")))
    }

    for (i <- 0 to 10) {
      println(new LexicalGenerator(productions).generate(Sort("Integernumber")))
    }

    for (i <- 0 to 10) {
      println(new LexicalGenerator(productions).generate(Sort("Realnumber")))
    }

    for (i <- 0 to 10) {
      println(new LexicalGenerator(productions).generate(Sort("String")))
    }
  }
}
