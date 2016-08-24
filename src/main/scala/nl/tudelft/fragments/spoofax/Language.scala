package nl.tudelft.fragments.spoofax

import com.typesafe.scalalogging.Logger
import nl.tudelft.fragments.Rule
import nl.tudelft.fragments.spoofax.models._
import org.slf4j.LoggerFactory
import org.spoofax.interpreter.terms.{IStrategoString, IStrategoTerm}

case class Language(productions: List[Production], signatures: List[Signature], specification: Specification, printer: IStrategoTerm => IStrategoString, startSymbols: List[Sort]) {
  def isStartSymbol(sort: Sort): Boolean =
    startSymbols.contains(sort)

  def isStartRule(rule: Rule): Boolean =
    isStartSymbol(rule.sort)

  def startRules: List[Rule] =
    specification.rules.filter(isStartRule)
}

object Language {
  val logger = Logger(LoggerFactory.getLogger(this.getClass))

  def load(projectPath: String, name: String): Language = {
    logger.info("Loading productions")

    val productions = Productions.read(
      sdfPath = "zip:/Users/martijn/Projects/sdf/org.metaborg.meta.lang.template/target/org.metaborg.meta.lang.template-2.1.0-SNAPSHOT.spoofax-language!/",
      productionsPath = s"$projectPath/syntax/$name.sdf3"
    )

    logger.info("Computing signatures")

    val signatures = defaultSignatures ++ productions
      .filter(_.cons.isDefined)
      .map(_.toSignature)

    logger.info("Loading specification")

    val specification = Specification.read(
      nablPath = "zip:/Users/martijn/Projects/nabl/org.metaborg.meta.nabl2.lang/target/org.metaborg.meta.nabl2.lang-2.1.0-SNAPSHOT.spoofax-language!/",
      specPath = s"$projectPath/trans/static-semantics.nabl2"
    )(signatures)

    logger.info("Constructing printer")

    val printer = Printer.printer(
      languagePath = projectPath
    )

    logger.info("Read start symbols")

    val languageImpl = Utils.loadLanguage(s"zip:$projectPath/target/org.metaborg.lang.${name.toLowerCase}-2.1.0-SNAPSHOT.spoofax-language!/")
    val startSymbols = Utils.startSymbols(languageImpl)

    Language(productions, signatures, specification, printer, startSymbols)
  }

  def defaultSignatures: List[Signature] = List(
    // List
    OpDecl("Cons", FunType(
      List(
        ConstType(models.SortVar("a")),
        ConstType(models.SortAppl("List", List(models.SortVar("a"))))
      ),
      ConstType(models.SortAppl("List", List(models.SortVar("a"))))
    ))

    , OpDecl("Nil", ConstType(
      models.SortAppl("List", List(models.SortVar("a")))
    ))

    // Option
    , OpDecl("Some", FunType(
      List(
        ConstType(models.SortVar("a"))
      ),
      ConstType(models.SortAppl("Option", List(models.SortVar("a"))))
    ))

    , OpDecl("None", ConstType(
      models.SortAppl("Option", List(models.SortVar("a")))
    ))

    // Iter
    , OpDeclInj(
      FunType(
        List(
          ConstType(models.SortAppl("Cons", List(models.SortVar("a"), models.SortAppl("List", List(models.SortVar("a"))))))
        ),
        ConstType(models.SortAppl("Iter", List(models.SortVar("a"))))
      )
    )

    // IterStar
    , OpDeclInj(
      FunType(
        List(
          ConstType(models.SortAppl("List", List(models.SortVar("a"))))
        ),
        ConstType(models.SortAppl("IterStar", List(models.SortVar("a"))))
      )
    )
  )
}
