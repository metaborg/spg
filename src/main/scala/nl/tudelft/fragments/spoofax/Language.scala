package nl.tudelft.fragments.spoofax

import com.typesafe.scalalogging.Logger
import nl.tudelft.fragments.spoofax.models._
import nl.tudelft.fragments.{Pattern, Rule, TermAppl}
import org.slf4j.LoggerFactory
import org.spoofax.interpreter.terms.{IStrategoString, IStrategoTerm}

class Language(val productions: List[Production], val signatures: Signatures, val specification: Specification, val printer: IStrategoTerm => String, val startSymbols: List[Sort]) {
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

  /**
    * Get the constructor names for the language.
    *
    * @return
    */
  def constructors: List[String] = signatures.list.collect {
    case OpDecl(constructor, _) =>
      constructor
  }
}

object Language {
  val logger = Logger(LoggerFactory.getLogger(this.getClass))
  val sdfPath = "zip:/Users/martijn/Projects/sdf/org.metaborg.meta.lang.template/target/org.metaborg.meta.lang.template-2.1.0-SNAPSHOT.spoofax-language!/"
  val nablPath = "zip:/Users/martijn/Projects/spoofax-releng/nabl/org.metaborg.meta.nabl2.lang/target/org.metaborg.meta.nabl2.lang-2.1.0-SNAPSHOT.spoofax-language!/"

  def load(projectPath: String, identifier: String, name: String): Language = {
    val Array(_, id, version) = identifier.split(':')

    logger.info("Loading language {}", id)
    val languageImpl = Utils.loadLanguage(s"zip:$projectPath/target/$id-$version.spoofax-language!/")

    logger.info("Loading productions")
    val productions = Productions.read(sdfPath, s"$projectPath/syntax/$name.sdf3")

    logger.info("Computing signatures")
    val signatures = Signatures(defaultSignatures ++ productions.map(_.toSignature))

    logger.info("Loading static semantics")
    val specification = Specification.read(nablPath, s"$projectPath/trans/static-semantics.nabl2")(signatures)

    logger.info("Constructing printer")
    val printer = Utils.getPrinter(languageImpl)

    logger.info("Read start symbols")
    val startSymbols = Utils.startSymbols(languageImpl)

    new Language(productions, signatures, specification, printer, startSymbols)
  }

  /**
    * These signatures are defined in the standard library and do not appear in
    * the signature file. Hence, we add them ourselves.
    *
    * The last two signatures define the sort Iter(a) and IterStar(a):
    *  - Iter(a) is a list with at least one element. This is not supported in
    *  Stratego.
    *  - IterStar(a) is a list with zero or more elements. It is the same as
    *  List(a), so we define an injection to it.
    *
    * @return
    */
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
