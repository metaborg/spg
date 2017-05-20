package org.metaborg.spg.core

import com.google.inject.Inject
import org.metaborg.core.language.{ILanguageImpl, ILanguageService, LanguageIdentifier}
import org.metaborg.core.project.IProject
import org.metaborg.spg.core.spoofax.{Language, LanguageService}
import rx.lang.scala.Observable

import scala.util.Random

class SyntaxGeneratorFactory @Inject()(languageService: LanguageService, baseLanguageService: ILanguageService)(implicit random: Random) {
  /**
    * Create a generator for programs based on the given language
    * implementation and generation configuration.
    *
    * @param lut
    * @param project
    * @param config
    * @return
    */
  def create(lut: ILanguageImpl, project: IProject, config: Config): SyntaxGenerator = {
    create(loadLanguage(lut, project), config)
  }

  def create(language: Language, config: Config): SyntaxGenerator = {
    new SyntaxGenerator(language, config)
  }

  /**
    * Load a language for generation based on the underlying language
    * implementation.
    *
    * @param lut
    * @param project
    * @return
    */
  def loadLanguage(lut: ILanguageImpl, project: IProject): Language = {
    val templateLang = getLanguage("org.metaborg:org.metaborg.meta.lang.template:2.1.0")
    val nablLang = getLanguage("org.metaborg:org.metaborg.meta.nabl2.lang:2.1.0")

    languageService.load(templateLang, nablLang, lut, project)
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
