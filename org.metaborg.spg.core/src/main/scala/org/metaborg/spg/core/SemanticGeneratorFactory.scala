package org.metaborg.spg.core

import com.google.inject.Inject
import org.metaborg.core.language.{ILanguageImpl, ILanguageService, LanguageIdentifier}
import org.metaborg.core.project.IProject
import org.metaborg.spg.core.spoofax.{Language, LanguageService}
import rx.lang.scala.Observable

import scala.util.Random

class SemanticGeneratorFactory @Inject()(languageService: LanguageService, baseLanguageService: ILanguageService)(implicit random: Random) {
  /**
    * Create a cold Observable that emits programs for the given language
    * implementation and generation configuration.
    *
    * @param lut
    * @param project
    * @param config
    * @return
    */
  def create(lut: ILanguageImpl, project: IProject, config: Config): Observable[String] = {
    val templateLang = getLanguage("org.metaborg:org.metaborg.meta.lang.template:2.1.0")
    val nablLang = getLanguage("org.metaborg:org.metaborg.meta.nabl2.lang:2.1.0")
    val language = languageService.load(templateLang, nablLang, lut, project)

    create(language, config)
  }

  /**
    * Create a cold Observable that emits programs for the given language
    * and generation configuration.
    *
    * @param language
    * @param config
    * @return
    */
  def create(language: Language, config: Config): Observable[String] = {
    val generator = new SemanticGenerator(language, config)

    Observable(subscriber => {
      Iterator
        .range(0, config.limit)
        .takeWhile(_ => !subscriber.isUnsubscribed)
        .foreach(_ => subscriber.onNext(generator.generateOne()))

      if (!subscriber.isUnsubscribed) {
        subscriber.onCompleted()
      }
    })
  }

  /**
    * Get a language implementation based on its identifier.
    *
    * @param identifier
    */
  protected def getLanguage(identifier: String): ILanguageImpl = {
    val languageIdentifier = LanguageIdentifier.parse(identifier)

    baseLanguageService.getImpl(languageIdentifier)
  }
}
