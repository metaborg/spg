package nl.tudelft.fragments.spoofax

import java.nio.charset.StandardCharsets

import com.google.common.collect.Iterables
import nl.tudelft.fragments.FragmentsModule
import nl.tudelft.fragments.spoofax.models.{Sort, SortAppl}
import org.apache.commons.io.IOUtils
import org.metaborg.core.language.{ILanguageImpl, LanguageUtils}
import org.metaborg.core.project.SimpleProjectService
import org.metaborg.spoofax.core.Spoofax
import org.metaborg.spoofax.core.syntax.SyntaxFacet
import org.spoofax.interpreter.terms.{IStrategoString, IStrategoTerm}

import scala.collection.JavaConverters._

object Utils {
  val s = new Spoofax(new FragmentsModule)

  /**
    * Load the language implementation
    */
  def loadLanguage(path: String): ILanguageImpl = {
    val languageDiscoveryRequest = s.languageDiscoveryService.request(s.resourceService.resolve(path))
    val lutComponents = s.languageDiscoveryService.discover(languageDiscoveryRequest)

    val languages = LanguageUtils.toImpls(lutComponents)

    if (languages.size() == 0) {
      throw new IllegalArgumentException("No language found at path '" + path + "'")
    }

    languages.iterator().next()
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
    * Parse string to AST
    */
  def parseString(languageImpl: ILanguageImpl, content: String): IStrategoTerm = {
    val inputUnit = s.unitService.inputUnit(content, languageImpl, null)
    val parseResult = s.syntaxService.parse(inputUnit)

    if (!parseResult.success()) {
      throw new RuntimeException(s"Unsuccessful parse in language ${languageImpl.id()}.")
    }

    parseResult.ast()
  }

  /**
    * Get a pretty-printer for the language
    */
  def getPrinter(languageImpl: ILanguageImpl): (IStrategoTerm => String) = {
    val languageLocation = Iterables.get(languageImpl.locations(), 0)
    val component = Iterables.get(languageImpl.components(), 0)

    val projectService = s.injector.getInstance(classOf[SimpleProjectService])
    projectService.create(languageLocation)

    val project = s.projectService.get(languageLocation)
    val context = s.contextService.getTemporary(languageLocation, project, languageImpl)
    val runtime = s.strategoRuntimeService.runtime(component, context, false)

    (term: IStrategoTerm) => s.strategoCommon.invoke(runtime, term, "pp-debug")
      .asInstanceOf[IStrategoString].stringValue()
  }

  /**
    * Compute all start symbols
    */
  def startSymbols(languageImpl: ILanguageImpl): Set[Sort] =
    languageImpl
      .facets(classOf[SyntaxFacet]).asScala
      .flatMap(_.startSymbols.asScala)
      .map(SortAppl(_))
      .toSet
}
