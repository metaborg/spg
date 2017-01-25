package org.metaborg.spg.spoofax

import com.typesafe.scalalogging.Logger
import org.metaborg.core.language.ILanguageImpl
import org.metaborg.spg.spoofax.models._
import org.metaborg.spg.{Pattern, Rule, TermAppl}
import org.slf4j.LoggerFactory
import org.spoofax.interpreter.terms.IStrategoTerm

import scala.collection.mutable

/**
  * Representation of a language consisting of productions, signatures,
  * specification, printer, startSymbols, and implementation.
  *
  * @param productions
  * @param signatures
  * @param specification
  * @param printer
  * @param startSymbols
  * @param implementation
  */
class Language(val productions: List[Production], val signatures: Signatures, val specification: Specification, val printer: IStrategoTerm => String, val startSymbols: Set[Sort], val implementation: ILanguageImpl) {
  val cache = mutable.Map[(String, Sort), List[Rule]]()

  /**
    * Check if the given sort is a start symbol. A sort is a start symbol if it
    * is part of the transitive closure of the injection relation.
    *
    * @param sort
    * @return
    */
  def isStartSymbol(sort: Sort): Boolean = {
    Sort.injectionsClosure(signatures, startSymbols).contains(sort)
  }

  /**
    * Check if the given language is a start rule.
    *
    * @param rule
    * @return
    */
  def isStartRule(rule: Rule): Boolean = {
    isStartSymbol(rule.sort)
  }

  /**
    * Check if the given rule is an init rule.
    *
    * @param rule
    * @return
    */
  def isInitRule(rule: Rule): Boolean = {
    rule.name == "Init"
  }

  /**
    * Get the start rules for the language.
    *
    * @return
    */
  def startRules: List[Rule] = {
    specification.rules.filter(isStartRule)
  }

  /**
    * Get the init rule for the language.
    *
    * @return
    */
  def initRule: Rule = {
    specification.rules.find(isInitRule).get
  }

  /**
    * Get all the sorts of the language.
    *
    * @return
    */
  def sorts: List[Sort] = signatures.list.map {
    case OpDecl(_, FunType(_, ConstType(resultType))) =>
      resultType
    case OpDeclInj(FunType(_, ConstType(resultType))) =>
      resultType
    case OpDecl(_, ConstType(resultType)) =>
      resultType
  }.distinct

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

  /**
    * Get all distinct rule names.
    *
    * @return
    */
  def ruleNames: List[String] = {
    specification.rules.map(_.name).distinct
  }

  /**
    * Get rules for the given name and sort.
    *
    * Rules have polymorphic sorts, e.g. List(a). This makes it impossible to
    * pre-compute the rules for a given sort. However, since the specification
    * stays the same, we are able to memoize the results to a specific
    * invocation.
    *
    * @param name
    * @param sort
    * @return
    */
  def rules(name: String, sort: Sort): List[Rule] = {
    cache.getOrElseUpdate((name, sort), specification.rules.filter(rule =>
      name == rule.name && Sort.injectionsClosure(signatures, sort).flatMap(_.unify(rule.sort)).nonEmpty
    ))
  }
}

object Language {
  val logger = Logger(LoggerFactory.getLogger(this.getClass))

  def load(sdfPath: String, nablPath: String, projectPath: String, semanticsPath: String): Language = {
    logger.info("Loading language at path {}", projectPath)
    val implementation = Utils.loadLanguage(projectPath)

    logger.info("Loading productions")
    val productions = Productions.read(sdfPath, s"$projectPath/syntax/${implementation.belongsTo().name()}.sdf3")

    logger.info("Computing signatures")
    val signatures = Signatures(defaultSignatures ++ productions.map(_.toSignature))

    logger.info("Loading static semantics")
    val specification = Specification.read(nablPath, s"$projectPath/$semanticsPath")(signatures)

    logger.info("Constructing printer")
    val printer = Utils.getPrinter(implementation)

    logger.info("Read start symbols")
    val startSymbols = Utils.startSymbols(implementation)

    new Language(productions, signatures, specification, printer, startSymbols, implementation)
  }

  /**
    * These signatures are defined in the standard library and do not appear in
    * the signature file. Hence, we add them ourselves.
    *
    * The last two signatures define the sort Iter(a) and IterStar(a):
    *  - Iter(a) is a list with at least one element. This is not supported in
    * Stratego.
    *  - IterStar(a) is a list with zero or more elements. It is the same as
    * List(a), so we define an injection to it.
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
