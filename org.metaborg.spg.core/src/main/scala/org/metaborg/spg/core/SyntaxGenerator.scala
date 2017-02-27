package org.metaborg.spg.core

import com.google.inject.Inject
import com.typesafe.scalalogging.LazyLogging
import org.metaborg.core.language.{ILanguageImpl, ILanguageService, LanguageIdentifier}
import org.metaborg.core.project.IProject
import org.metaborg.spg.core.lexical.LexicalGenerator
import org.metaborg.spg.core.spoofax.models.{ConstType, FunType, Sort}
import org.metaborg.spg.core.spoofax.{Converter, Language, LanguageService}
import org.metaborg.spg.core.terms.{Pattern, TermAppl, TermString}
import rx.lang.scala.Observable

/**
  * The syntax generator is a degenerate case of the generator that performs
  * simple top-down generation of syntactically valid terms using a
  * recursive-descent strategy with a size limit.
  *
  * @param languageService
  * @param baseLanguageService
  * @param chooser
  */
class SyntaxGenerator @Inject()(val languageService: LanguageService, val baseLanguageService: ILanguageService, chooser: AutomaticChooser) extends LazyLogging {
  /**
    * Create a cold Observable that emits programs for the given language
    * implementation and generation configuration.
    *
    * @param lut
    * @param project
    * @param config
    * @return
    */
  def generate(lut: ILanguageImpl, project: IProject, config: Config): Observable[String] = {
    val templateLang = getLanguage("org.metaborg:org.metaborg.meta.lang.template:2.1.0")
    val nablLang = getLanguage("org.metaborg:org.metaborg.meta.nabl2.lang:2.1.0")
    val language = languageService.load(templateLang, nablLang, lut, project)

    generate(language, config)
  }

  /**
    * Create a cold Observable that emits programs for the given language
    * and generation configuration.
    *
    * @param lut
    * @param config
    * @return
    */
  private def generate(lut: Language, config: Config): Observable[String] = {
    Observable(subscriber => {
      Iterator
        .range(0, config.limit)
        .takeWhile(_ => !subscriber.isUnsubscribed)
        .foreach(_ => subscriber.onNext(generateSingle(lut, config)))

      if (!subscriber.isUnsubscribed) {
        subscriber.onCompleted()
      }
    })
  }

  /**
    * Generate a single term by repeatedly invoking generateTry until it
    * returns a term.
    *
    * @param language
    * @param config
    * @return
    */
  private def generateSingle(implicit language: Language, config: Config): String = {
    val startSymbol = language
      .startSymbols
      .toSeq
      .random

    Iterator
      .continually(generateTry(language, config, startSymbol, 1000))
      .dropWhile(_.isEmpty)
      .next
      .map(pattern => language.printer(Converter.toTerm(pattern)))
      .get
  }

  /**
    * Generate a pattern for the given sort that is at most the given size.
    *
    * If a term can be generated within the given size, returns Some with the
    * term. Otherwise, returns None.
    *
    * @param language
    * @param config
    * @param sort
    * @param size
    * @return
    */
  private def generateTry(implicit language: Language, config: Config, sort: Sort, size: Int): Option[Pattern] = {
    if (size <= 0) {
      return None
    }

    val constructors = language.signatures.forSort(sort)

    if (constructors.isEmpty) {
      Some(TermString(new LexicalGenerator(language.grammar).generate(sort)))
    } else {
      for (constructor <- constructors.shuffle) {
        constructor.typ match {
          case ConstType(_) =>
            return Some(TermAppl(constructor.name, Nil))
          case FunType(types, _) => {
            val childTypes = types.asInstanceOf[List[ConstType]]
            val childSorts = childTypes.map(_.sort)

            if (childSorts.nonEmpty) {
              val childSize = (size - 1) / childSorts.size
              val childTerms = childSorts.map(generateTry(language, config, _, childSize))

              if (childTerms.forall(_.isDefined)) {
                return Some(TermAppl(constructor.name, childTerms.map(_.get)))
              }
            } else {
              return Some(TermAppl(constructor.name))
            }
          }
        }
      }

      None
    }
  }

  /**
    * Get a language implementation based on its identifier.
    *
    * @param identifier
    */
  private def getLanguage(identifier: String) = {
    val languageIdentifier = LanguageIdentifier.parse(identifier)

    baseLanguageService.getImpl(languageIdentifier)
  }
}
