package org.metaborg.spg.core.spoofax

import org.metaborg.core.language.ILanguageImpl
import org.metaborg.spg.core.spoofax.models._
import org.metaborg.spg.core.Rule
import org.spoofax.interpreter.terms.IStrategoTerm

import scala.collection.mutable

/**
  * Representation of a language consisting of productions, signatures,
  * specification, printer, startSymbols, and implementation.
  *
  * @param grammar
  * @param signature
  * @param specification
  * @param printer
  * @param startSymbols
  * @param implementation
  */
case class Language(grammar: Grammar, signature: Signature, specification: Specification, printer: IStrategoTerm => String, startSymbols: Set[Sort], implementation: ILanguageImpl) {
  /**
    * Check if the given sort is a start symbol. A sort is a start symbol if it
    * is part of the transitive closure of the injection relation.
    *
    * @param sort
    * @return
    */
  def isStartSymbol(sort: Sort): Boolean = {
    signature.injectionsClosure(startSymbols).contains(sort)
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
    specification.rules.filter(rule =>
      name == rule.name && signature.injectionsClosure(sort).flatMap(_.unify(rule.sort)).nonEmpty
    )
  }

  /**
    * A memoized version of the rules function.
    */
  lazy val rulesMem: (String, Sort) => List[Rule] = {
    val memory = mutable.Map.empty[(String, Sort), List[Rule]]

    (s: String, t: Sort) => memory.getOrElseUpdate((s, t), rules(s, t))
  }
}
