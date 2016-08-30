package nl.tudelft.fragments.spoofax

import java.nio.charset.StandardCharsets

import com.google.common.collect.Iterables
import nl.tudelft.fragments.Main
import nl.tudelft.fragments.spoofax.models.{Sort, SortAppl}
import org.apache.commons.io.IOUtils
import org.metaborg.core.language.ILanguageImpl
import org.metaborg.core.project.SimpleProjectService
import org.metaborg.spoofax.core.syntax.SyntaxFacet
import org.spoofax.interpreter.terms.{IStrategoString, IStrategoTerm}

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
    * Parse file to AST
    */
  def parseFile(languageImpl: ILanguageImpl, filePath: String): IStrategoTerm = {
    val file = s.resourceService.resolve(filePath)
    val text = IOUtils.toString(file.getContent.getInputStream, StandardCharsets.UTF_8)
    val inputUnit = s.unitService.inputUnit(text, languageImpl, null)
    val parseResult = s.syntaxService.parse(inputUnit)

    if (!parseResult.success()) {
      throw new RuntimeException(s"Unsuccessful parse of $filePath in language ${languageImpl.id()}.")
    }

    parseResult.ast()
  }

  /**
    * Get a pretty-printer for the language
    */
  def getPrinter(languageImpl: ILanguageImpl): (IStrategoTerm => IStrategoString) = {
    val languageLocation = Iterables.get(languageImpl.locations(), 0)
    val component = Iterables.get(languageImpl.components(), 0)

    val projectService = s.injector.getInstance(classOf[SimpleProjectService])
    projectService.create(languageLocation)

    val project = s.projectService.get(languageLocation)
    val context = s.contextService.getTemporary(languageLocation, project, languageImpl)
    val runtime = s.strategoRuntimeService.runtime(component, context, false)

    (term: IStrategoTerm) => s.strategoCommon.invoke(runtime, term, "pp-debug").asInstanceOf[IStrategoString]
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
