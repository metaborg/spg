package org.metaborg.spg.core.spoofax

import org.metaborg.core.language.ILanguageImpl
import org.metaborg.spg.core.spoofax.models._
import org.metaborg.spg.core.{Pattern, Rule, TermAppl}
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
case class Language(productions: List[Production], signatures: Signatures, specification: Specification, printer: IStrategoTerm => String, startSymbols: Set[Sort], implementation: ILanguageImpl) {
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
