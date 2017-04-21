package org.metaborg.spg.core

import org.metaborg.spg.core.lexical.LexicalGenerator
import org.metaborg.spg.core.sdf.Sort
import org.metaborg.spg.core.spoofax.{Converter, Language}
import org.metaborg.spg.core.terms.{Pattern, TermAppl, TermString}
import rx.lang.scala.Observable

import scala.util.Random

/**
  * The syntax generator generates syntactically valid programs.
  *
  * @param language
  * @param config
  * @param random
  */
class SyntaxGenerator(language: Language, config: Config)(implicit val random: Random) {
  /**
    * Create a cold Observable that emits programs for the given language
    * and generation configuration.
    *
    * @return
    */
  def generate(): Observable[String] = {
    Observable(subscriber => {
      Iterator
        .range(0, config.limit)
        .takeWhile(_ => !subscriber.isUnsubscribed)
        .foreach(_ => subscriber.onNext(generateOne()))

      if (!subscriber.isUnsubscribed) {
        subscriber.onCompleted()
      }
    })
  }

  /**
    * Generate a single term by repeatedly invoking generateTry until it
    * returns a syntactically valid term.
    *
    * @return
    */
  def generateOne(): String = {
    val startSymbol = language
      .startSymbols
      .toSeq
      .random

    Iterator
      .continually(generateTry(startSymbol, config.sizeLimit))
      .dropWhile(_.isEmpty)
      .next
      .map(p => language.printer(Converter.toTerm(p)))
      .get
  }

  /**
    * Try to generate a syntactically valid term for the given sort that is at
    * most the given size.
    *
    * If a term can be generated within the given size, returns Some with the
    * term. Otherwise, returns None.
    *
    * @param sort
    * @param size
    * @return
    */
  def generateTry(sort: Sort, size: Int): Option[Pattern] = {
    if (size <= 0) {
      return None
    }

    val constructors = language
      .signature
      .getOperationsTransitive(sort)

    if (constructors.isEmpty) {
      Some(TermString(new LexicalGenerator(language.grammar).generate(sort)))
    } else {
      for (constructor <- constructors.shuffle) {
        val childSize = (size - 1) / (constructor.arity max 1)
        val childTerms = constructor.arguments.map(generateTry(_, childSize))

        if (childTerms.forall(_.isDefined)) {
          return Some(TermAppl(constructor.name, childTerms.map(_.get)))
        }
      }

      None
    }
  }
}
