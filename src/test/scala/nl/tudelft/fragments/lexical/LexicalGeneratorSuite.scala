package nl.tudelft.fragments.lexical

import nl.tudelft.fragments.spoofax.Language
import nl.tudelft.fragments.spoofax.models._
import org.scalatest.FunSuite

class LexicalGeneratorSuite extends FunSuite {
  test("generate ") {
    val productions = List(
      // Identifier = [a-zA-Z][a-zA-Z0-9\_]*
      Production(SortAppl("Identifier"), List(
        Simple(Range(Short('a'), Short('z')), Range(Short('A'), Short('Z'))),
        IterStar(
          Simple(Range(Short('a'), Short('z')), Range(Short('A'), Short('Z')), Range(Short('0'), Short('9')), Short('_'))
        )
      )),

      // Integernumber = Digitsequence
      Production(SortAppl("Integernumber"), List(SortAppl("Digitsequence"))),

      // Realnumber = Digitsequence "." Digitsequence? Scalefactor?
      Production(SortAppl("Realnumber"), List[Symbol](
        SortAppl("Digitsequence"),
        Lit("."),
        Opt(SortAppl("Digitsequence")),
        Opt(SortAppl("Scalefactor"))
      )),

      // Realnumber = Digitsequence Scalefactor
      Production(SortAppl("Realnumber"), List[Symbol](
        SortAppl("Digitsequence"),
        Opt(SortAppl("Scalefactor"))
      )),

      // Scalefactor = [Ee] [\+\-]? Digitsequence
      Production(SortAppl("Scalefactor"), List[Symbol](
        Simple(Short('E'), Short('e')),
        Opt(
          Simple(Short('+'), Short('-'))
        ),
        SortAppl("Digitsequence")
      )),

      // Unsigneddigitsequence = [0-9]+
      Production(SortAppl("Unsigneddigitsequence"), List[Symbol](
        Iter(Simple(Range(Short('0'), Short('9'))))
      )),

      // Digitsequence = [\+\-]? Unsigneddigitsequence
      Production(SortAppl("Digitsequence"), List[Symbol](
        Opt(
          Simple(Short('+'), Short('-'))
        ),
        SortAppl("Unsigneddigitsequence")
      )),

      // String = "'" Stringcharacter+  "'"
      Production(SortAppl("String"), List[Symbol](
        Lit("'"),
        Iter(
          SortAppl("Stringcharacter")
        ),
        Lit("'")
      )),

      // Stringcharacter = ~[\']
      Production(SortAppl("Stringcharacter"), List[Symbol](
        Comp(Simple(Short('\\')))
      )),

      // Stringcharacter = "''"
      Production(SortAppl("Stringcharacter"), List[Symbol](
        Lit("''")
      ))
    )

    for (i <- 0 to 10) {
      println(new LexicalGenerator(productions).generate(SortAppl("Identifier")))
    }

    for (i <- 0 to 10) {
      println(new LexicalGenerator(productions).generate(SortAppl("Integernumber")))
    }

    for (i <- 0 to 10) {
      println(new LexicalGenerator(productions).generate(SortAppl("Realnumber")))
    }

    for (i <- 0 to 10) {
      println(new LexicalGenerator(productions).generate(SortAppl("String")))
    }
  }

  test("generate Tiger strings") {
    val language = Language.load("/Users/martijn/Projects/metaborg-tiger/org.metaborg.lang.tiger", "org.metaborg:org.metaborg.lang.tiger:0.1.0-SNAPSHOT", "Tiger")

    val generator = new LexicalGenerator(language.productions)

    for (i <- 0 to 20) {
      println(generator.generate(SortAppl("StrChar")))
    }
  }
}
