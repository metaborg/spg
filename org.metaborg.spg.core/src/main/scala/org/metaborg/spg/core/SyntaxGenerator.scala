package org.metaborg.spg.core

import com.google.inject.Inject
import com.typesafe.scalalogging.LazyLogging
import org.metaborg.core.language.ILanguageService
import org.metaborg.spg.core.lexical.LexicalGenerator
import org.metaborg.spg.core.spoofax.models.{ConstType, FunType, Sort}
import org.metaborg.spg.core.spoofax.{Converter, Language, LanguageService}
import org.metaborg.spg.core.terms.{Pattern, TermAppl, TermString}

/**
  * The syntax generator generates syntactically valid programs.
  *
  * @param languageService
  * @param baseLanguageService
  * @param chooser
  */
class SyntaxGenerator @Inject()(languageService: LanguageService, baseLanguageService: ILanguageService, chooser: AutomaticChooser) extends AbstractGenerator(languageService, baseLanguageService) with LazyLogging {
  /**
    * Generate a single term by repeatedly invoking generateTry until it
    * returns a syntactically valid term.
    *
    * @param language
    * @param config
    * @return
    */
  override def generateSingle(language: Language, config: Config): String = {
    val startSymbol = language
      .startSymbols
      .toSeq
      .random

    Iterator
      .continually(generateTry(language, config, startSymbol, config.sizeLimit))
      .dropWhile(_.isEmpty)
      .next
      .map(pattern => language.printer(Converter.toTerm(pattern)))
      .get
  }

  /**
    * Try to generate a syntactically valid term for the given sort that is at
    * most the given size.
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
  private def generateTry(language: Language, config: Config, sort: Sort, size: Int): Option[Pattern] = {
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
}
