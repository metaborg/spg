package nl.tudelft.fragments.spoofax

import com.google.common.collect.Iterables
import nl.tudelft.fragments.Main
import nl.tudelft.fragments.spoofax.models.{Sort, SortAppl}
import org.metaborg.core.language.ILanguageImpl
import org.metaborg.spoofax.core.syntax.SyntaxFacet

import scala.collection.JavaConverters._

object Utils {
  val s = Main.spoofax

  /**
    * Load the language implementation
    */
  def loadLanguage(path: String): ILanguageImpl = {
    val languageLocation = s.resourceService.resolve(path)
    val languageComponents = s.discoverLanguages(languageLocation)

    val component = Iterables.get(languageComponents, 0)
    val languageImpl = Iterables.get(component.contributesTo(), 0)

    languageImpl
  }

  /**
    * Compute all start symbols
    */
  def startSymbols(languageImpl: ILanguageImpl): List[Sort] =
    languageImpl
      .facets(classOf[SyntaxFacet]).asScala
      .flatMap(_.startSymbols.asScala)
      .map(SortAppl(_))
      .toList
}
