package nl.tudelft.fragments.spoofax

import com.typesafe.scalalogging.Logger
import nl.tudelft.fragments.Rule
import nl.tudelft.fragments.spoofax.models._
import org.metaborg.core.language.LanguageIdentifier
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
  val SdfPath = "zip:/Users/martijn/Projects/sdf/org.metaborg.meta.lang.template/target/org.metaborg.meta.lang.template-2.1.0-SNAPSHOT.spoofax-language!/"
  val NablPath = "zip:/Users/martijn/Projects/spoofax-releng/nabl/org.metaborg.meta.nabl2.lang/target/org.metaborg.meta.nabl2.lang-2.1.0-SNAPSHOT.spoofax-language!/"

  def load(projectPath: String, identifier: String, name: String): Language = {
    val Array(_, id, version) = identifier.split(':')

    logger.info("Loading productions")

    val productions = Productions.read(
      sdfPath = SdfPath,
      productionsPath = s"$projectPath/syntax/$name.sdf3"
    )

    logger.info("Computing signatures")

    val signatures = defaultSignatures ++ productions.map(_.toSignature)

    logger.info("Loading specification")

    val specification = Specification.read(
      nablPath = NablPath,
      specPath = s"$projectPath/trans/static-semantics.nabl2"
    )(signatures)

    logger.info("Constructing printer")

    val printer = Printer.printer(
      languagePath = projectPath
    )

    logger.info("Read start symbols")

    val languageImpl = Utils.loadLanguage(s"zip:$projectPath/target/$id-$version.spoofax-language!/")
    val startSymbols = Utils.startSymbols(languageImpl)

    Language(productions, signatures, specification, printer, startSymbols)
  }

  def defaultSignatures: List[Signature] = List(
    // Cons : a * Lits(a) -> List(a)
    OpDecl("Cons", FunType(
      List(
        ConstType(SortVar("a")),
        ConstType(SortAppl("List", List(SortVar("a"))))
      ),
      ConstType(SortAppl("List", List(SortVar("a"))))
    )),

    // Nil : List(a)
    OpDecl("Nil", ConstType(
      SortAppl("List", List(SortVar("a")))
    )),

    // Some : Option(a)
    OpDecl("Some", FunType(
      List(
        ConstType(SortVar("a"))
      ),
      ConstType(SortAppl("Option", List(SortVar("a"))))
    )),

    // None : Option(a)
    OpDecl("None", ConstType(
      SortAppl("Option", List(SortVar("a")))
    )),

    // Conss : a * List(a) -> Iter(a)
    OpDecl("Conss", FunType(
      List(
        ConstType(SortVar("a")),
        ConstType(SortAppl("List", List(SortVar("a"))))
      ),
      ConstType(SortAppl("Iter", List(SortVar("a"))))
    )),

    // : List(a) -> IterStar(a)
    OpDeclInj(FunType(
      List(
        ConstType(SortAppl("List", List(SortVar("a"))))
      ),
      ConstType(SortAppl("IterStar", List(SortVar("a"))))
    ))
  )
}
