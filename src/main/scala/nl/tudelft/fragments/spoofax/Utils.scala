package nl.tudelft.fragments.spoofax

import com.google.common.collect.Iterables
import nl.tudelft.fragments.Main
import org.metaborg.core.language.ILanguageImpl

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
}
