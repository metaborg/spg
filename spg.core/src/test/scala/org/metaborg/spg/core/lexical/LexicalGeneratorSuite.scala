package org.metaborg.spg.core.lexical

import org.metaborg.spg.core.spoofax.models._
import org.metaborg.spg.core.spoofax.{Language, models}
import org.scalatest.FunSuite

class LexicalGeneratorSuite extends FunSuite {
  test("generate ") {
    val productions = List(
      // Identifier = [a-zA-Z][a-zA-Z0-9\_]*
      Production(SortAppl("Identifier"), List(
        Simple(models.Range(models.Short('a'), models.Short('z')), models.Range(models.Short('A'), models.Short('Z'))),
        IterStar(
          Simple(models.Range(models.Short('a'), models.Short('z')), models.Range(models.Short('A'), models.Short('Z')), models.Range(models.Short('0'), models.Short('9')), models.Short('_'))
        )
      )),

      // Integernumber = Digitsequence
      Production(SortAppl("Integernumber"), List(SortAppl("Digitsequence"))),

      // Realnumber = Digitsequence "." Digitsequence? Scalefactor?
      Production(SortAppl("Realnumber"), List[models.Symbol](
        SortAppl("Digitsequence"),
        Lit("."),
        Opt(SortAppl("Digitsequence")),
        Opt(SortAppl("Scalefactor"))
      )),

      // Realnumber = Digitsequence Scalefactor
      Production(SortAppl("Realnumber"), List[models.Symbol](
        SortAppl("Digitsequence"),
        Opt(SortAppl("Scalefactor"))
      )),

      // Scalefactor = [Ee] [\+\-]? Digitsequence
      Production(SortAppl("Scalefactor"), List[models.Symbol](
        Simple(models.Short('E'), models.Short('e')),
        Opt(
          Simple(models.Short('+'), models.Short('-'))
        ),
        SortAppl("Digitsequence")
      )),

      // Unsigneddigitsequence = [0-9]+
      Production(SortAppl("Unsigneddigitsequence"), List[models.Symbol](
        Iter(Simple(models.Range(models.Short('0'), models.Short('9'))))
      )),

      // Digitsequence = [\+\-]? Unsigneddigitsequence
      Production(SortAppl("Digitsequence"), List[models.Symbol](
        Opt(
          Simple(models.Short('+'), models.Short('-'))
        ),
        SortAppl("Unsigneddigitsequence")
      )),

      // String = "'" Stringcharacter+  "'"
      Production(SortAppl("String"), List[models.Symbol](
        Lit("'"),
        Iter(
          SortAppl("Stringcharacter")
        ),
        Lit("'")
      )),

      // Stringcharacter = ~[\']
      Production(SortAppl("Stringcharacter"), List[models.Symbol](
        Comp(Simple(models.Short('\\')))
      )),

      // Stringcharacter = "''"
      Production(SortAppl("Stringcharacter"), List[models.Symbol](
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

    val generator = new LexicalGenerator(language.productions)

    for (i <- 0 to 20) {
      println(generator.generate(SortAppl("StrChar")))
    }
  }
}
