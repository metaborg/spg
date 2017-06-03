package org.metaborg.spg.core

import org.metaborg.core.language.{ILanguageImpl, ILanguageService, LanguageIdentifier}
import org.metaborg.core.project.IProject
import org.metaborg.spg.core.spoofax.{Language, LanguageService}
import rx.lang.scala.Observable

abstract class AbstractGenerator(val languageService: LanguageService, val baseLanguageService: ILanguageService) {
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
    val templateLang = getLanguage("org.metaborg:org.metaborg.meta.lang.template:2.2.1")
    val nablLang = getLanguage("org.metaborg:org.metaborg.meta.nabl2.lang:2.2.1")
    val language = languageService.load(templateLang, nablLang, lut, project)

    generate(language, config)
  }

  /**
    * Create a cold Observable that emits programs for the given language
    * and generation configuration.
    *
    * @param language
    * @param config
    * @return
    */
  private def generate(language: Language, config: Config): Observable[String] = {
    Observable(subscriber => {
      Iterator
        .range(0, config.limit)
        .takeWhile(_ => !subscriber.isUnsubscribed)
        .foreach(_ => subscriber.onNext(generateSingle(language, config)))

      if (!subscriber.isUnsubscribed) {
        subscriber.onCompleted()
      }
    })
  }

  /**
    * Generate a single term by repeatedly invoking generateTry until it
    * returns a well-formed term.
    *
    * @param language
    * @param config
    * @return
    */
  def generateSingle(language: Language, config: Config): String

  /**
    * Get a language implementation based on its identifier.
    *
    * @param identifier
    */
  protected def getLanguage(identifier: String) = {
    val languageIdentifier = LanguageIdentifier.parse(identifier)

    baseLanguageService.getImpl(languageIdentifier)
  }
}
