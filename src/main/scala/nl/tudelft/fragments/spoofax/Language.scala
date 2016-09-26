package nl.tudelft.fragments.spoofax

import com.typesafe.scalalogging.Logger
import nl.tudelft.fragments.spoofax.models._
import nl.tudelft.fragments.{Pattern, Rule, TermAppl}
import org.slf4j.LoggerFactory
import org.spoofax.interpreter.terms.{IStrategoString, IStrategoTerm}

class Language(val productions: List[Production], val signatures: Signatures, val specification: Specification, val printer: IStrategoTerm => IStrategoString, val startSymbols: List[Sort]) {
  /**
    * Check if the given sort is a start symbol
    *
    * @param sort
    * @return
    */
  def isStartSymbol(sort: Sort): Boolean =
    startSymbols.contains(sort)

  /**
    * Check if the given langauge is a start rule
    *
    * @param rule
    * @return
    */
  def isStartRule(rule: Rule): Boolean =
    isStartSymbol(rule.sort)

  /**
    * Get the start rules for the language
    *
    * @return
    */
  def startRules: List[Rule] =
    specification.rules.filter(isStartRule)

  /**
    * Get all the sorts of the language
    *
    * @return
    */
  def sorts: List[Sort] = signatures
    .list
    .map {
      case OpDecl(_, FunType(_, ConstType(resultType))) =>
        resultType
      case OpDeclInj(FunType(_, ConstType(resultType))) =>
        resultType
      case OpDecl(_, ConstType(resultType)) =>
        resultType
    }
    .distinct

  /**
    * Get signatures for the given pattern based on its constructor name.
    *
    * TODO: This ignores overloaded and duplicate constructors!
    *
    * @param pattern
    * @return
    */
  def signatures(pattern: Pattern): List[Signature] = pattern match {
    case termAppl: TermAppl =>
      signatures
        .list
        .filter(_.isInstanceOf[OpDecl])
        .map(_.asInstanceOf[OpDecl])
        .filter(_.name == termAppl.cons)
    case _ =>
      Nil
  }
}

object Language {
  val logger = Logger(LoggerFactory.getLogger(this.getClass))
  val SdfPath = "zip:/Users/martijn/Projects/sdf/org.metaborg.meta.lang.template/target/org.metaborg.meta.lang.template-2.1.0-SNAPSHOT.spoofax-language!/"
  val NablPath = "zip:/Users/martijn/Projects/spoofax-releng/nabl/org.metaborg.meta.nabl2.lang/target/org.metaborg.meta.nabl2.lang-2.1.0-SNAPSHOT.spoofax-language!/"

  def load(projectPath: String, identifier: String, name: String): Language = {
    val Array(_, id, version) = identifier.split(':')

    logger.info("Loading language {}", id)

    val languageImpl = Utils.loadLanguage(s"zip:$projectPath/target/$id-$version.spoofax-language!/")

    logger.info("Loading productions")

    val productions = Productions.read(
      sdfPath = SdfPath,
      productionsPath = s"$projectPath/syntax/$name.sdf3"
    )

    logger.info("Computing signatures")

    val signatures = Signatures(defaultSignatures ++ productions.map(_.toSignature))

    logger.info("Loading specification")

    val specification = Specification.read(
      nablPath = NablPath,
      specPath = s"$projectPath/trans/static-semantics.nabl2"
    )(signatures)

    logger.info("Constructing printer")

    val printer = Utils.getPrinter(languageImpl)

    logger.info("Read start symbols")

    val startSymbols = Utils.startSymbols(languageImpl)

    new Language(productions, signatures, specification, printer, startSymbols)
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

//    // Duplicate Cons to get larger lists (in expectation)
//    OpDecl("Cons", FunType(
//      List(
//        ConstType(SortVar("a")),
//        ConstType(SortAppl("List", List(SortVar("a"))))
//      ),
//      ConstType(SortAppl("List", List(SortVar("a"))))
//    )),
//
//    // Duplicate Cons to get larger lists (in expectation)
//    OpDecl("Cons", FunType(
//      List(
//        ConstType(SortVar("a")),
//        ConstType(SortAppl("List", List(SortVar("a"))))
//      ),
//      ConstType(SortAppl("List", List(SortVar("a"))))
//    )),

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
