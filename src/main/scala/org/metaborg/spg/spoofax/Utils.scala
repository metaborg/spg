package org.metaborg.spg.spoofax

import java.nio.charset.StandardCharsets

import com.google.common.collect.Iterables
import org.metaborg.spg.FragmentsModule
import org.apache.commons.io.IOUtils
import org.apache.commons.vfs2.FileObject
import org.metaborg.core.language.{ILanguageImpl, LanguageUtils}
import org.metaborg.core.project.SimpleProjectService
import org.metaborg.spg.spoofax.models.{Sort, SortAppl}
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
    * Get a pretty-printer for the language.
    *
    * @param languageImpl
    * @return
    */
  def getPrinter(languageImpl: ILanguageImpl): (IStrategoTerm => String) = {
    val languageLocation = Iterables.get(languageImpl.locations(), 0)
    val component = Iterables.get(languageImpl.components(), 0)

    val project = getOrCreateProject(languageLocation)
    val context = s.contextService.getTemporary(languageLocation, project, languageImpl)
    val runtime = s.strategoRuntimeService.runtime(component, context, false)

    (term: IStrategoTerm) => {
      s.strategoCommon.invoke(runtime, term, "pp-debug")
        .asInstanceOf[IStrategoString].stringValue()
    }
  }

  /**
    * Get existing project or create new project at given resource.
    *
    * @param resource
    */
  def getOrCreateProject(resource: FileObject) = {
    val projectService = s.injector.getInstance(classOf[SimpleProjectService])

    Option(projectService.get(resource)).getOrElse(
      projectService.create(resource)
    )
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
